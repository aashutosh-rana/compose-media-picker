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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aashutosh.mediapicker.MediaFile

/**
 * iOS opener is intentionally a no-op for now. A future revision will present a
 * `QLPreviewController` or `UIActivityViewController` so the Files / Photos / share-sheet
 * flow opens directly from the sample.
 */
@Composable
actual fun rememberMediaOpener(): MediaOpener = remember { IosMediaOpener }

private object IosMediaOpener : MediaOpener {
    override suspend fun open(file: MediaFile) = Unit
}
