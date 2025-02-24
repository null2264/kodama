package kodama.preferences.internal

import co.touchlab.kermit.Logger
import kodama.preferences.Preference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

internal abstract class AbstractPreference<T>(
    private val key: String,
    private val default: T,
    private val internalGet: () -> T,
    private val internalGetFlow: () -> Flow<T>,
    private val internalGetStateFlow: (CoroutineScope, SharingStarted) -> StateFlow<T>,
    private val internalSet: (T) -> Unit,
    private val internalIsSet: () -> Boolean,
    private val internalDelete: () -> Unit,
) : Preference<T> {
    override fun key(): String {
        return key
    }

    override fun get(): T {
        return try {
            internalGet()
        } catch (e: ClassCastException) {
            Logger.i { "Invalid value for $key; deleting" }
            delete()
            default
        }
    }

    override fun getFlow(): Flow<T> {
        return internalGetFlow()
    }

    override fun getStateFlow(scope: CoroutineScope, started: SharingStarted): StateFlow<T> {
        return internalGetStateFlow(scope, started)
    }

    override fun default(): T {
        return default
    }

    override fun set(value: T) {
        internalSet(value)
    }

    override fun isSet(): Boolean {
        return internalIsSet()
    }

    override fun delete() {
        internalDelete()
    }
}
