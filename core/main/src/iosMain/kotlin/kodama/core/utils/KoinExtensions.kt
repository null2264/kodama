package kodama.core.utils

import kodama.core.preference.DarwinPreferenceStore
import kodama.core.preference.PreferenceStore
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier

actual fun Module.setupPreferenceStore(
    qualifier: Qualifier?,
    createdAtStart: Boolean,
) : KoinDefinition<PreferenceStore> = single<PreferenceStore>(qualifier = qualifier, createdAtStart = createdAtStart) {
    DarwinPreferenceStore()
}
