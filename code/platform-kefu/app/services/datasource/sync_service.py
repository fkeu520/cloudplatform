"""Full ingestion sync for park-enterprise data source.

Phase-1: pull the FULL enterprise dataset from upstream,
store it in kefu_datasource_record / kefu_datasource_record_field,
and embed one profile text per enterprise into the shared FAISS index.
"""
from __future__ import annotations

import asyncio
import hashlib
import json
import logging
import os
import re
from typing import Any, Dict, List, Optional, Tuple

import httpx

from app.models.database import get_db_connection

log = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Pure helpers (no I/O) — unit-testable in isolation
# ---------------------------------------------------------------------------

# (java_key, chinese_label, exact_matchable)
_FIELD_MAP: list[tuple[str, str, bool]] = [
    ("name", "企业名称", False),
    ("alias", "简称", False),
    ("creditCode", "统一社会信用代码", True),
    ("taxNumber", "纳税人识别号", True),
    ("regNumber", "注册号", True),
    ("orgNumber", "组织机构代码", True),
    ("legalPersonName", "法定代表人", False),
    ("regStatus", "注册状态", False),
    ("regCapital", "注册资本", False),
    ("estiblishTime", "成立时间", False),
    ("regLocation", "注册地", False),
    ("industry", "行业", False),
    ("businessScope", "经营范围", False),
    ("phoneNumber", "电话", True),
    ("email", "邮箱", True),
    ("staffNumRange", "人员规模", False),
    ("socialStaffNum", "参保人数", False),
    ("websiteList", "官网", False),
    ("tags", "标签", False),
]


def _norm(value: Any) -> str:
    """Normalise a value for exact matching.

    - strip, collapse whitespace and hyphens
    - uppercase alphanumeric runs, keep CJK characters as-is
    """
    if value is None:
        return ""
    s = str(value).strip()
    s = re.sub(r"[\s\-]+", "", s)
    # Uppercase runs of letters/digits only; CJK and other non-ASCII chars
    # are left untouched so Chinese values stay byte-identical.
    s = re.sub(r"[0-9a-zA-Z]+", lambda m: m.group(0).upper(), s)
    return s


def build_profile_text(record: dict) -> str:
    """Build ONE Chinese-labeled profile block for an enterprise record.

    Name first, identifiers near top, empty values omitted.
    """
    bits: list[str] = []
    for key, label, _exact in _FIELD_MAP:
        val = record.get(key)
        # Handle list values (websiteList, tags)
        if isinstance(val, list):
            val = "、".join(str(x) for x in val if x)
        if val in (None, "", []):
            continue
        bits.append(f"{label}：{val}")
    return "；".join(bits)


def profile_hash(text: str) -> str:
    """SHA-256 hexdigest of *text*."""
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def explode_fields(record: dict) -> list[tuple[str, str, str, bool]]:
    """Yield (field_key, field_label, field_value, is_exact) for each field.

    Empty values are skipped.  ``field_value`` is the human-readable string.
    """
    results: list[tuple[str, str, str, bool]] = []
    for key, label, is_exact in _FIELD_MAP:
        val = record.get(key)
        if isinstance(val, list):
            val = "、".join(str(x) for x in val if x)
        if val in (None, "", []):
            continue
        results.append((key, label, str(val), is_exact))
    return results


# ---------------------------------------------------------------------------
# Async fetch
# ---------------------------------------------------------------------------

_PAGE_SIZE = 200
_HTTP_TIMEOUT = 15.0


async def fetch_all_enterprises() -> list[dict]:
    """Paginate /enterprise/page (pageSize=200) until all records collected.

    Reads PLATFORM_ENTERPRISE_BASE_URL; if empty, returns [].
    """
    base_url = os.getenv("PLATFORM_ENTERPRISE_BASE_URL", "").rstrip("/")
    if not base_url:
        log.info("[sync] PLATFORM_ENTERPRISE_BASE_URL not set; skip fetch")
        return []

    all_records: list[dict] = []
    page_num = 1
    async with httpx.AsyncClient(timeout=_HTTP_TIMEOUT) as client:
        while True:
            resp = await client.get(
                f"{base_url}/enterprise/page",
                params={"pageNum": page_num, "pageSize": _PAGE_SIZE},
            )
            resp.raise_for_status()
            payload = resp.json()
            if payload.get("code") != 200:
                raise RuntimeError(
                    f"park-enterprise returned code={payload.get('code')}: "
                    f"{payload.get('message')}"
                )
            data = payload.get("data") or {}
            records: list[dict] = data.get("records") or []
            all_records.extend(records)
            total = int(data.get("total") or 0)
            pages = int(data.get("pages") or 0)
            if len(records) < _PAGE_SIZE or page_num >= pages or len(all_records) >= total:
                break
            page_num += 1

    log.info("[sync] fetched %d enterprise records", len(all_records))
    return all_records


# ---------------------------------------------------------------------------
# Full sync orchestration
# ---------------------------------------------------------------------------

def _chunk_id(source_id: str, rec_id: str) -> str:
    return f"ds:{source_id}:enterprise:{rec_id}"


def _record_key(source_id: str, rec_id: str) -> str:
    return f"{source_id}:enterprise:{rec_id}"


async def sync_source(source_id: str = "park-enterprise", tenant_id: Optional[int] = None) -> dict:
    """Run a full ingestion sync for the given data source.

    Returns a summary dict:
        {status, fetched, inserted, updated, skipped, deleted, embed_calls, errors}
    """
    from app.services.vector_store import get_store
    from pathlib import Path

    vs_dir = Path(__file__).parent.parent.parent / "data" / "vector_index"
    store = get_store(vs_dir)

    summary: Dict[str, Any] = {
        "status": "ok",
        "fetched": 0,
        "inserted": 0,
        "updated": 0,
        "skipped": 0,
        "deleted": 0,
        "embed_calls": 0,
        "errors": 0,
    }

    # 1. Fetch
    records = await fetch_all_enterprises()
    summary["fetched"] = len(records)
    if not records:
        log.info("[sync] no records fetched; nothing to do")
        await _update_source_meta(source_id, "skipped")
        return summary

    # 2. Per-record: upsert record + replace fields, collect vectors to (re)embed
    fetched_keys: set[str] = set()
    to_embed: list[tuple[str, str]] = []  # (chunk_id, profile_text)

    for rec in records:
        try:
            rec_id = str(rec.get("id"))
            rkey = _record_key(source_id, rec_id)
            fetched_keys.add(rkey)

            profile = build_profile_text(rec)
            p_hash = profile_hash(profile)
            tenant = int(rec.get("tenantId") or 0)
            title = str(rec.get("name") or f"企业#{rec_id}")

            existing = await _get_record(source_id, rkey)
            if existing:
                old_hash = existing["profile_hash"]
                old_version = existing["version"]
                if old_hash == p_hash:
                    summary["skipped"] += 1
                else:
                    summary["updated"] += 1
                    to_embed.append((_chunk_id(source_id, rec_id), profile))
                    await _upsert_record(
                        source_id=source_id,
                        record_key=rkey,
                        entity_type="enterprise",
                        entity_id=rec_id,
                        tenant_id=tenant,
                        title=title,
                        record_json=rec,
                        profile_text=profile,
                        profile_hash=p_hash,
                        version=old_version + 1,
                        record_id=existing["id"],
                    )
            else:
                summary["inserted"] += 1
                to_embed.append((_chunk_id(source_id, rec_id), profile))
                await _upsert_record(
                    source_id=source_id,
                    record_key=rkey,
                    entity_type="enterprise",
                    entity_id=rec_id,
                    tenant_id=tenant,
                    title=title,
                    record_json=rec,
                    profile_text=profile,
                    profile_hash=p_hash,
                    version=1,
                    record_id=None,
                )

            # 3. Replace field rows
            await _replace_fields(source_id, rkey, rec, tenant)

        except Exception as exc:
            summary["errors"] += 1
            log.exception("[sync] per-record error for rec_id=%s: %s", rec.get("id"), exc)
            summary["status"] = "partial"
            continue

    # 4. Embed changed / new records (off the event loop).
    #    For updated records the old vector is deleted BEFORE adding the new one.
    if to_embed:
        chunk_ids = [cid for cid, _ in to_embed]
        texts = [txt for _, txt in to_embed]
        try:
            # Delete old vectors first (rebuild preserves the rest)
            existing_vec_ids = [
                cid for cid in chunk_ids if cid in store.chunk_id_to_vector_id
            ]
            if existing_vec_ids:
                store.delete_by_chunk_ids(existing_vec_ids)

            from app.services.embedder import embedder
            vectors = await asyncio.get_running_loop().run_in_executor(
                None, lambda: embedder.embed(texts)
            )

            # add_vectors stays on the event loop (no await inside, keeps
            # the _all_instances reload atomic) — same contract as docs.py
            store.add_vectors(chunk_ids, vectors)
            summary["embed_calls"] += len(texts)
        except Exception as exc:
            summary["errors"] += 1
            summary["status"] = "partial"
            log.exception("[sync] embedding failed: %s", exc)

    # 5. Delete stale local records (not present in the fetched set)
    local_keys = await _list_record_keys(source_id)
    stale_keys = local_keys - fetched_keys
    if stale_keys:
        stale_chunk_ids: list[str] = []
        for skey in stale_keys:
            try:
                row = await _get_record(source_id, skey)
                if row:
                    await _delete_record_and_fields(source_id, skey, row["id"])
                    summary["deleted"] += 1
                    stale_chunk_ids.append(
                        _chunk_id(source_id, skey.split(":enterprise:", 1)[-1])
                    )
            except Exception as exc:
                summary["errors"] += 1
                log.exception("[sync] stale-delete error for %s: %s", skey, exc)
        if stale_chunk_ids:
            try:
                store.delete_by_chunk_ids(stale_chunk_ids)
            except Exception as exc:
                summary["errors"] += 1
                log.exception("[sync] failed to delete stale vectors: %s", exc)

    # 6. Update source meta
    final_status = summary["status"]
    await _update_source_meta(source_id, final_status)

    log.info(
        "[sync] %s: %d fetched, %d inserted, %d updated, %d skipped, %d deleted, %d errors",
        source_id, summary["fetched"], summary["inserted"],
        summary["updated"], summary["skipped"], summary["deleted"],
        summary["errors"],
    )
    return summary


# ---------------------------------------------------------------------------
# DB helpers (each acquires its own connection, closes in finally)
# ---------------------------------------------------------------------------

async def _get_record(source_id: str, record_key: str) -> Optional[dict]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT id, profile_hash, version FROM kefu_datasource_record "
                "WHERE source_id=%s AND record_key=%s",
                (source_id, record_key),
            )
            row = await cur.fetchone()
            if not row:
                return None
            return {"id": row[0], "profile_hash": row[1], "version": row[2]}
    finally:
        conn.close()


async def _upsert_record(
    *,
    source_id: str,
    record_key: str,
    entity_type: str,
    entity_id: str,
    tenant_id: int,
    title: str,
    record_json: dict,
    profile_text: str,
    profile_hash: str,
    version: int,
    record_id: Optional[int],
) -> None:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if record_id is None:
                await cur.execute(
                    """INSERT INTO kefu_datasource_record
                       (source_id, record_key, entity_type, entity_id, tenant_id,
                        title, record_json, profile_text, profile_hash, version, status)
                       VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,'active')""",
                    (
                        source_id, record_key, entity_type, entity_id, tenant_id,
                        title, json.dumps(record_json, ensure_ascii=False),
                        profile_text, profile_hash, version,
                    ),
                )
            else:
                await cur.execute(
                    """UPDATE kefu_datasource_record
                       SET record_json=%s, profile_text=%s, profile_hash=%s,
                           version=%s, tenant_id=%s, title=%s
                       WHERE id=%s""",
                    (
                        json.dumps(record_json, ensure_ascii=False),
                        profile_text, profile_hash, version, tenant_id, title,
                        record_id,
                    ),
                )
    finally:
        conn.close()


async def _replace_fields(
    source_id: str, record_key: str, record: dict, tenant_id: int
) -> None:
    """Delete old field rows for the record, insert new ones."""
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            # Get the record's id
            await cur.execute(
                "SELECT id FROM kefu_datasource_record WHERE source_id=%s AND record_key=%s",
                (source_id, record_key),
            )
            row = await cur.fetchone()
            if not row:
                return
            record_id = row[0]

            await cur.execute(
                "DELETE FROM kefu_datasource_record_field WHERE record_id=%s",
                (record_id,),
            )

            fields = explode_fields(record)
            for key, label, value, is_exact in fields:
                norm_val = _norm(value)[:512]
                await cur.execute(
                    """INSERT INTO kefu_datasource_record_field
                       (source_id, record_id, tenant_id, field_key, field_label,
                        field_value, field_value_norm, exact_matchable)
                       VALUES (%s,%s,%s,%s,%s,%s,%s,%s)""",
                    (
                        source_id, record_id, tenant_id, key, label,
                        value, norm_val, 1 if is_exact else 0,
                    ),
                )
    finally:
        conn.close()


async def _delete_record_and_fields(source_id: str, record_key: str, record_id: int) -> None:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "DELETE FROM kefu_datasource_record_field WHERE record_id=%s",
                (record_id,),
            )
            await cur.execute(
                "DELETE FROM kefu_datasource_record WHERE source_id=%s AND record_key=%s",
                (source_id, record_key),
            )
    finally:
        conn.close()


async def _list_record_keys(source_id: str) -> set[str]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT record_key FROM kefu_datasource_record WHERE source_id=%s",
                (source_id,),
            )
            rows = await cur.fetchall()
            return {r[0] for r in rows}
    finally:
        conn.close()


async def _update_source_meta(source_id: str, status: str) -> None:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "UPDATE kefu_data_source SET last_sync_at=NOW(), last_sync_status=%s "
                "WHERE id=%s",
                (status, source_id),
            )
    finally:
        conn.close()