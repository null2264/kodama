package kodama.core.utils

import android.content.Context
import kodama.core.preference.AndroidPreferenceStore
import kodama.core.preference.PreferenceStore
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier

actual fun Module.setupPreferenceStore(
    qualifier: Qualifier?,
    createdAtStart: Boolean,
) : KoinDefinition<PreferenceStore> = single(qualifier = qualifier, createdAtStart = createdAtStart) {
    AndroidPreferenceStore(get<Context>())
}
