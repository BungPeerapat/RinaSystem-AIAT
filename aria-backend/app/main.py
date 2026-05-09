import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from sqlalchemy import update

from app.api import api_router, ws_api_router
from app.core.exceptions import AriaException, aria_exception_handler, internal_error_handler
from app.core.logging import setup_logging
from app.database import async_session, engine
from app.models.user import User

logger = logging.getLogger("aria")

limiter = Limiter(key_func=get_remote_address)


@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging()
    logger.info("ARIA System starting...")
    # Reset all users to offline on startup (ป้องกันค้าง online จาก crash/shutdown)
    try:
        async with async_session() as db:
            await db.execute(update(User).values(is_online=False))
            await db.commit()
        logger.info("Reset all users to offline")
    except Exception as e:
        logger.warning(f"Failed to reset online status: {e}")
    yield
    await engine.dispose()
    logger.info("ARIA System shut down.")


app = FastAPI(
    title="ARIA System",
    description="Adaptive Remote Intelligence Assistant — Backend API",
    version="0.1.0",
    lifespan=lifespan,
)

app.state.limiter = limiter

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.add_exception_handler(AriaException, aria_exception_handler)
app.add_exception_handler(Exception, internal_error_handler)


@app.exception_handler(RateLimitExceeded)
async def rate_limit_handler(request: Request, exc: RateLimitExceeded) -> JSONResponse:
    return JSONResponse(
        status_code=429,
        content={"detail": "Too many requests", "error_code": "RATE_LIMIT_EXCEEDED"},
    )


app.include_router(api_router)
app.include_router(ws_api_router)  # WebSocket routes: /ws/user/{id}, /ws/admin/{id}
