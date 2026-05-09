import logging

from fastapi import APIRouter, Depends
from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db

router = APIRouter(tags=["Health"])
logger = logging.getLogger("aria")


@router.get("/health")
async def health_check(db: AsyncSession = Depends(get_db)) -> dict:
    db_status = "connected"
    try:
        await db.execute(text("SELECT 1"))
    except Exception:
        db_status = "error"
        logger.error("Database health check failed")

    status = "ok" if db_status == "connected" else "degraded"

    return {"status": status, "database": db_status}
