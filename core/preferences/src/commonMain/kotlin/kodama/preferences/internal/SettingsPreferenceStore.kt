package kodama.preferences.internal

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import kodama.preferences.Preference
import kodama.preferences.PreferenceStore

class SettingsPreferenceStore(
    private val settings: ObservableSettings,
) : PreferenceStore {

    class Factory(private val delegate: Settings.Factory) : PreferenceStore.Factory {
        override fun default(): PreferenceStore = get(PreferenceStore.Factory.DEFAULT_PREFERENCE_NAME)

        override fun get(name: String): PreferenceStore = create(name)

        override fun create(name: String?): PreferenceStore {
            val settings = delegate.create(name ?: PreferenceStore.Factory.DEFAULT_PREFERENCE_NAME)
            return SettingsPreferenceStore(settings as ObservableSettings)
        }
    }

    override fun getString(key: String, default: String): Preference<String> {
        return StringPreferences(settings, key, default)
    }

    override fun getLong(key: String, default: Long): Preference<Long> {
        return LongPreferences(settings, key, default)
    }

    override fun getInt(key: String, default: Int): Preference<Int> {
        return IntPreferences(settings, key, default)
    }

    override fun getFloat(key: String, default: Float): Preference<Float> {
        return FloatPreferences(settings, key, default)
    }

    override fun getBoolean(key: String, default: Boolean): Preference<Boolean> {
        return BooleanPreferences(settings, key, default)
    }

    override fun <T> getObject(
        key: String,
        default: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> {
        return ObjectPreferences(settings, key, default, serializer, deserializer)
    }
}
