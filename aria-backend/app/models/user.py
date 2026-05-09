import enum
import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Index, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.models.base import BaseModel


class UserStatus(str, enum.Enum):
    active = "active"
    blocked = "blocked"
    suspended = "suspended"


class User(BaseModel):
    __tablename__ = "users"

    email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    display_name: Mapped[str] = mapped_column(String(100), nullable=False)
    role_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("master_roles.id"), nullable=False
    )
    status: Mapped[UserStatus] = mapped_column(
        Enum(UserStatus, name="user_status"), nullable=False, server_default="active"
    )
    fcm_token: Mapped[str | None] = mapped_column(String(255), nullable=True)
    device_info: Mapped[dict | None] = mapped_column(JSONB, nullable=True)
    is_online: Mapped[bool] = mapped_column(Boolean, server_default="false", nullable=False)
    is_test_user: Mapped[bool] = mapped_column(Boolean, server_default="false", nullable=False)
    last_active_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    role_ref: Mapped["MasterRole"] = relationship("MasterRole", back_populates="users", lazy="joined")  # noqa: F821

    @property
    def role(self) -> str:
        return self.role_ref.name if self.role_ref else "user"

    __table_args__ = (
        Index("idx_users_email", "email"),
        Index("idx_users_role_id", "role_id"),
        Index("idx_users_status", "status"),
    )
