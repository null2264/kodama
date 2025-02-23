package io.github.null2264.bonsai.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import bonsai.core.preference.Preference

@Composable
fun <T> Preference<T>.collectAsState(): State<T> {
    val flow = remember(this) { getFlow() }
    return flow.collectAsState(initial = get())
}
