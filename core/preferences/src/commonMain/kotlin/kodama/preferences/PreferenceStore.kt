/*
 * Copyright © 2015 Javier Tomás
 * Copyright © 2024 null2264
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package kodama.preferences

interface PreferenceStore {

    interface Factory {
        fun default(): PreferenceStore

        fun get(name: String): PreferenceStore

        fun create(name: String? = null): PreferenceStore

        companion object {
            const val DEFAULT_PREFERENCE_NAME = "kodama"
        }
    }

    fun getString(key: String, default: String = ""): Preference<String>

    fun getLong(key: String, default: Long = 0): Preference<Long>

    fun getInt(key: String, default: Int = 0): Preference<Int>

    fun getFloat(key: String, default: Float = 0f): Preference<Float>

    fun getBoolean(key: String, default: Boolean = false): Preference<Boolean>

    fun <T> getObject(
        key: String,
        default: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T>
}

inline fun <reified T : Enum<T>> PreferenceStore.getEnum(
    key: String,
    default: T,
): Preference<T> {
    return getObject(
        key = key,
        default = default,
        serializer = { it.name },
        deserializer = {
            try {
                enumValueOf(it)
            } catch (e: IllegalArgumentException) {
                default
            }
        },
    )
}
