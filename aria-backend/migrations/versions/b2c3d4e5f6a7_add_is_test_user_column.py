"""add is_test_user column

Revision ID: b2c3d4e5f6a7
Revises: a1b2c3d4e5f6
Create Date: 2026-03-11 22:30:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision: str = 'b2c3d4e5f6a7'
down_revision: Union[str, None] = 'a1b2c3d4e5f6'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column('users', sa.Column('is_test_user', sa.Boolean(), server_default='false', nullable=False))

    # Mark existing test accounts as test users
    op.execute("""
        UPDATE users
        SET is_test_user = true
        WHERE email IN ('test@aria.local', 'user2@aria.local')
    """)


def downgrade() -> None:
    op.drop_column('users', 'is_test_user')
