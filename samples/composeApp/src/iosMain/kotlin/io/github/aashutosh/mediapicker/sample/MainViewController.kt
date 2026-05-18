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
import platform.UIKit.UIViewController

/**
 * Entry point consumed by the Xcode `iosApp` target. Returns a `UIViewController` that
 * hosts the shared Compose UI. The library auto-discovers this controller as the
 * presentation host on the first picker call — no Swift-side or Kotlin-side
 * initialization required.
 */
@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
