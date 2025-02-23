package kodama.ui.di

import kodama.core.utils.setupPreferenceStore
import org.koin.dsl.module

val preferenceModule = module {
    setupPreferenceStore()
}
