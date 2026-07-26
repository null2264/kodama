package kodama.core.util

import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Return user's app role, can be admin or user (superadmin also a valid role but is unused at the moment)
 */
val UserInfo.kodamaRole: String? get() = (appMetadata?.get("role") as? JsonPrimitive)?.contentOrNull
