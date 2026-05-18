/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(io.github.aashutosh.mediapicker.InternalMediaPickerApi::class)

package io.github.aashutosh.mediapicker.internal.image

import io.github.aashutosh.mediapicker.CompressionConfig
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.Buffer
import kotlinx.io.Source

/**
 * Applies optional EXIF rotation and/or JPEG re-encode/resize to a [MediaFile] returned
 * by a picker engine. The Compressor / ExifRotator instances are platform actuals and
 * already exist — this is the wiring that consumers' [io.github.aashutosh.mediapicker.ImagePickerConfig]
 * + [io.github.aashutosh.mediapicker.MultiImagePickerConfig] settings were expected to use.
 *
 * Returns the original [MediaFile] unchanged when:
 * - The MIME type isn't an image (or is null);
 * - Both `compression` and `applyExifRotation` are off;
 * - The platform's compressor + rotator are no-ops (e.g. wasmJs, which has no Skia/JPEG path yet).
 *
 * When transforming, returns an [InMemoryJpegMediaFile] holding the re-encoded JPEG bytes
 * and a `.jpg` filename. EXIF orientation is baked into the pixels so downstream consumers
 * never have to honor the EXIF tag again.
 */
internal suspend fun MediaFile.postProcessImage(compression: CompressionConfig?, applyExifRotation: Boolean): MediaFile {
    if (mimeType?.startsWith("image/") != true) return this
    if (compression == null && !applyExifRotation) return this

    // Defensive: a single corrupt or oversized image (OOM in BitmapFactory, decoder bug,
    // missing EXIF, etc.) must not break the whole pick. Fall back to the original
    // MediaFile so consumers get something usable rather than the entire selection failing.
    return runCatching {
        val original = loadBytes()
        val rotated = if (applyExifRotation) platformExifRotator().applyOrientation(original) else original
        val compressed = if (compression != null) platformCompressor().compress(rotated, compression) else rotated

        // No-op platforms (web today) return the input array unchanged. Skip the wrapper so
        // consumers still see the original filename / MIME / size.
        if (compressed === original) {
            this
        } else {
            InMemoryJpegMediaFile(
                name = renameToJpg(name),
                bytes = compressed,
                width = width,
                height = height,
            )
        }
    }.getOrDefault(this)
}

private fun renameToJpg(name: String): String = if (name.contains('.')) "${name.substringBeforeLast('.')}.jpg" else "$name.jpg"

/**
 * Backing [MediaFile] for post-processor output. Holds the re-encoded JPEG bytes in
 * memory — `loadBytes` and `source` are zero-I/O. `thumbnail` runs on the already-encoded
 * bytes through the platform thumbnail factory.
 */
internal class InMemoryJpegMediaFile(
    override val name: String,
    private val bytes: ByteArray,
    override val width: Int?,
    override val height: Int?,
) : MediaFile {
    override val mimeType: String = "image/jpeg"
    override val sizeBytes: Long = bytes.size.toLong()
    override val durationMs: Long? = null

    override suspend fun source(): Source = Buffer().also { it.write(bytes) }
    override suspend fun loadBytes(): ByteArray = bytes
    override suspend fun thumbnail(maxDimension: Int): ByteArray? = platformThumbnailFactory().create(bytes, maxDimension)
    override fun readProgress(): Flow<Progress> = emptyFlow()
}
