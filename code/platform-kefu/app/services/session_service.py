"""会话服务 - 集成数据源路由 + RAG + LLM 生成

2026-09-24 P0 租户隔离: 所有 session/message 的 SELECT/INSERT/UPDATE/DELETE
均附加 tenant_id 过滤 (平台管理员 userType==2 除外)。缺失 tenantId 时调用方
应拒绝请求 (由 auth_middleware / sessions API 层保证)。
"""
import uuid
import json
import time
import re
import logging
from typing import Optional, List, Dict, Any
from app.config import settings as app_settings
from app.models.database import get_db_connection
from app.services.datasource.base import registry
from app.services.datasource.faq_adapter import FaqAdapter
from app.services.datasource.knowledge_adapter import KnowledgeAdapter
from app.services.datasource.park_enterprise_adapter import ParkEnterpriseAdapter
from app.services.embedder import embedder
from app.services.vector_store import get_store
from pathlib import Path
import httpx

logger = logging.getLogger(__name__)


class SessionNotFoundError(LookupError):
    """Raised when a tenant attempts to use a session it does not own."""


_vector_dir = Path(__file__).parent.parent / "data" / "vector_index"
_vector_store = get_store(_vector_dir)

_SESSION_COLUMNS = (
    "id, tenant_id, customer_id, customer_name, contact, channel, status, "
    "agent_id, agent_name, start_time, end_time, satisfaction, satisfaction_comment, "
    "last_message_preview, last_message_at"
)
_MESSAGE_COLUMNS = (
    "id, msg_id, tenant_id, session_id, role, content, data_source_refs, "
    "knowledge_refs, model, latency_ms, create_time"
)

_adapters_registered = False


def ensure_adapters_registered():
    global _adapters_registered
    if _adapters_registered:
        return
    registry.register(FaqAdapter())
    registry.register(KnowledgeAdapter())
    registry.register(ParkEnterpriseAdapter())
    _adapters_registered = True


async def create_session(
    tenant_id: Optional[int] = None,
    customer_id=None,
    customer_name=None,
    contact=None,
    channel="web",
) -> Dict[str, Any]:
    # Platform-admin reads may be cross-tenant, but writes are quarantined in
    # tenant 0 rather than being written without an owner.
    tenant_id = 0 if tenant_id is None else tenant_id
    ensure_adapters_registered()
    sid = str(uuid.uuid4())
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_session "
                "(id, tenant_id, customer_id, customer_name, contact, channel, status) "
                "VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (sid, tenant_id, customer_id, customer_name, contact, channel, "AI"),
            )
        # 显式 commit: 在 aiomysql 连接池 + autocommit 模式下, 不同连接之间偶有读不到刚写入的行
        # (复现: 前端发消息无响应, body 为空字符串). 显式 commit 修复.
        await conn.commit()
    finally:
        conn.close()
    sess = await get_session(sid, tenant_id)
    if sess is None:
        # 极端兜底: INSERT 后立刻 SELECT 拿不到行时, 用入参构造返回值,
        # 保证客户端至少拿到有效 session_id (否则前端 res.data 为空 →
        # session_id 变 undefined → 发送按钮永远 disabled)。
        # 出现这条日志说明跨连接可见性有问题, 排查时需要 sid + tenant + customer。
        logger.warning(
            "create_session: SELECT returned None after INSERT, using fallback "
            "(sid=%s tenant_id=%s customer_id=%r channel=%r)",
            sid, tenant_id, customer_id, channel,
        )
        from datetime import datetime
        now = datetime.utcnow()
        return {
            "id": sid,
            "tenant_id": tenant_id,
            "customer_id": customer_id,
            "customer_name": customer_name,
            "contact": contact,
            "channel": channel,
            "status": "AI",
            "agent_id": None,
            "agent_name": None,
            "start_time": now,
            "end_time": None,
            "satisfaction": None,
            "satisfaction_comment": None,
            "last_message_preview": None,
            "last_message_at": None,
        }
    return sess


async def get_session(sid: str, tenant_id: Optional[int] = None) -> Optional[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    f"SELECT {_SESSION_COLUMNS} FROM kefu_session "
                    "WHERE id=%s AND tenant_id=%s",
                    (sid, tenant_id),
                )
            else:
                # 平台管理员 (userType==2) 跨租户可见, 不限制 tenant_id
                await cur.execute(
                    f"SELECT {_SESSION_COLUMNS} FROM kefu_session WHERE id=%s",
                    (sid,),
                )
            row = await cur.fetchone()
            if not row:
                return None
            return _row_to_session(row)
    finally:
        conn.close()


async def list_sessions(
    tenant_id: Optional[int] = None,
    status=None,
    channel=None,
    limit=50,
    offset=0,
) -> List[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            sql = f"SELECT {_SESSION_COLUMNS} FROM kefu_session WHERE 1=1"
            args = []
            # tenant_id 为 None 表示平台管理员, 不加 tenant 过滤
            if tenant_id is not None:
                sql += " AND tenant_id=%s"
                args.append(tenant_id)
            if status:
                sql += " AND status=%s"
                args.append(status)
            if channel:
                sql += " AND channel=%s"
                args.append(channel)
            sql += " ORDER BY start_time DESC LIMIT %s OFFSET %s"
            args.extend([limit, offset])
            await cur.execute(sql, args)
            rows = await cur.fetchall()
            return [_row_to_session(r) for r in rows]
    finally:
        conn.close()


async def list_my_sessions(
    customer_id,
    tenant_id: Optional[int] = None,
    status=None,
    limit=50,
    offset=0,
) -> List[Dict[str, Any]]:
    """「我的会话」— 按登录用户 customer_id + tenant_id 过滤的历史会话列表.

    2026-09-28 新增: kefu_session.customer_id 在 create_session 时由后端自动写入
    userId(auth_middleware 注入的 request.state.user). 空 customer_id 会话不返回.
    """
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            sql = f"SELECT {_SESSION_COLUMNS} FROM kefu_session WHERE customer_id=%s"
            args: list = [customer_id]
            if tenant_id is not None:
                sql += " AND tenant_id=%s"
                args.append(tenant_id)
            if status:
                sql += " AND status=%s"
                args.append(status)
            sql += " ORDER BY start_time DESC LIMIT %s OFFSET %s"
            args.extend([limit, offset])
            await cur.execute(sql, args)
            rows = await cur.fetchall()
            return [_row_to_session(r) for r in rows]
    finally:
        conn.close()


async def transfer_to_human(
    sid: str,
    tenant_id: Optional[int] = None,
    agent_id=None,
    agent_name=None,
) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    "UPDATE kefu_session SET status='HUMAN', agent_id=%s, agent_name=%s "
                    "WHERE id=%s AND tenant_id=%s",
                    (agent_id, agent_name, sid, tenant_id),
                )
            else:
                await cur.execute(
                    "UPDATE kefu_session SET status='HUMAN', agent_id=%s, agent_name=%s "
                    "WHERE id=%s",
                    (agent_id, agent_name, sid),
                )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def close_session(
    sid: str,
    tenant_id: Optional[int] = None,
) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    "UPDATE kefu_session SET status='CLOSED', end_time=NOW() "
                    "WHERE id=%s AND tenant_id=%s",
                    (sid, tenant_id),
                )
            else:
                await cur.execute(
                    "UPDATE kefu_session SET status='CLOSED', end_time=NOW() WHERE id=%s",
                    (sid,),
                )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def rate_session(
    sid: str,
    tenant_id: Optional[int] = None,
    satisfaction: int = 0,
    comment=None,
) -> Dict[str, Any]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    "UPDATE kefu_session SET satisfaction=%s, satisfaction_comment=%s "
                    "WHERE id=%s AND tenant_id=%s",
                    (satisfaction, comment, sid, tenant_id),
                )
            else:
                await cur.execute(
                    "UPDATE kefu_session SET satisfaction=%s, satisfaction_comment=%s "
                    "WHERE id=%s",
                    (satisfaction, comment, sid),
                )
    finally:
        conn.close()
    return await get_session(sid, tenant_id)


async def get_messages(
    sid: str,
    tenant_id: Optional[int] = None,
    limit=100,
) -> List[Dict[str, Any]]:
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            if tenant_id is not None:
                await cur.execute(
                    f"SELECT {_MESSAGE_COLUMNS} FROM kefu_message "
                    "WHERE session_id=%s AND tenant_id=%s "
                    "ORDER BY create_time ASC LIMIT %s",
                    (sid, tenant_id, limit),
                )
            else:
                await cur.execute(
                    f"SELECT {_MESSAGE_COLUMNS} FROM kefu_message "
                    "WHERE session_id=%s ORDER BY create_time ASC LIMIT %s",
                    (sid, limit),
                )
            rows = await cur.fetchall()
            return [_row_to_message(r) for r in rows]
    finally:
        conn.close()


async def save_message(
    sid: str,
    tenant_id: Optional[int],
    role: str,
    content: str,
    data_source_refs=None,
    knowledge_refs=None,
    model="",
    latency_ms=0,
) -> Dict[str, Any]:
    tenant_id = 0 if tenant_id is None else tenant_id
    msg_id = str(uuid.uuid4())
    refs_json = json.dumps(data_source_refs, ensure_ascii=False) if data_source_refs else None
    krefs_json = json.dumps(knowledge_refs, ensure_ascii=False) if knowledge_refs else None
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_message "
                "(msg_id, tenant_id, session_id, role, content, data_source_refs, "
                " knowledge_refs, model, latency_ms) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                (msg_id, tenant_id, sid, role, content, refs_json, krefs_json, model, latency_ms),
            )
            await cur.execute(
                "SELECT create_time FROM kefu_message WHERE msg_id=%s",
                (msg_id,),
            )
            row = await cur.fetchone()
            create_time = str(row[0]) if row else ""
            await cur.execute(
                "UPDATE kefu_session SET last_message_preview=%s, last_message_at=NOW() "
                "WHERE id=%s AND tenant_id=%s",
                (content[:255], sid, tenant_id),
            )
    finally:
        conn.close()
    return {
        "msg_id": msg_id,
        "session_id": sid,
        "tenant_id": tenant_id,
        "role": role,
        "content": content,
        "data_source_refs": data_source_refs or [],
        "knowledge_refs": knowledge_refs or [],
        "model": model,
        "latency_ms": latency_ms,
        "create_time": create_time,
    }


# ---------- Phase-1 混合检索 (数据源结构化字段 + FAISS 语义) ----------
#
# 并行任务 (企业全量入仓) 的冻结接口:
#   kefu_datasource_record(id, source_id, record_key, entity_type, entity_id,
#                          tenant_id, title, record_json, profile_text,
#                          profile_hash, version, status, created_at, updated_at)
#   kefu_datasource_record_field(id, source_id, record_id, tenant_id,
#                                 field_key, field_label, field_value,
#                                 field_value_norm, searchable, exact_matchable, ...)
#   企业向量以确定性 chunk_id ``ds:{source_id}:{entity_type}:{entity_id}``
#   进同一个共享 FAISS 索引, 全文在 record.profile_text。
#
# 注意: 这两张表由另一个任务创建, 本任务运行时可能不存在 —— 所有新 DB 路径
# 必须捕获异常并降级 (返回 "", 打 warning), chat 绝不能因此崩溃。

# 精确字段: 归一化 (去空格/连字符, 小写) 后落 field_value_norm。
_EXACT_FIELD_KEYS = (
    "creditCode", "taxNumber", "regNumber", "orgNumber", "phoneNumber", "email",
)

# 18 位统一社会信用代码 [0-9A-HJ-NPQRTUWXY]{18}
_CREDIT_CODE_RE = re.compile(r"[0-9A-HJ-NPQRTUWXY]{18}")
# 税号 (15-20 位字母数字)
_TAX_NUMBER_RE = re.compile(r"[0-9A-Z]{15,20}")
# 手机号 1[3-9]xxxxxxxxx
_PHONE_RE = re.compile(r"1[3-9]\d{9}")
# 座机/400: 3-4 位区号 + 7-8 位号码, 区号后可带连字符
_TEL_RE = re.compile(r"\d{3,4}-?\d{7,8}")
# 邮箱
_EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")


def _normalize_exact_value(value: str) -> str:
    """把候选编码归一化成 field_value_norm 的形态: 去空白/连字符, 小写。"""
    return re.sub(r"[\s-]+", "", (value or "")).lower()


def _tenant_filter(tenant_id: Optional[int], alias: str = "") -> "tuple[str, list[Any]]":
    """构造租户过滤 SQL 片段 + 参数。

    语义 (与 session 层一致):
      - int      -> AND tenant_id=%s
      - 0        -> AND tenant_id=0 (平台隔离桶, 也是 int, 走同一条路)
      - None     -> 平台管理员跨租户, 不过滤
    ``alias`` 用于 JOIN 查询消歧 —— record 与 field 两表都有 tenant_id,
    不带别名会触发 MySQL 1052 ambiguous column。
    返回 (sql_fragment, params)。
    """
    if tenant_id is None:
        return "", []
    col = f"{alias}.tenant_id" if alias else "tenant_id"
    return f" AND {col}=%s", [tenant_id]


def _extract_code_candidates(question: str) -> List[str]:
    """正则抽取问题里的编码类候选 (信用代码/税号/手机/座机/邮箱), 去重保序。"""
    if not question:
        return []
    candidates: List[str] = []
    seen: set = set()

    def _add(value: str):
        value = (value or "").strip()
        if value and value not in seen:
            seen.add(value)
            candidates.append(value)

    for m in _CREDIT_CODE_RE.finditer(question):
        _add(m.group(0))
    for m in _TAX_NUMBER_RE.finditer(question):
        _add(m.group(0))
    for m in _PHONE_RE.finditer(question):
        _add(m.group(0))
    for m in _TEL_RE.finditer(question):
        _add(m.group(0))
    for m in _EMAIL_RE.finditer(question):
        _add(m.group(0))
    return candidates


async def _structured_lookup(question: str, tenant_id: Optional[int]) -> str:
    """Phase-1 精确/名称结构化查询, 命中则返回该企业的 profile_text, 否则 ""。

    (a) 精确优先: 问题里出现编码类字段 (creditCode/taxNumber/...) 时, 归一化后
        按 field_value_norm 精确匹配记录, 直接取其 profile_text。
    (b) 名称兜底: 否则用 name/alias 字段值在问题里做包含匹配 (长值优先), 取 profile_text。

    任何 DB 异常 (包括表还不存在) 都降级为 "", 绝不向上抛。
    """
    conn = None
    try:
        # (a) 精确匹配: 编码类字段归一化后匹配 field_value_norm。
        #     两个坑:
        #       1) JOIN 后 record 与 field 都有 tenant_id, 过滤必须带别名 (f.tenant_id),
        #          否则 MySQL 报 1052 ambiguous column —— 原来静默降级, 精确路径永不生效。
        #       2) 入仓任务 (_norm) 把 alnum 归一化为**大写**, 而这里候选值是小写,
        #          故用 LOWER() 双边忽略大小写, 兼容 email / 含字母的编码。
        raw_candidates = _extract_code_candidates(question)
        norm_values = [v for v in (_normalize_exact_value(c) for c in raw_candidates) if v]
        if norm_values:
            seen: set = set()
            deduped: List[str] = []
            for v in norm_values:
                if v not in seen:
                    seen.add(v)
                    deduped.append(v)
            tf_sql, tf_args = _tenant_filter(tenant_id, "f")
            for norm_value in deduped:
                conn = await get_db_connection()
                try:
                    async with conn.cursor() as cur:
                        sql = (
                            "SELECT r.profile_text "
                            "FROM kefu_datasource_record_field f "
                            "JOIN kefu_datasource_record r ON r.id=f.record_id "
                            "WHERE f.source_id='park-enterprise' "
                            "AND LOWER(f.field_value_norm)=%s AND r.status='active'"
                            + tf_sql
                            + " LIMIT 1"
                        )
                        args = [norm_value] + tf_args
                        await cur.execute(sql, args)
                        row = await cur.fetchone()
                        if row and row[0]:
                            return row[0]
                finally:
                    conn.close()
                    conn = None

        # (b) 名称匹配: 企业的 name/alias 字段值出现在问题里即命中
        #     (INSTR(question, field_value) > 0), 长值优先。
        #     原实现用整条 title 做子串 (INSTR(question, title)) —— "华为" 永远
        #     匹配不到 "华为技术有限公司", 名称路径形同虚设, 改成按字段值反向包含。
        tf_sql, tf_args = _tenant_filter(tenant_id, "f")
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                sql = (
                    "SELECT r.profile_text "
                    "FROM kefu_datasource_record_field f "
                    "JOIN kefu_datasource_record r ON r.id=f.record_id "
                    "WHERE f.source_id='park-enterprise' "
                    "AND f.field_key IN ('name','alias') "
                    "AND CHAR_LENGTH(f.field_value) >= 2 "
                    "AND INSTR(%s, f.field_value) > 0 "
                    "AND r.status='active'"
                    + tf_sql
                    + " ORDER BY CHAR_LENGTH(f.field_value) DESC LIMIT 1"
                )
                args = [question] + tf_args
                await cur.execute(sql, args)
                row = await cur.fetchone()
                if row and row[0]:
                    return row[0]
        finally:
            conn.close()

        return ""
    except Exception as e:
        # 表可能尚未创建 / 连接不可用 —— 降级, 不阻断 chat。
        logger.warning("[StructuredLookup] 结构化查询失败, 已降级: %s", e)
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass
        return ""


async def _resolve_ds_profiles(chunk_ids: List[str], tenant_id: Optional[int]) -> str:
    """把 FAISS 命中里以 ds: 开头的 chunk_id 解析成 profile_text (不截断)。

    ds chunk id 形如 ``ds:{source_id}:{entity_type}:{entity_id}``, 其全文在
    kefu_datasource_record.profile_text。表缺失/连接失败时静默降级为空串。
    """
    ds_ids = [cid for cid in chunk_ids if isinstance(cid, str) and cid.startswith("ds:")]
    if not ds_ids:
        return ""
    tf_sql, tf_args = _tenant_filter(tenant_id)
    conn = None
    try:
        conn = await get_db_connection()
        try:
            async with conn.cursor() as cur:
                placeholders = ",".join(["%s"] * len(ds_ids))
                sql = (
                    "SELECT profile_text FROM kefu_datasource_record "
                    "WHERE source_id='park-enterprise' AND status='active'"
                    + tf_sql
                    + f" AND CONCAT('ds:', source_id, ':', entity_type, ':', "
                      f"CAST(entity_id AS CHAR)) IN ({placeholders})"
                )
                args = tf_args + ds_ids
                await cur.execute(sql, args)
                rows = await cur.fetchall()
                return "\n---\n".join(
                    r[0] for r in rows if r and r[0]
                )
        finally:
            conn.close()
    except Exception as e:
        logger.warning("[RAG] 数据源 profile 解析失败 (表可能未建), 已降级: %s", e)
        if conn is not None:
            try:
                conn.close()
            except Exception:
                pass
        return ""


def _dedup_adapter_context(context_parts: List[str], dedup_keys: List[str]) -> List[str]:
    """去掉与结构化/数据源已覆盖企业重复的 live-adapter 摘要 (按名称/entity)。"""
    if not dedup_keys:
        return list(context_parts)
    keys_lower = {k.lower() for k in dedup_keys if k}
    out: List[str] = []
    for part in context_parts:
        hit = False
        low = part.lower()
        for key in keys_lower:
            if key in low:
                hit = True
                break
        if not hit:
            out.append(part)
    return out


async def chat(
    sid: str,
    tenant_id: Optional[int],
    user_content: str,
    settings=None,
) -> Dict[str, Any]:
    """核心对话流程：
    1. 保存用户消息
    2. 数据源路由查询
    3. RAG 向量检索
    4. 合并上下文
    5. LLM 生成回答
    6. 保存 AI 消息
    7. 返回
    """
    tenant_id = 0 if tenant_id is None else tenant_id
    if await get_session(sid, tenant_id) is None:
        raise SessionNotFoundError(f"会话不存在或无权访问: {sid}")
    ensure_adapters_registered()
    t0 = time.time()

    # 1. 保存用户消息 (tenant_id 必须非 None，否则上层已拒绝)
    user_msg = await save_message(sid, tenant_id, "customer", user_content)

    # 2. 数据源路由 (live adapter, 关键词门控; 放在最后作为补充)
    ds_results = await registry.route_query(user_content, tenant_id=tenant_id)
    ds_refs = []
    adapter_parts: List[str] = []
    for source_id, result in ds_results.items():
        for ref in result.refs:
            ds_refs.append(ref.to_dict())
        if result.summary:
            adapter_parts.append(f"[{source_id}] {result.summary}")
        # 记录命中
        await _record_ds_hit(sid, source_id, tenant_id)

    context_parts: List[str] = []
    dedup_keys: List[str] = []

    # 2a. 结构化精确/名称查询 (无条件执行, 不受 intent_keywords 门控)。
    #     exact-first: 命中即把该 profile 置顶, 并把它记为去重键。
    structured = await _structured_lookup(user_content, tenant_id)
    if structured:
        context_parts.append(f"[enterprise-exact] {structured}")

    # 2b. 混合 RAG: 数据源 profile (不截断) + 知识库文档块 (保持原 [:300] 行为)。
    rag_context = await _rag_search(user_content, tenant_id=tenant_id, top_k=8)
    if rag_context:
        context_parts.append(f"[knowledge-rag] {rag_context}")

    # 2c. 去重: 若 _structured_lookup 已覆盖某企业, live adapter 的同名摘要不再重复。
    #     最佳努力按名称匹配, 不精确时保留 adapter 摘要 (宁多勿漏, 由 LLM 收敛)。
    if structured:
        # 从结构化 profile 文本里尽力提取企业名称作为去重键
        name_m = re.search(r"名称[:：]\s*([^\n（(]+)", structured)
        if name_m:
            dedup_keys.append(name_m.group(1).strip())
        for title_match in re.finditer(r"企业名称[:：]\s*([^\n（(]+)", structured):
            dedup_keys.append(title_match.group(1).strip())

    adapter_parts = _dedup_adapter_context(adapter_parts, dedup_keys)
    context_parts.extend(adapter_parts)

    # 3. LLM 生成
    full_context = "\n\n".join(context_parts) if context_parts else "（无外部数据源命中）"
    answer, model_name = await _llm_generate(user_content, full_context, sid)

    latency = int((time.time() - t0) * 1000)

    # 5. 保存 AI 消息
    ai_msg = await save_message(
        sid, tenant_id, "assistant", answer,
        data_source_refs=ds_refs,
        knowledge_refs=[r["entity_id"] for r in ds_refs if r.get("source") == "knowledge"],
        model=model_name,
        latency_ms=latency,
    )

    session = await get_session(sid, tenant_id)
    return {
        "user_message": user_msg,
        "ai_message": ai_msg,
        "session_status": session["status"] if session else "AI",
        "referenced_data_sources": ds_refs,
    }


async def _rag_search(question: str, tenant_id: int, top_k=8) -> str:
    """混合 RAG (Phase-1): 数据源 profile (不截断) + 上传文档块 (保持 [:300])。

    - 向量检索用同一个共享 FAISS 索引 (top_k=8, min_score=0.25)。
    - 命中 chunk_id 以 ``ds:`` 开头的来自入仓企业, 全文从
      kefu_datasource_record.profile_text 取, **不截断**; 表缺失时静默降级为纯文档行为。
    - 其余命中保持原有文档块行为 (content[:300])。
    - 对外契约不变: 仍返回拼接后的上下文字符串 (可为 "")。
    """
    try:
        vec = embedder.embed([question])[0]
        hits = _vector_store.search(vec, top_k=top_k, min_score=0.25)
        if not hits:
            return ""
        chunk_ids = [h[0] for h in hits]

        ds_ids = [c for c in chunk_ids if c.startswith("ds:")]
        doc_ids = [c for c in chunk_ids if not c.startswith("ds:")]

        parts: List[str] = []
        # 数据源 profile 在前 (exact-first 已置于 chat 的 [enterprise-exact],
        # 这里放语义命中, 仍先于文档块), 不截断。
        if ds_ids:
            ds_text = await _resolve_ds_profiles(ds_ids, tenant_id)
            if ds_text:
                parts.append(ds_text)
        # 文档块保持原行为 (tenant 过滤 + [:300] 截断)。
        if doc_ids:
            conn = await get_db_connection()
            try:
                async with conn.cursor() as cur:
                    placeholders = ",".join(["%s"] * len(doc_ids))
                    await cur.execute(
                        f"SELECT content FROM chunks WHERE chunk_id IN ({placeholders}) "
                        "AND tenant_id=%s",
                        doc_ids + [tenant_id],
                    )
                    rows = await cur.fetchall()
                    doc_text = "\n---\n".join(r[0][:300] for r in rows if r and r[0])
                    if doc_text:
                        parts.append(doc_text)
            finally:
                conn.close()
        return "\n---\n".join(parts)
    except Exception as e:
        logger.warning(f"[RAG] 检索失败: {e}")
        return ""


async def _llm_generate(question: str, context: str, sid: str, settings=None) -> tuple:
    """调用 DeepSeek LLM 生成回答

    2026-09-28 修复: 本函数的形参名为 `settings`, 遮蔽了 app.config.settings 全局单例;
    而 sessions.py 的 send_message 恒定传 `settings=None`, 于是 `if not settings` 恒真,
    LLM 分支永远不可达 —— 所有回答都是 _mock_llm_response 的固定话术, 表现为
    "AI 只会说 抱歉我暂时无法回答"。现改为始终读取全局配置, 入参仅保留兼容。
    """
    api_key = app_settings.deepseek_api_key
    if not api_key or not api_key.strip():
        logger.warning("[LLM] DEEPSEEK_API_KEY 未配置, 返回兜底话术 (非真实模型回答)")
        return _mock_llm_response(question, context), "mock"

    system_prompt = f"""你是云枢园区的智能客服助手。请基于以下数据源信息回答用户问题。
如果数据源信息足够，请直接引用并给出准确回答。
如果数据源信息不足，请诚实说明，并引导用户联系人工客服。
统一社会信用代码、纳税人识别号、注册号等编码类字段必须逐字来自【数据源信息】，禁止推断或编造；若数据源未提供，请明确说明。

【数据源信息】
{context}
"""
    payload = {
        "model": app_settings.deepseek_model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": question},
        ],
        "max_tokens": 800,
        "temperature": 0.3,
    }
    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            resp = await client.post(
                f"{app_settings.deepseek_base_url}/chat/completions",
                json=payload,
                headers={"Authorization": f"Bearer {api_key.strip()}"},
            )
            resp.raise_for_status()
            data = resp.json()
            answer = data["choices"][0]["message"]["content"]
            return answer, app_settings.deepseek_model
    except Exception as e:
        logger.error(f"[LLM] DeepSeek 调用失败: {e}")
        return _mock_llm_response(question, context), "mock-fallback"


def _mock_llm_response(question: str, context: str) -> str:
    if context and context != "（无外部数据源命中）":
        return f"根据数据源信息：\n\n{context}\n\n如果您需要更详细的帮助，可以转接人工客服。"
    return f"抱歉，我暂时无法回答「{question}」。您可以尝试换一种问法，或转接人工客服。"


async def _record_ds_hit(sid: str, source_id: str, tenant_id: int):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_session_data_source_hit "
                "(tenant_id, session_id, data_source_id, hit_count) VALUES (%s,%s,%s,1) "
                "ON DUPLICATE KEY UPDATE hit_count=hit_count+1, last_hit_at=NOW()",
                (tenant_id, sid, source_id),
            )
    finally:
        conn.close()


def _row_to_session(row) -> Dict[str, Any]:
    # kefu_session columns (with tenant_id):
    # id=0, tenant_id=1, customer_id=2, customer_name=3, contact=4, channel=5,
    # status=6, agent_id=7, agent_name=8, start_time=9, end_time=10,
    # satisfaction=11, satisfaction_comment=12, last_message_preview=13, last_message_at=14
    return {
        "id": row[0],
        "tenant_id": row[1] if len(row) > 1 else None,
        "customer_id": row[2] if len(row) > 2 else None,
        "customer_name": row[3] if len(row) > 3 else None,
        "contact": row[4] if len(row) > 4 else None,
        "channel": row[5] if len(row) > 5 else "web",
        "status": row[6] if len(row) > 6 else "AI",
        "agent_id": row[7] if len(row) > 7 else None,
        "agent_name": row[8] if len(row) > 8 else None,
        "start_time": str(row[9]) if len(row) > 9 and row[9] else None,
        "end_time": str(row[10]) if len(row) > 10 and row[10] else None,
        "satisfaction": row[11] if len(row) > 11 else None,
        "satisfaction_comment": row[12] if len(row) > 12 else None,
        "last_message_preview": row[13] if len(row) > 13 else None,
        "last_message_at": str(row[14]) if len(row) > 14 and row[14] else None,
    }


def _row_to_message(row) -> Dict[str, Any]:
    # kefu_message columns (with tenant_id):
    # id=0, msg_id=1, tenant_id=2, session_id=3, role=4, content=5,
    # data_source_refs=6, knowledge_refs=7, model=8, latency_ms=9, create_time=10
    tenant_id = row[2] if len(row) > 2 else 0
    refs_raw = row[6] if len(row) > 6 else None
    krefs_raw = row[7] if len(row) > 7 else None
    refs = []
    krefs = []
    try:
        if refs_raw:
            refs = json.loads(refs_raw) if isinstance(refs_raw, str) else refs_raw
    except (json.JSONDecodeError, TypeError):
        refs = []
    try:
        if krefs_raw:
            krefs = json.loads(krefs_raw) if isinstance(krefs_raw, str) else krefs_raw
    except (json.JSONDecodeError, TypeError):
        krefs = []
    content = row[5] if len(row) > 5 and row[5] else ""
    return {
        "msg_id": row[1],
        "tenant_id": tenant_id,
        "session_id": row[3],
        "role": row[4],
        "content": content,
        "data_source_refs": refs,
        "knowledge_refs": krefs,
        "model": row[8] if len(row) > 8 and row[8] else "",
        "latency_ms": row[9] if len(row) > 9 and row[9] else 0,
        "create_time": str(row[10]) if len(row) > 10 and row[10] else "",
    }
