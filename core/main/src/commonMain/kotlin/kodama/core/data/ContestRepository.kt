package kodama.core.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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

@Serializable
data class BonsaiContestClass(
    val id: String,
    val contest_id: String,
    val class_id: String,
    val data: BonsaiClass,
)

@Serializable
data class Bonsai(
    val id: String,
    val name: String,
    val owner_id: String,
    val contest_id: String,
    val contest_class_id: String,
    val created_at: String? = null,
    val pict_path: String? = null,
    val state: String,
    val payment_proof_path: String? = null,
)

@Serializable
data class Review(
    val id: String,
    val bonsai_id: String,
    val judge_id: String,
    val scores: Map<String, Int>,
    val total_score: Int,
    val comments: String? = null,
    val created_at: String? = null,
)

@Serializable
data class ContestUser(
    val user_id: String,
    val email: String,
    val role: String,
    val contest_class_id: String? = null,
)

class ContestRepository(private val client: SupabaseClient) {

    suspend fun getOpenContests(): List<Contest> {
        return client.from("kodama", "contests")
            .select {
                filter {
                    or {
                        eq("state", "accepting")
                        eq("state", "draft")
                    }
                }
            }
            .decodeList<Contest>()
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

    suspend fun editContestBanner(contestId: String, bannerPath: String? = null) {
        client.from("kodama", "contests")
            .update({
                set("banner_path", bannerPath)
            }) {
                filter {
                    eq("id", contestId)
                }
            }
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

    suspend fun getContestClasses(contestId: String): List<ContestClass> {
        return client.from("kodama", "contest_classes")
            .select {
                filter { eq("contest_id", contestId) }
            }
            .decodeList<ContestClass>()
    }

    suspend fun getContestClassIds(contestId: String): List<String> {
        return getContestClasses(contestId).map { it.class_id }
    }

    suspend fun getBonsaiContestClasses(contestId: String): List<BonsaiContestClass> {
        val bonsaiClasses = getBonsaiClasses().associateBy { it.id }
        return getContestClasses(contestId).map {
            BonsaiContestClass(it.id, it.contest_id, it.class_id, bonsaiClasses[it.class_id]!!)
        }
    }

    suspend fun updateContest(contestId: String, name: String, description: String?) {
        client.from("kodama", "contests")
            .update({
                set("name", name)
                set("description", description)
            }) {
                filter { eq("id", contestId) }
            }
    }

    suspend fun updateContestState(contestId: String, newState: String) {
        client.from("kodama", "contests")
            .update({
                set("state", newState)
            }) {
                filter { eq("id", contestId) }
            }
    }

    suspend fun removeContestClasses(contestId: String) {
        client.from("kodama", "contest_classes")
            .delete {
                filter { eq("contest_id", contestId) }
            }
    }

    suspend fun deleteContest(contestId: String) {
        client.from("kodama", "contests")
            .delete {
                filter { eq("id", contestId) }
            }
    }

    suspend fun getBonsaiWithMetadataForContest(contestId: String): List<Bonsai> {
        return client.postgrest.rpc(
            "get_bonsai_with_metadata",
            mapOf("p_contest_id" to contestId),
        ) {
            schema = "kodama"
        }.decodeList<Bonsai>()
    }

    suspend fun getReviewsForContest(contestId: String): List<Review> {
        val bonsaiIds = getBonsaiWithMetadataForContest(contestId).map { it.id }
        if (bonsaiIds.isEmpty()) return emptyList()
        return client.from("kodama", "reviews")
            .select {
                filter {
                    isIn("bonsai_id", bonsaiIds)
                }
            }
            .decodeList<Review>()
    }

    suspend fun getContestUsers(contestId: String): List<ContestUser> {
        return client.postgrest.rpc(
            "get_contest_users",
            mapOf("p_contest_id" to contestId),
        ) {
            schema = "kodama"
        }.decodeList<ContestUser>()
    }

    suspend fun verifyBonsai(bonsaiId: String): Boolean {
        return client.postgrest.rpc(
            "verify_bonsai",
            mapOf("bonsai_id" to bonsaiId),
        ) {
            schema = "kodama"
        }.decodeAs()
    }

    suspend fun createBonsai(contestId: String, classId: String, name: String): String {
        val result = client.from("kodama", "bonsai")
            .insert(
                mapOf(
                    "contest_id" to contestId,
                    "contest_class_id" to classId,
                    "name" to name,
                )
            ) {
                select()
            }
            .decodeSingle<Bonsai>()
        return result.id
    }

    suspend fun finalizeBonsai(bonsaiId: String): Boolean {
        return client.postgrest.rpc(
            "finalize_bonsai",
            mapOf("bonsai_id" to bonsaiId),
        ) {
            schema = "kodama"
        }.decodeAs()
    }

    suspend fun deleteBonsai(bonsaiId: String) {
        client.from("kodama", "bonsai")
            .delete {
                filter { eq("id", bonsaiId) }
            }
    }

    suspend fun setBonsaiPictPath(bonsaiId: String, path: String): Boolean {
        return client.postgrest.rpc(
            "set_bonsai_pict_path",
            mapOf("bonsai_id" to bonsaiId, "path" to path),
        ) {
            schema = "kodama"
        }.decodeAs()
    }

    suspend fun setBonsaiPaymentProofPath(bonsaiId: String, path: String) {
        client.postgrest.rpc(
            "set_bonsai_payment_proof_path",
            mapOf("p_bonsai_id" to bonsaiId, "p_path" to path),
        ) {
            schema = "kodama"
        }
    }

    suspend fun getMyBonsaiForContest(contestId: String): List<Bonsai> {
        val userId = client.auth.currentUserOrNull()?.id ?: return emptyList()
        return getBonsaiWithMetadataForContest(contestId).filter { it.owner_id == userId }
    }
}
