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
import io.github.aashutosh.mediapicker.MediaFile

/**
 * Per-platform helper that takes a [MediaFile] picked by the library and hands it to the
 * OS's native viewer (or, on web, an inline overlay). Keeps the sample's "Open" button
 * platform-agnostic.
 */
interface MediaOpener {
    suspend fun open(file: MediaFile)
}

@Composable
expect fun rememberMediaOpener(): MediaOpener
