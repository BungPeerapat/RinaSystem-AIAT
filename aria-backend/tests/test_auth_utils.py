"""Tests for app.core.auth_utils — pure functions, no DB needed."""
import os

os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")
os.environ.setdefault("SECRET_KEY", "test-secret-key")
os.environ.setdefault("JWT_SECRET_KEY", "test-jwt-secret-key-for-testing")

from app.core.auth_utils import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)


class TestHashPassword:
    def test_hash_password_returns_string(self):
        h = hash_password("mypassword")
        assert isinstance(h, str)
        assert h != "mypassword"

    def test_hash_password_different_each_time(self):
        h1 = hash_password("same")
        h2 = hash_password("same")
        assert h1 != h2  # bcrypt uses random salt


class TestVerifyPassword:
    def test_verify_correct_password(self):
        h = hash_password("correct")
        assert verify_password("correct", h) is True

    def test_verify_wrong_password(self):
        h = hash_password("correct")
        assert verify_password("wrong", h) is False


class TestCreateAccessToken:
    def test_creates_decodable_token(self):
        token = create_access_token({"sub": "user-123"})
        payload = decode_token(token)
        assert payload is not None
        assert payload["sub"] == "user-123"
        assert payload["type"] == "access"

    def test_contains_exp_claim(self):
        token = create_access_token({"sub": "user-123"})
        payload = decode_token(token)
        assert "exp" in payload


class TestCreateRefreshToken:
    def test_creates_refresh_type(self):
        token = create_refresh_token({"sub": "user-456"})
        payload = decode_token(token)
        assert payload is not None
        assert payload["sub"] == "user-456"
        assert payload["type"] == "refresh"


class TestDecodeToken:
    def test_decode_valid_token(self):
        token = create_access_token({"sub": "test"})
        payload = decode_token(token)
        assert payload is not None
        assert payload["sub"] == "test"

    def test_decode_invalid_token_returns_none(self):
        assert decode_token("invalid.token.here") is None

    def test_decode_empty_string_returns_none(self):
        assert decode_token("") is None
