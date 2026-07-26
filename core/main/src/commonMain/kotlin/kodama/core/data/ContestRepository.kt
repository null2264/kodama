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

@Serializable
data class BonsaiClass(
    val id: String,
    val name: String,
    val description: String? = null,
)

@Serializable
data class ContestClass(
    val id: String,
    val contest_id: String,
    val class_id: String,
)

class ContestRepository(private val client: SupabaseClient) {

    suspend fun getOpenContests(): List<Contest> {
        return client.from("kodama", "contests")
            .select {
                filter {
                    or {
                        eq("state", "accepting")
                        if (isAdmin()) eq("state", "draft")
                    }
                }
            }
            .decodeList<Contest>()
    }

    suspend fun isAdmin(): Boolean {
        return client.postgrest.rpc("is_admin") {
            schema = "kodama"
        }.decodeAs<Boolean>()
    }

    suspend fun getBonsaiClasses(): List<BonsaiClass> {
        return client.from("kodama", "bonsai_classes")
            .select {}
            .decodeList<BonsaiClass>()
    }

    suspend fun createContest(name: String, description: String?): String {
        val result = client.from("kodama", "contests")
            .insert(
                buildMap {
                    put("name", name)
                    if (description != null) put("description", description)
                }
            ) {
                select()
            }
            .decodeSingle<Contest>()
        return result.id
    }

    suspend fun addContestClasses(contestId: String, classIds: List<String>) {
        if (classIds.isEmpty()) return
        client.from("kodama", "contest_classes")
            .insert(classIds.map { classId ->
                mapOf("contest_id" to contestId, "class_id" to classId)
            })
    }

    suspend fun getContestById(contestId: String): Contest? {
        return client.from("kodama", "contests")
            .select {
                filter { eq("id", contestId) }
            }
            .decodeList<Contest>()
            .firstOrNull()
    }

    suspend fun getContestClassIds(contestId: String): List<String> {
        return client.from("kodama", "contest_classes")
            .select {
                filter { eq("contest_id", contestId) }
            }
            .decodeList<ContestClass>()
            .map { it.class_id }
    }
}
