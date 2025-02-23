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

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.core.content.edit
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

sealed class AndroidPreference<T>(
    private val preferences: SharedPreferences,
    private val keyFlow: Flow<String?>,
    private val key: String,
    private val defaultValue: T,
) : Preference<T> {

    abstract fun read(preferences: SharedPreferences, key: String, defaultValue: T): T

    abstract fun write(key: String, value: T): Editor.() -> Unit

    override fun key(): String {
        return key
    }

    override fun get(): T {
        return try {
            read(preferences, key, defaultValue)
        } catch (e: ClassCastException) {
            Logger.i { "Invalid value for $key; deleting" }
            delete()
            defaultValue
        }
    }

    override fun set(value: T) {
        preferences.edit(action = write(key, value))
    }

    override fun isSet(): Boolean {
        return preferences.contains(key)
    }

    override fun delete() {
        preferences.edit {
            remove(key)
        }
    }

    override fun default(): T {
        return defaultValue
    }

    override fun getFlow(): Flow<T> {
        return keyFlow
            .filter { it == key || it == null }
            .onStart { emit("ignition") }
            .map { get() }
            .conflate()
    }

    override fun getStateFlow(scope: CoroutineScope, started: SharingStarted): StateFlow<T> {
        return getFlow().stateIn(scope, started, get())
    }

    class StringPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: String,
    ) : AndroidPreference<String>(preferences, keyFlow, key, defaultValue) {
        override fun read(
            preferences: SharedPreferences,
            key: String,
            defaultValue: String,
        ): String {
            return preferences.getString(key, defaultValue) ?: defaultValue
        }

        override fun write(key: String, value: String): Editor.() -> Unit = {
            putString(key, value)
        }
    }

    class LongPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Long,
    ) : AndroidPreference<Long>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Long): Long {
            return preferences.getLong(key, defaultValue)
        }

        override fun write(key: String, value: Long): Editor.() -> Unit = {
            putLong(key, value)
        }
    }

    class IntPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Int,
    ) : AndroidPreference<Int>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Int): Int {
            return preferences.getInt(key, defaultValue)
        }

        override fun write(key: String, value: Int): Editor.() -> Unit = {
            putInt(key, value)
        }
    }

    class FloatPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Float,
    ) : AndroidPreference<Float>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: Float): Float {
            return preferences.getFloat(key, defaultValue)
        }

        override fun write(key: String, value: Float): Editor.() -> Unit = {
            putFloat(key, value)
        }
    }

    class BooleanPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Boolean,
    ) : AndroidPreference<Boolean>(preferences, keyFlow, key, defaultValue) {
        override fun read(
            preferences: SharedPreferences,
            key: String,
            defaultValue: Boolean,
        ): Boolean {
            return preferences.getBoolean(key, defaultValue)
        }

        override fun write(key: String, value: Boolean): Editor.() -> Unit = {
            putBoolean(key, value)
        }
    }

    class StringSetPrimitive(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: Set<String>,
    ) : AndroidPreference<Set<String>>(preferences, keyFlow, key, defaultValue) {
        override fun read(
            preferences: SharedPreferences,
            key: String,
            defaultValue: Set<String>,
        ): Set<String> {
            return preferences.getStringSet(key, defaultValue) ?: defaultValue
        }

        override fun write(key: String, value: Set<String>): Editor.() -> Unit = {
            putStringSet(key, value)
        }
    }

    class Object<T>(
        preferences: SharedPreferences,
        keyFlow: Flow<String?>,
        key: String,
        defaultValue: T,
        val serializer: (T) -> String,
        val deserializer: (String) -> T,
    ) : AndroidPreference<T>(preferences, keyFlow, key, defaultValue) {
        override fun read(preferences: SharedPreferences, key: String, defaultValue: T): T {
            return try {
                preferences.getString(key, null)?.let(deserializer) ?: defaultValue
            } catch (e: Exception) {
                defaultValue
            }
        }

        override fun write(key: String, value: T): Editor.() -> Unit = {
            putString(key, serializer(value))
        }
    }
}
