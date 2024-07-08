package io.github.null2264.bonsai.core.di

import bonsai.core.utils.setupPreferenceStore
import org.koin.dsl.module

val preferenceModule = module {
    setupPreferenceStore()
}
