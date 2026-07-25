package kodama.core.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable

@Serializable
data class Contest(
    val id: String,
    val name: String,
    val description: String? = null,
    val state: String,
    val banner_path: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

class ContestRepository(private val client: SupabaseClient) {

    suspend fun getOpenContests(): List<Contest> {
        return client.from("kodama", "contests")
            .select {
                filter {
                    eq("state", "accepting")
                }
            }
            .decodeList<Contest>()
    }

    suspend fun isAdmin(): Boolean {
        return client.postgrest.rpc("is_admin") {
            schema = "kodama"
        }.decodeAs<Boolean>()
    }
}
