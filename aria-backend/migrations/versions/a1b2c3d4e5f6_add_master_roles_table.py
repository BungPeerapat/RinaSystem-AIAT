"""add master_roles table and refactor user role

Revision ID: a1b2c3d4e5f6
Revises: 614d74b08d7c
Create Date: 2026-03-11 22:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = 'a1b2c3d4e5f6'
down_revision: Union[str, None] = '614d74b08d7c'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # 1. Create master_roles table
    op.create_table('master_roles',
        sa.Column('id', sa.UUID(), server_default=sa.text('gen_random_uuid()'), nullable=False),
        sa.Column('name', sa.String(length=50), nullable=False),
        sa.Column('description', sa.String(length=255), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=False),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('name')
    )

    # 2. Seed default roles
    op.execute("""
        INSERT INTO master_roles (id, name, description)
        VALUES
            (gen_random_uuid(), 'admin', 'Administrator with full access'),
            (gen_random_uuid(), 'user', 'Regular user')
    """)

    # 3. Add role_id column (nullable first for migration)
    op.add_column('users', sa.Column('role_id', sa.UUID(), nullable=True))

    # 4. Migrate existing role data
    op.execute("""
        UPDATE users
        SET role_id = mr.id
        FROM master_roles mr
        WHERE users.role::text = mr.name
    """)

    # 5. Make role_id NOT NULL and add FK
    op.alter_column('users', 'role_id', nullable=False)
    op.create_foreign_key('fk_users_role_id', 'users', 'master_roles', ['role_id'], ['id'])
    op.create_index('idx_users_role_id', 'users', ['role_id'], unique=False)

    # 6. Drop old role column and index
    op.drop_index('idx_users_role', table_name='users')
    op.drop_column('users', 'role')

    # 7. Drop old enum type
    op.execute("DROP TYPE IF EXISTS user_role")


def downgrade() -> None:
    # 1. Recreate user_role enum
    user_role = postgresql.ENUM('admin', 'user', name='user_role', create_type=False)
    op.execute("CREATE TYPE user_role AS ENUM ('admin', 'user')")

    # 2. Add role column back
    op.add_column('users', sa.Column('role', user_role, server_default='user', nullable=True))

    # 3. Migrate data back
    op.execute("""
        UPDATE users
        SET role = mr.name::user_role
        FROM master_roles mr
        WHERE users.role_id = mr.id
    """)

    # 4. Make role NOT NULL
    op.alter_column('users', 'role', nullable=False)
    op.create_index('idx_users_role', 'users', ['role'], unique=False)

    # 5. Drop role_id column and FK
    op.drop_index('idx_users_role_id', table_name='users')
    op.drop_constraint('fk_users_role_id', 'users', type_='foreignkey')
    op.drop_column('users', 'role_id')

    # 6. Drop master_roles table
    op.drop_table('master_roles')
