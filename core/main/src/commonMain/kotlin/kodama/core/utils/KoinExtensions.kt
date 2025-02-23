package kodama.core.utils

import kodama.core.preference.PreferenceStore
import org.koin.core.definition.KoinDefinition
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier

expect fun Module.setupPreferenceStore(
    qualifier: Qualifier? = null,
    createdAtStart: Boolean = false,
) : KoinDefinition<PreferenceStore>
