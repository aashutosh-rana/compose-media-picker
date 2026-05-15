/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import io.github.aashutosh.mediapicker.WebPlatformContext
import io.github.aashutosh.mediapicker.initializeMediaPicker

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initializeMediaPicker(WebPlatformContext())
    CanvasBasedWindow(canvasElementId = "ComposeTarget", title = "Media Picker Sample") {
        App()
    }
}
