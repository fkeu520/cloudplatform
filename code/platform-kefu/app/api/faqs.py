"""FAQ CRUD API 路由"""
import uuid
from datetime import datetime
from fastapi import APIRouter, HTTPException, Request
from app.models.schemas import FaqCreateRequest, FaqResponse, FaqListResponse
from app.models.database import get_db_connection
from app.core.access import has_permission, normalize_tenant_id

router = APIRouter(prefix="/api/kefu", tags=["faqs"])


def _tenant_id(request: Request, permission: str) -> int:
    user = getattr(request.state, "user", None)
    if not user:
        raise HTTPException(401, "Unauthorized: no user context")
    if not has_permission(user, permission):
        raise HTTPException(403, f"Permission denied: {permission}")
    if str(user.get("userType", "")) == "2":
        return 0
    try:
        return normalize_tenant_id(user.get("tenantId"))
    except ValueError as exc:
        raise HTTPException(403, f"Tenant access denied: {exc}") from exc


@router.post("/faqs", response_model=FaqResponse)
async def create_faq(request: Request, req: FaqCreateRequest):
    tenant_id = _tenant_id(request, "kefu:faqs")
    faq_id = str(uuid.uuid4())
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "INSERT INTO kefu_faq (tenant_id, faq_id, question, answer, category) "
                "VALUES (%s,%s,%s,%s,%s)",
                (tenant_id, faq_id, req.question, req.answer, req.category),
            )
    finally:
        conn.close()
    return FaqResponse(
        faq_id=faq_id, question=req.question, answer=req.answer, category=req.category,
        hit_count=0, sat_avg=None, enabled=True,
        created_at=datetime.now().isoformat(), updated_at=datetime.now().isoformat(),
    )


@router.get("/faqs", response_model=FaqListResponse)
async def list_faqs(
    request: Request,
    category: str = None,
    enabled: bool = None,
    limit: int = 50,
    offset: int = 0,
):
    tenant_id = _tenant_id(request, "kefu:faqs")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            sql = "SELECT faq_id, question, answer, category, hit_count, sat_sum, sat_count, enabled, created_at, updated_at FROM kefu_faq WHERE tenant_id=%s"
            args = [tenant_id]
            if category:
                sql += " AND category=%s"
                args.append(category)
            if enabled is not None:
                sql += " AND enabled=%s"
                args.append(1 if enabled else 0)
            sql += " ORDER BY created_at DESC LIMIT %s OFFSET %s"
            args.extend([limit, offset])
            await cur.execute(sql, args)
            rows = await cur.fetchall()
            items = []
            for r in rows:
                sat_avg = (r[5] / r[6]) if r[6] > 0 else None
                items.append(FaqResponse(
                    faq_id=r[0], question=r[1], answer=r[2], category=r[3],
                    hit_count=r[4], sat_avg=sat_avg, enabled=bool(r[7]),
                    created_at=str(r[8]) if r[8] else "", updated_at=str(r[9]) if r[9] else "",
                ))
            return FaqListResponse(total=len(items), items=items)
    finally:
        conn.close()


@router.get("/faqs/{faq_id}", response_model=FaqResponse)
async def get_faq(request: Request, faq_id: str):
    tenant_id = _tenant_id(request, "kefu:faqs")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT faq_id, question, answer, category, hit_count, sat_sum, sat_count, "
                "enabled, created_at, updated_at FROM kefu_faq "
                "WHERE faq_id=%s AND tenant_id=%s",
                (faq_id, tenant_id),
            )
            row = await cur.fetchone()
            if not row:
                raise HTTPException(404, f"FAQ 不存在: {faq_id}")
            sat_avg = (row[5] / row[6]) if row[6] > 0 else None
            return FaqResponse(
                faq_id=row[0], question=row[1], answer=row[2], category=row[3],
                hit_count=row[4], sat_avg=sat_avg, enabled=bool(row[7]),
                created_at=str(row[8]) if row[8] else "", updated_at=str(row[9]) if row[9] else "",
            )
    finally:
        conn.close()


@router.put("/faqs/{faq_id}", response_model=FaqResponse)
async def update_faq(request: Request, faq_id: str, req: FaqCreateRequest):
    tenant_id = _tenant_id(request, "kefu:faqs")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "SELECT faq_id FROM kefu_faq WHERE faq_id=%s AND tenant_id=%s",
                (faq_id, tenant_id),
            )
            if not await cur.fetchone():
                raise HTTPException(404, f"FAQ 不存在: {faq_id}")
            await cur.execute(
                "UPDATE kefu_faq SET question=%s, answer=%s, category=%s, updated_at=NOW() "
                "WHERE faq_id=%s AND tenant_id=%s",
                (req.question, req.answer, req.category, faq_id, tenant_id),
            )
    finally:
        conn.close()
    return await get_faq(request, faq_id)


@router.delete("/faqs/{faq_id}")
async def delete_faq(request: Request, faq_id: str):
    tenant_id = _tenant_id(request, "kefu:faqs")
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute(
                "DELETE FROM kefu_faq WHERE faq_id=%s AND tenant_id=%s",
                (faq_id, tenant_id),
            )
            if cur.rowcount == 0:
                raise HTTPException(404, f"FAQ 不存在: {faq_id}")
    finally:
        conn.close()
    return {"message": "FAQ 已删除"}