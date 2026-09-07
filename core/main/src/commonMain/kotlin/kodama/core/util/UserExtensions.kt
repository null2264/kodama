package kodama.core.util

import io.github.jan.supabase.auth.user.UserInfo
import kodama.core.data.ContestUser
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Return user's app role, can be admin or user (superadmin also a valid role but is unused at the moment)
 */
val UserInfo.kodamaRole: String? get() = (appMetadata?.get("role") as? JsonPrimitive)?.contentOrNull
val UserInfo?.isAdmin: Boolean get() = this?.kodamaRole == "admin"

fun UserInfo.isJudge(contestUsers: List<ContestUser>): Boolean {
    val contestUser = contestUsers.find { it.user_id == id }
    return contestUser?.role?.contains("judge") == true
}
