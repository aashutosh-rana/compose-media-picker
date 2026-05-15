/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.internal.image

import io.github.aashutosh.mediapicker.CompressionConfig
import io.github.aashutosh.mediapicker.InternalMediaPickerApi

/**
 * Web image ops are no-ops for now. A full implementation would route through an
 * `OffscreenCanvas` + `createImageBitmap` pipeline; that's planned for a follow-up.
 *
 * Returning the input unchanged is safe — `MediaFile.thumbnail()` returns `null` on
 * platforms that can't produce one, and consumers handle that already.
 */
@OptIn(InternalMediaPickerApi::class)
internal class WebCompressor : Compressor {
    override suspend fun compress(input: ByteArray, config: CompressionConfig): ByteArray = input
}

@OptIn(InternalMediaPickerApi::class)
internal class WebExifRotator : ExifRotator {
    override suspend fun applyOrientation(input: ByteArray): ByteArray = input
}

@OptIn(InternalMediaPickerApi::class)
internal class WebThumbnailFactory : ThumbnailFactory {
    override suspend fun create(input: ByteArray, maxDimension: Int): ByteArray? = null
}

@OptIn(InternalMediaPickerApi::class)
public actual fun platformCompressor(): Compressor = WebCompressor()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformExifRotator(): ExifRotator = WebExifRotator()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformThumbnailFactory(): ThumbnailFactory = WebThumbnailFactory()
