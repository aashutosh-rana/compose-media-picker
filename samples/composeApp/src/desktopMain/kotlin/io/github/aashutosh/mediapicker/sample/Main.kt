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

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.aashutosh.mediapicker.DesktopPlatformContext
import io.github.aashutosh.mediapicker.initializeMediaPicker

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Media Picker Sample") {
        initializeMediaPicker(DesktopPlatformContext(parent = window))
        App()
    }
}
