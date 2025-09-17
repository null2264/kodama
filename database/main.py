import asyncio
import asyncpg
import os
import click

from typing import Literal
from gotrue import User
from supabase import Client, create_client
from dotenv import load_dotenv

_ = load_dotenv()

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
def upgrade():
    asyncio.run(run_upgrade())

async def run_upgrade():
    return

    uri: str = os.environ["PG_URI"]
    conn: asyncpg.Connection = await asyncpg.connect(uri)

    await conn.execute("""
        CREATE TABLE IF NOT EXISTS public.schema_migrations (
            version TEXT PRIMARY KEY,
            applied_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        )
    """)

    # TODO: Grab migration files and execute them
    migration_file = ""
    async with conn.transaction():
        #await conn.execute()
        await conn.execute(f"INSERT INTO schema_migrations (version) VALUES ('{migration_file}')")

@db.command()
def test():
    """
    Test user flow, mimicing how the user(s) would use the app.
    """
    url: str = os.environ["SUPABASE_URL"]
    key: str = os.environ["SUPABASE_KEY"]
    supabase: Client = create_client(url, key)

    # --- Admin creates a contest and finalizing it
    _ = login(supabase, "admin")
    contest_id = create_contest(supabase)
    finalize_contest(supabase, contest_id)

    # --- Contestants registering their bonsai
    _ = login(supabase, "demo")
    bonsai_id_1 = register_bonsai(supabase, contest_id)
    finalize_bonsai(supabase, bonsai_id_1)
    bonsai_id_2 = register_bonsai(supabase, contest_id)
    finalize_bonsai(supabase, bonsai_id_2)

    # --- Admin closes the registration
    _ = login(supabase, "admin")
    verify_bonsai(supabase, bonsai_id_1)
    verify_bonsai(supabase, bonsai_id_2)
    close_contest_registration(supabase, contest_id)

    # --- A contestant try to register their bonsai when the contest is already closing its registration
    _ = login(supabase, "demo")
    try:
        _ = register_bonsai(supabase, contest_id)
    except:
        print("Failed to register as it should be")


if __name__ == "__main__":
    main()
