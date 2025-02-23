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

package kodama.preference

interface PreferenceStore {

    fun getString(key: String, defaultValue: String = ""): Preference<String>

    fun getLong(key: String, defaultValue: Long = 0): Preference<Long>

    fun getInt(key: String, defaultValue: Int = 0): Preference<Int>

    fun getFloat(key: String, defaultValue: Float = 0f): Preference<Float>

    fun getBoolean(key: String, defaultValue: Boolean = false): Preference<Boolean>

    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Preference<Set<String>>

    fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T>

    fun getAll(): Map<String, *>
}

inline fun <reified T : Enum<T>> PreferenceStore.getEnum(
    key: String,
    defaultValue: T,
): Preference<T> {
    return getObject(
        key = key,
        defaultValue = defaultValue,
        serializer = { it.name },
        deserializer = {
            try {
                enumValueOf(it)
            } catch (e: IllegalArgumentException) {
                defaultValue
            }
        },
    )
}
