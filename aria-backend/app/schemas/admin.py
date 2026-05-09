import uuid
from datetime import datetime

from pydantic import BaseModel

from app.models.stream_session import StreamType
from app.models.user import UserStatus
from app.schemas.auth import UserResponse


class UserListResponse(BaseModel):
    users: list[UserResponse]
    total: int
    online_count: int


class UpdateStatusRequest(BaseModel):
    status: UserStatus


class UpdateRoleRequest(BaseModel):
    role: str


class StreamSessionResponse(BaseModel):
    id: uuid.UUID
    admin_id: uuid.UUID
    user_id: uuid.UUID
    stream_type: StreamType
    recording_path: str | None = None
    duration_seconds: int | None = None
    started_at: datetime
    ended_at: datetime | None = None

    model_config = {"from_attributes": True}


class DashboardResponse(BaseModel):
    total_users: int
    online_count: int
    total_streams: int
    total_messages: int
