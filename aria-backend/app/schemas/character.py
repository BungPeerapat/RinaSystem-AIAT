import uuid
from pydantic import BaseModel, Field


class CharacterResponse(BaseModel):
    id: uuid.UUID
    name: str
    model_id: int
    speaker_id: int
    emoji: str
    sort_order: int

    model_config = {"from_attributes": True}


class CharacterCreateRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=100)
    model_id: int = Field(0, ge=0)
    speaker_id: int = Field(0, ge=0)
    emoji: str = Field("🎭", max_length=10)
    sort_order: int = Field(0, ge=0)


class CharacterListResponse(BaseModel):
    characters: list[CharacterResponse]
    total: int
