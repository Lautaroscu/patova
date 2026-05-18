"""add_user_behavior_tables

Revision ID: a7d1e2f3b4c5
Revises: 4c3b3f82fc0f
Create Date: 2026-05-17 10:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision: str = "a7d1e2f3b4c5"
down_revision: Union[str, None] = "4c3b3f82fc0f"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "user_preferences",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("user_id", sa.String(length=256), nullable=False),
        sa.Column("strict_mode", sa.Boolean(), nullable=False),
        sa.Column("block_unknown", sa.Boolean(), nullable=False),
        sa.Column("spam_threshold", sa.Float(), nullable=False),
        sa.Column("sync_enabled", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("user_id"),
    )

    op.create_table(
        "whitelist_entries",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("user_id", sa.String(length=256), nullable=False),
        sa.Column("phone_hash", sa.String(length=128), nullable=False),
        sa.Column("label", sa.String(length=200), nullable=True),
        sa.Column("added_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_whitelist_entries_user_id", "whitelist_entries", ["user_id"])
    op.create_index(
        "ix_whitelist_entries_user_phone",
        "whitelist_entries",
        ["user_id", "phone_hash"],
        unique=True,
    )

    op.create_table(
        "blacklist_entries",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("user_id", sa.String(length=256), nullable=False),
        sa.Column("phone_hash", sa.String(length=128), nullable=False),
        sa.Column("reason", sa.Text(), nullable=True),
        sa.Column("added_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_blacklist_entries_user_id", "blacklist_entries", ["user_id"])
    op.create_index(
        "ix_blacklist_entries_user_phone",
        "blacklist_entries",
        ["user_id", "phone_hash"],
        unique=True,
    )


def downgrade() -> None:
    op.drop_index("ix_blacklist_entries_user_phone", table_name="blacklist_entries")
    op.drop_index("ix_blacklist_entries_user_id", table_name="blacklist_entries")
    op.drop_table("blacklist_entries")

    op.drop_index("ix_whitelist_entries_user_phone", table_name="whitelist_entries")
    op.drop_index("ix_whitelist_entries_user_id", table_name="whitelist_entries")
    op.drop_table("whitelist_entries")

    op.drop_table("user_preferences")
