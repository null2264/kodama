package bonsai.core.utils

import bonsai.core.preference.DarwinPreferenceStore
import bonsai.core.preference.PreferenceStore
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier

actual fun Module.setupPreferenceStore(
    qualifier: Qualifier?,
    createdAtStart: Boolean,
) : KoinDefinition<PreferenceStore> = single<PreferenceStore>(qualifier = qualifier, createdAtStart = createdAtStart) {
    DarwinPreferenceStore()
}
