import os
from typing import Literal
from gotrue import User
from supabase import Client, create_client
from dotenv import load_dotenv

_ = load_dotenv()

demo_pass = "demo12345"

def login(supabase: Client, user: Literal["demo"] | Literal["admin"]) -> User:
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

def create_contest(supabase: Client):
    resp = supabase.schema("kodama").table("contests").insert({ "name": "Test", "description": "Lorem ipsum" }).execute()
    id: str = resp.data[0]["id"]

    # Adding classes to the contest
    (
        supabase.schema("kodama")
        .table("contest_classes")
        .insert([
            { "contest_id": id, "class_id": "8f8e46fd-75e9-443e-a1f2-7ffab68ece31" },
            { "contest_id": id, "class_id": "fb85ea10-0fc5-40b9-9e27-f236962c8271" },
            { "contest_id": id, "class_id": "11943061-aa9d-4cc0-90f6-2ca7b70bc1b5" },
        ])
        .execute()
    )

def main():
    """
    Mimicing how the app would flow as the user used it
    """
    url: str = os.environ["SUPABASE_URL"]
    key: str = os.environ["SUPABASE_KEY"]
    supabase: Client = create_client(url, key)

    user = login(supabase, "admin")
    create_contest(supabase)


if __name__ == "__main__":
    main()
