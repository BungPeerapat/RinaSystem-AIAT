from pydantic import BaseModel, Field


class UpdateUserRequest(BaseModel):
    display_name: str | None = Field(None, min_length=1, max_length=100)


class UpdateFCMTokenRequest(BaseModel):
    fcm_token: str
