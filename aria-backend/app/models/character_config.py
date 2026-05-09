import uuid

from sqlalchemy import Integer, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.models.base import BaseModel


class CharacterConfig(BaseModel):
    """Global character list — Admin ตั้งค่าไว้ User ทุกคน trigger ได้"""

    __tablename__ = "character_configs"

    name: Mapped[str] = mapped_column(String(100), nullable=False)
    model_id: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    speaker_id: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    emoji: Mapped[str] = mapped_column(String(10), nullable=False, server_default="🎭")
    sort_order: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")
