from fastapi import APIRouter

from app.api.routes.admin import router as admin_router
from app.api.routes.app_update import router as app_update_router
from app.api.routes.auth import router as auth_router
from app.api.routes.health import router as health_router
from app.api.routes.users import router as users_router
from app.api.routes.ws import router as ws_router

api_router = APIRouter(prefix="/api")
api_router.include_router(health_router)
api_router.include_router(auth_router)
api_router.include_router(users_router)
api_router.include_router(admin_router)
api_router.include_router(app_update_router)

# WS router ไม่ใช้ /api prefix — include ตรงบน app ใน main.py
ws_api_router = ws_router
