from __future__ import annotations
from pathlib import Path

import asyncpg
import datetime
import re

import click


REVISION_FILE = re.compile(r"(?P<is_test>T?)(?P<timestamp>[0-9]+)__(?P<description>.+).sql")


class Revision:
    __slots__: tuple[str, ...] = ("is_test", "timestamp", "description", "file")

    def __init__(self, *, is_test: bool, timestamp: int, description: str, file: Path) -> None:
        self.is_test: bool = is_test
        self.timestamp: int = timestamp
        self.description: str = description
        self.file: Path = file

    @classmethod
    def from_match(cls, match: re.Match[str], file: Path):
        return cls(
            is_test=match.group("is_test") == "T",
            timestamp=int(match.group("timestamp")),
            description=match.group("description"),
            file=file,
        )


class Migrations:
    def __init__(self, *, connection: asyncpg.Connection, load_test: bool = False):
        self.root: Path = Path("migrations/")
        self.connection: asyncpg.Connection = connection
        self.revisions: dict[int, Revision] = self.get_revisions(load_test)
        self.executed: list[int] = []

    @staticmethod
    async def load(*, connection: asyncpg.Connection, load_test: bool = False) -> Migrations:
        migration = Migrations(connection=connection, load_test=load_test)
        await migration._load()
        return migration

    def get_revisions(self, load_test: bool) -> dict[int, Revision]:
        result: dict[int, Revision] = {}
        for file in self.root.glob("*.sql"):
            match = REVISION_FILE.match(file.name)
            if match is not None:
                rev = Revision.from_match(match, file)
                if rev.is_test and not load_test:
                    continue
                result[rev.timestamp] = rev

        return result

    async def _load(self) -> None:
        await self.connection.execute("""
            CREATE TABLE IF NOT EXISTS public.schema_migrations (
                version NUMBER PRIMARY KEY,
                applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
            )
        """)
        await self.connection.execute("ALTER TABLE public.schema_migrations ENABLE ROW LEVEL SECURITY")
        data = await self.connection.fetch("SELECT * FROM public.schema_migrations")
        self.executed = [int(i["version"]) for i in data]

    def is_next_revision_taken(self, revision: int) -> bool:
        return revision in self.revisions

    @property
    def ordered_revisions(self) -> list[Revision]:
        return sorted(self.revisions.values(), key=lambda r: r.timestamp)

    def create_revision(self, reason: str, is_test: bool = False) -> Revision:
        timestamp = datetime.datetime.now(datetime.timezone.utc)
        assert not self.is_next_revision_taken(int(timestamp.timestamp()))

        cleaned = re.sub(r"\s", "_", reason)
        filename = f"{"T" if is_test else ""}{int(timestamp.timestamp())}__{cleaned}.sql"
        path = self.root / filename

        stub = f"-- Creation Date: {timestamp} UTC\n-- Reason: {reason}\n\n"

        with open(path, "w", encoding="utf-8", newline="\n") as fp:
            fp.write(stub)

        return Revision(
            is_test=is_test, description=reason, timestamp=int(timestamp.timestamp()), file=path
        )

    async def upgrade(self) -> int:
        ordered = self.ordered_revisions
        successes = 0
        async with self.connection.transaction():
            for revision in ordered:
                if revision.timestamp not in self.executed:
                    click.echo(f"Applying '{revision.file}'")
                    sql = revision.file.read_text("utf-8")
                    await self.connection.execute(sql)
                    await self.connection.execute(
                        f"INSERT INTO schema_migrations (version) VALUES ('{revision.timestamp}')"
                    )

                    successes += 1

        return successes

    def display(self) -> None:
        ordered = self.ordered_revisions
        for revision in ordered:
            if revision.timestamp not in self.executed:
                sql = revision.file.read_text("utf-8")
                click.echo(sql)

    @staticmethod
    async def reset(*, connection: asyncpg.Connection) -> None:
        await connection.execute("DROP SCHEMA IF EXISTS kodama CASCADE")
        await connection.execute("DELETE FROM auth.users WHERE email ~ '@test.example.com$'")
        await connection.execute("DELETE FROM public.schema_migrations")
