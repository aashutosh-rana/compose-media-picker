/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(InternalMediaPickerApi::class)

package io.github.aashutosh.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.aashutosh.mediapicker.internal.DefaultMediaPicker
import io.github.aashutosh.mediapicker.internal.MediaPickerEngine
import io.github.aashutosh.mediapicker.internal.PlatformContextHolder

/**
 * Returns a [MediaPicker] scoped to the calling composable. The picker is detached and
 * all pending operations are cancelled when this composable leaves composition — no
 * leaks, no zombie callbacks.
 *
 * Call [initializeMediaPicker] once at app startup before any composable invokes this.
 */
@Composable
public fun rememberMediaPicker(): MediaPicker {
    val context = PlatformContextHolder.require()
    val engine = remember(context) { MediaPickerEngine(context) }
    val picker = remember(engine) { DefaultMediaPicker(engine) }

    DisposableEffect(engine) {
        engine.attach()
        onDispose { engine.detach() }
    }
    return picker
}
