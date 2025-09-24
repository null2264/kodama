import asyncio
import asyncpg
import os
import click

from colorama import Fore, Style
from typing import Literal
from gotrue import User
from supabase import Client, create_client
from dotenv import dotenv_values

from core.migration import Migrations

environ: dict[str, str | None] = {
    **os.environ,
    **dotenv_values(".env"),
    **dotenv_values(".env.local"),
}

demo_pass = "demo12345"

class Class:
    Madya: str = "11943061-aa9d-4cc0-90f6-2ca7b70bc1b5"
    Prospek: str = "8f8e46fd-75e9-443e-a1f2-7ffab68ece31"
    Pratama: str = "fb85ea10-0fc5-40b9-9e27-f236962c8271"

def login(supabase: Client, user: Literal["demo"] | Literal["admin"]) -> User:
    resp = supabase.auth.get_user()
    if resp:
        supabase.auth.sign_out()

    _ = supabase.auth.sign_in_with_password(
        {
            "email": f"{user}@example.com",
            "password": demo_pass,
        }
    )

    resp = supabase.auth.get_user()
    if not resp:
        raise RuntimeError()

    return resp.user

#region Admin (and Judge)
def create_contest(supabase: Client) -> str:
    resp = (
        supabase.schema("kodama")
        .table("contests")
        .insert({ "name": "Test", "description": "Lorem ipsum" })
        .execute()
    )
    id: str = resp.data[0]["id"]

    # Adding classes to the contest draft
    _ = (
        supabase.schema("kodama")
        .table("contest_classes")
        .insert([
            { "contest_id": id, "class_id": Class.Prospek },
            { "contest_id": id, "class_id": Class.Madya },
            { "contest_id": id, "class_id": Class.Pratama },
        ])
        .execute()
    )

    return id

def finalize_contest(supabase: Client, contest_id: str):
    """
    Mimicing admin finalizing contest draft and accepting registration.
    """
    _ = (
        supabase.schema("kodama")
        .table("contests")
        .update({ "state": "accepting" })
        .eq("id", contest_id)
        .execute()
    )

def verify_bonsai(supabase: Client, bonsai_id: str):
    _ = (
        supabase.schema("kodama")
        .rpc("verify_bonsai", { "bonsai_id": bonsai_id })
        .execute()
    )

def close_contest_registration(supabase: Client, contest_id: str):
    _ = (
        supabase.schema("kodama")
        .table("contests")
        .update({ "state": "closed" })
        .eq("id", contest_id)
        .execute()
    )

def review_contest(supabase: Client, contest_id: str):
    _ = (
        supabase.schema("kodama")
        .table("contests")
        .update({ "state": "reviewing_phase_1" })
        .eq("id", contest_id)
        .execute()
    )

def vote_contest(supabase: Client, contest_id: str):
    _ = (
        supabase.schema("kodama")
        .table("contests")
        .update({ "state": "reviewing_phase_2" })
        .eq("id", contest_id)
        .execute()
    )
#endregion

#region Normal User
def _choose_class(supabase: Client, contest_id: str, class_id: str) -> str:
    response = (
        supabase.schema("kodama")
        .table("contest_classes")
        .select("id")
        .eq("contest_id", contest_id)
        .eq("class_id", class_id)
        .execute()
    )
    return response.data[0]["id"]

def register_bonsai(supabase: Client, contest_id: str) -> str:
    response = (
        supabase.schema("kodama")
        .table("bonsai")
        .insert({
            "name": "Test",
            "contest_id": contest_id,
            "contest_class_id": _choose_class(supabase, contest_id, Class.Pratama),
        })
        .execute()
    )
    return response.data[0]["id"]

def finalize_bonsai(supabase: Client, bonsai_id: str):
    _ = (
        supabase.schema("kodama")
        .rpc("finalize_bonsai", { "bonsai_id": bonsai_id })
        .execute()
    )
#endregion

@click.group()
def main():
    pass

@main.group()
def db():
    pass

@db.command()
def init():
    asyncio.run(run_upgrade())

@db.command()
def upgrade():
    asyncio.run(run_upgrade())

async def get_conn() -> asyncpg.Connection:
    uri: str = environ["PG_URI"] or ""
    return await asyncpg.connect(uri)

async def run_upgrade():
    migration = await Migrations.load(connection = await get_conn())
    migration.display()

async def reset():
    conn = await get_conn()
    await Migrations.reset(conn)


class TestLogger():
    @staticmethod
    def log(message: str) -> None:
        print(f"[{Fore.BLUE}INFO{Style.RESET_ALL}] {message}")

    @staticmethod
    def init(message: str) -> None:
        print(f"[{Style.DIM}TESTING{Style.RESET_ALL}] {message}", end="\x1b[1K\r")

    @staticmethod
    def success(message: str) -> None:
        print(f"[{Fore.GREEN}SUCCESS{Style.RESET_ALL}] {message}")

    @staticmethod
    def fail(message: str) -> None:
        print(f"[{Fore.RED}FAIL{Style.RESET_ALL}] {message}")

@db.command()
def test():
    """
    Test user flow, mimicing how the user(s) would use the app.
    """
    url: str = environ["SUPABASE_URL"] or ""
    key: str = environ["SUPABASE_KEY"] or ""
    supabase: Client = create_client(url, key)

    # --- Admin creates a contest and finalizing it
    TestLogger.log("Logging in as Admin...")
    _ = login(supabase, "admin")
    TestLogger.init("Creating contest...")
    contest_id = create_contest(supabase)
    TestLogger.success("Contest has been created")
    TestLogger.init("Finalizing contest...")
    finalize_contest(supabase, contest_id)
    TestLogger.success("Contest has been finalized")

    # --- Contestants registering their bonsai
    TestLogger.log("Logging in as Demo...")
    _ = login(supabase, "demo")
    TestLogger.init("Registering bonsai to a contest as a contestant...")
    bonsai_id_1 = register_bonsai(supabase, contest_id)
    TestLogger.success(f"Bonsai registered with ID {bonsai_id_1}")
    TestLogger.init("Finalizing bonsai...")
    finalize_bonsai(supabase, bonsai_id_1)
    TestLogger.success(f"Bonsai with ID {bonsai_id_1} has been finalized")
    TestLogger.init("Registering bonsai to a contest as a contestant...")
    bonsai_id_2 = register_bonsai(supabase, contest_id)
    TestLogger.success(f"Bonsai registered with ID {bonsai_id_2}")
    TestLogger.init("Finalizing bonsai...")
    finalize_bonsai(supabase, bonsai_id_2)
    TestLogger.success(f"Bonsai with ID {bonsai_id_2} has been finalized")

    # --- Admin closes the registration
    TestLogger.log("Logging in as Admin...")
    _ = login(supabase, "admin")
    TestLogger.init(f"Verifying bonsai with ID {bonsai_id_1} as a admin...")
    verify_bonsai(supabase, bonsai_id_1)
    TestLogger.success(f"Bonsai with ID {bonsai_id_1} has been verified")
    TestLogger.init(f"Verifying bonsai with ID {bonsai_id_2} as a admin...")
    verify_bonsai(supabase, bonsai_id_2)
    TestLogger.success(f"Bonsai with ID {bonsai_id_2} has been verified")
    TestLogger.init("Closing contest as a admin...")
    close_contest_registration(supabase, contest_id)
    TestLogger.success("Contest has been closed")

    # --- A contestant try to register their bonsai when the contest is already closing its registration
    TestLogger.log("Logging in as Demo...")
    _ = login(supabase, "demo")
    try:
        TestLogger.init("Registering bonsai to a CLOSED contest as a contestant...")
        bonsai_id_3 = register_bonsai(supabase, contest_id)
        TestLogger.fail(f"Bonsai registered with ID {bonsai_id_3}")
    except:
        TestLogger.success(f"Bonsai failed to register to a CLOSED contest")


if __name__ == "__main__":
    main()
