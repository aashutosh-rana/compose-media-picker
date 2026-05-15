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

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes JPEG / PNG / WebP bytes (the format the library's `MediaFile.thumbnail()` returns)
 * into a Compose [ImageBitmap]. Returns `null` if the platform couldn't decode them.
 *
 * - Android: `BitmapFactory.decodeByteArray` → `.asImageBitmap()`
 * - iOS / Desktop / Web: `org.jetbrains.skia.Image.makeFromEncoded(bytes)` → `.toComposeImageBitmap()`
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
