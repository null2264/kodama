package kodama.preferences.internal

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import com.russhwolf.settings.coroutines.getFloatFlow
import com.russhwolf.settings.coroutines.getFloatStateFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getIntStateFlow
import com.russhwolf.settings.coroutines.getLongFlow
import com.russhwolf.settings.coroutines.getLongStateFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.coroutines.getStringOrNullFlow
import com.russhwolf.settings.coroutines.getStringOrNullStateFlow
import com.russhwolf.settings.coroutines.getStringStateFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalSettingsApi::class)
internal class StringPreferences(
    private val settings: ObservableSettings,
    private val key: String,
    private val default: String,
) : AbstractPreference<String>(
    key = key,
    default = default,
    internalGet = { settings.getString(key, default) },
    internalGetFlow = { settings.getStringFlow(key, default) },
    internalGetStateFlow = { scope, started -> settings.getStringStateFlow(scope, key, default, started) },
    internalSet = { value -> settings[key] = value },
    internalIsSet = { settings.hasKey(key) },
    internalDelete = { settings.remove(key) },
)

@OptIn(ExperimentalSettingsApi::class)
internal class LongPreferences(
    private val settings: ObservableSettings,
    private val key: String,
    private val default: Long,
) : AbstractPreference<Long>(
    key = key,
    default = default,
    internalGet = { settings.getLong(key, default) },
    internalGetFlow = { settings.getLongFlow(key, default) },
    internalGetStateFlow = { scope, started -> settings.getLongStateFlow(scope, key, default, started) },
    internalSet = { value -> settings[key] = value },
    internalIsSet = { settings.hasKey(key) },
    internalDelete = { settings.remove(key) },
)

@OptIn(ExperimentalSettingsApi::class)
internal class IntPreferences(
    private val settings: ObservableSettings,
    private val key: String,
    private val default: Int,
) : AbstractPreference<Int>(
    key = key,
    default = default,
    internalGet = { settings.getInt(key, default) },
    internalGetFlow = { settings.getIntFlow(key, default) },
    internalGetStateFlow = { scope, started -> settings.getIntStateFlow(scope, key, default, started) },
    internalSet = { value -> settings[key] = value },
    internalIsSet = { settings.hasKey(key) },
    internalDelete = { settings.remove(key) },
)

@OptIn(ExperimentalSettingsApi::class)
internal class FloatPreferences(
    private val settings: ObservableSettings,
    private val key: String,
    private val default: Float,
) : AbstractPreference<Float>(
    key = key,
    default = default,
    internalGet = { settings.getFloat(key, default) },
    internalGetFlow = { settings.getFloatFlow(key, default) },
    internalGetStateFlow = { scope, started -> settings.getFloatStateFlow(scope, key, default, started) },
    internalSet = { value -> settings[key] = value },
    internalIsSet = { settings.hasKey(key) },
    internalDelete = { settings.remove(key) },
)

@OptIn(ExperimentalSettingsApi::class)
internal class BooleanPreferences(
    private val settings: ObservableSettings,
    private val key: String,
    private val default: Boolean,
) : AbstractPreference<Boolean>(
    key = key,
    default = default,
    internalGet = { settings.getBoolean(key, default) },
    internalGetFlow = { settings.getBooleanFlow(key, default) },
    internalGetStateFlow = { scope, started -> settings.getBooleanStateFlow(scope, key, default, started) },
    internalSet = { value -> settings[key] = value },
    internalIsSet = { settings.hasKey(key) },
    internalDelete = { settings.remove(key) },
)

@OptIn(ExperimentalSettingsApi::class)
internal class ObjectPreferences<T>(
    private val settings: ObservableSettings,
    private val key: String,
    private val default: T,
    private val serializer: (T) -> String,
    private val deserializer: (String) -> T,
) : AbstractPreference<T>(
    key = key,
    default = default,
    internalGet = {
        try {
            settings.getStringOrNull(key)?.let(deserializer) ?: default
        } catch (e: Exception) {
            default
        }
    },
    internalGetFlow = {
        settings.getStringOrNullFlow(key).map {
            try {
                it?.let(deserializer) ?: default
            } catch (e: Exception) {
                default
            }
        }
    },
    internalGetStateFlow = { scope, started ->
        settings.getStringOrNullStateFlow(scope, key, started).map {
            try {
                it?.let(deserializer) ?: default
            } catch (e: Exception) {
                default
            }
        } as StateFlow<T>
    },
    internalSet = { value -> settings[key] = serializer(value) },
    internalIsSet = { settings.hasKey(key) },
    internalDelete = { settings.remove(key) },
)
