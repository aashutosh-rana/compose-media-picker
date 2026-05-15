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

package io.github.aashutosh.mediapicker.internal

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.Progress
import io.github.aashutosh.mediapicker.internal.image.platformThumbnailFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.ByteArrayOutputStream

internal class AndroidMediaFile(
    private val resolver: ContentResolver,
    private val uri: Uri,
    override val mimeType: String?,
    private val knownName: String? = null,
    private val knownSize: Long? = null,
) : MediaFile {

    private val progress = ProgressChannel()

    override val name: String by lazy { knownName ?: queryName(resolver, uri) ?: uri.lastPathSegment.orEmpty() }
    override val sizeBytes: Long by lazy { knownSize ?: querySize(resolver, uri) ?: -1L }

    override val width: Int? by lazy { queryDimensions().first }
    override val height: Int? by lazy { queryDimensions().second }
    override val durationMs: Long? by lazy { queryDurationMs() }

    override suspend fun source(): Source = withContext(Dispatchers.IO) {
        val stream = checkNotNull(resolver.openInputStream(uri)) {
            "ContentResolver returned null InputStream for $uri"
        }
        stream.asSource().buffered()
    }

    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.IO) {
        val total = sizeBytes
        ByteArrayOutputStream().use { out ->
            resolver.openInputStream(uri)?.use { input ->
                val buf = ByteArray(BUFFER_BYTES)
                var read: Int
                var soFar = 0L
                while (input.read(buf).also { read = it } != -1) {
                    out.write(buf, 0, read)
                    soFar += read
                    progress.emit(Progress(bytesRead = soFar, totalBytes = total))
                }
            }
            out.toByteArray()
        }
    }

    override suspend fun thumbnail(maxDimension: Int): ByteArray? = withContext(Dispatchers.Default) {
        when {
            mimeType?.startsWith("video/") == true -> extractVideoFrame(maxDimension)
            mimeType?.startsWith("image/") == true -> platformThumbnailFactory().create(loadBytes(), maxDimension)
            else -> null
        }
    }

    private fun extractVideoFrame(maxDimension: Int): ByteArray? = MediaMetadataRetriever().runCatching {
        setDataSource(resolver.openAssetFileDescriptor(uri, "r")?.fileDescriptor)
        // First frame is enough for a thumbnail.
        val frame = getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return@runCatching null
        release()
        val longest = maxOf(frame.width, frame.height)
        val scaled = if (longest <= maxDimension) {
            frame
        } else {
            val ratio = maxDimension.toFloat() / longest
            android.graphics.Bitmap.createScaledBitmap(
                frame,
                (frame.width * ratio).toInt().coerceAtLeast(1),
                (frame.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        }
        ByteArrayOutputStream().use { out ->
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, VIDEO_THUMB_QUALITY, out)
            if (scaled !== frame) scaled.recycle()
            frame.recycle()
            out.toByteArray()
        }
    }.getOrNull()

    override fun readProgress(): Flow<Progress> = progress.asFlow()

    private fun queryDimensions(): Pair<Int?, Int?> = when {
        mimeType?.startsWith("image/") == true -> readImageDimensions()
        mimeType?.startsWith("video/") == true -> readVideoDimensions()
        else -> null to null
    }

    private fun readImageDimensions(): Pair<Int?, Int?> = try {
        resolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            val rotated = needsAxisSwap()
            val w = opts.outWidth.takeIf { it > 0 }
            val h = opts.outHeight.takeIf { it > 0 }
            if (rotated) h to w else w to h
        } ?: (null to null)
    } catch (_: Throwable) {
        null to null
    }

    private fun needsAxisSwap(): Boolean = try {
        resolver.openInputStream(uri)?.use { stream ->
            val orientation = ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE
        } ?: false
    } catch (_: Throwable) {
        false
    }

    private fun readVideoDimensions(): Pair<Int?, Int?> = MediaMetadataRetriever().runCatching {
        setDataSource(resolver.openAssetFileDescriptor(uri, "r")?.fileDescriptor)
        val w = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val h = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        release()
        w to h
    }.getOrDefault(null to null)

    private fun queryDurationMs(): Long? {
        if (mimeType?.startsWith("video/") != true) return null
        return MediaMetadataRetriever().runCatching {
            setDataSource(resolver.openAssetFileDescriptor(uri, "r")?.fileDescriptor)
            val d = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            release()
            d
        }.getOrNull()
    }

    private companion object {
        const val BUFFER_BYTES = 32 * 1024
        const val VIDEO_THUMB_QUALITY = 80
    }
}

internal fun queryName(resolver: ContentResolver, uri: Uri): String? = try {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
} catch (_: Throwable) {
    null
}

internal fun querySize(resolver: ContentResolver, uri: Uri): Long? = try {
    resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getLong(0) else null
    }
} catch (_: Throwable) {
    null
}

internal fun queryMimeType(resolver: ContentResolver, uri: Uri): String? = resolver.getType(uri) ?: MediaStore.Images.Media.CONTENT_TYPE
