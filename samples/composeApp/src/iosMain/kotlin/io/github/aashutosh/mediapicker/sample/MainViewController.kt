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

import androidx.compose.ui.window.ComposeUIViewController
import io.github.aashutosh.mediapicker.IosPlatformContext
import io.github.aashutosh.mediapicker.initializeMediaPicker
import platform.UIKit.UIViewController

/**
 * Entry point consumed by the Xcode `iosApp` target. Returns a `UIViewController` that
 * hosts the shared Compose UI. The Swift side wraps this in a `UIViewControllerRepresentable`.
 *
 * The view controller itself is passed to [initializeMediaPicker] so the media-picker
 * library can present `PHPickerViewController` / `UIDocumentPickerViewController` /
 * `UIImagePickerController` modally on top of it.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController { App() }
    initializeMediaPicker(IosPlatformContext(controller))
    return controller
}
