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

package kodama.core.preference

import kodama.core.preference.DarwinPreference.*
import kodama.core.preference.Preference
import kodama.core.preference.PreferenceStore
import platform.Foundation.NSUserDefaults

class DarwinPreferenceStore(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults(),
) : PreferenceStore {

    override fun getString(key: String, defaultValue: String): Preference<String> {
        return StringPrimitive(userDefaults, key, defaultValue)
    }

    override fun getLong(key: String, defaultValue: Long): Preference<Long> {
        return LongPrimitive(userDefaults, key, defaultValue)
    }

    override fun getInt(key: String, defaultValue: Int): Preference<Int> {
        return IntPrimitive(userDefaults, key, defaultValue)
    }

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> {
        return FloatPrimitive(userDefaults, key, defaultValue)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> {
        return BooleanPrimitive(userDefaults, key, defaultValue)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> {
        return StringSetPrimitive(userDefaults, key, defaultValue)
    }

    override fun <T> getObject(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return Object(
            userDefaults = userDefaults,
            key = key,
            defaultValue = defaultValue,
            serializer = serializer,
            deserializer = deserializer,
        )
    }

    override fun getAll(): Map<String, *> {
        return userDefaults.dictionaryRepresentation() as? Map<String, *> ?: emptyMap<String, Any>()
    }
}
