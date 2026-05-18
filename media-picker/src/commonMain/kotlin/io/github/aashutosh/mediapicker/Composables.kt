/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.github.aashutosh.mediapicker.internal.DefaultMediaPicker
import io.github.aashutosh.mediapicker.internal.MediaPickerEngine
import io.github.aashutosh.mediapicker.internal.discoverPlatformContext

/**
 * Returns a [MediaPicker] scoped to the calling composable. The picker auto-discovers
 * the host (`ComponentActivity` on Android, foreground `UIViewController` on iOS, the
 * Compose `Window` on Desktop, the browser on Web) so consumers don't have to register
 * anything at app startup.
 *
 * All pending operations are cancelled when this composable leaves composition — no
 * leaks, no zombie callbacks.
 *
 * Example:
 * ```
 * class MainActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContent {
 *             val picker = rememberMediaPicker()
 *             val scope = rememberCoroutineScope()
 *             Button(onClick = {
 *                 scope.launch {
 *                     when (val r = picker.captureImage()) {
 *                         is MediaPickerResult.Success -> handle(r.data)
 *                         else -> Unit
 *                     }
 *                 }
 *             }) { Text("Take photo") }
 *         }
 *     }
 * }
 * ```
 *
 * For tests, pass a fake `MediaPicker` directly to the composable instead of calling
 * this function — there's no global to override.
 */
@Composable
public fun rememberMediaPicker(): MediaPicker {
    val context = discoverPlatformContext()
        ?: error(
            "rememberMediaPicker() requires a Compose host backed by a ComponentActivity " +
                "(Android), a foreground UIViewController (iOS), or a Compose Window (Desktop). " +
                "See https://github.com/aashutosh-rana/compose-media-picker#quickstart.",
        )
    val engine = remember(context) { MediaPickerEngine(context) }
    val picker = remember(engine) { DefaultMediaPicker(engine) }

    DisposableEffect(engine) {
        engine.attach()
        onDispose { engine.detach() }
    }
    return picker
}
