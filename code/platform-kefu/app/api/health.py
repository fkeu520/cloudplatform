from fastapi import APIRouter

router = APIRouter()

@router.get("/api/kefu/health")
async def health():
    return {"status": "UP", "service": "platform-kefu", "version": "1.2.0"}
