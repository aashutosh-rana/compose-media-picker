/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(io.github.aashutosh.mediapicker.InternalMediaPickerApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package io.github.aashutosh.mediapicker.internal

import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.Progress
import io.github.aashutosh.mediapicker.internal.image.platformThumbnailFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import platform.AVFoundation.AVAsset
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.duration
import platform.AVFoundation.naturalSize
import platform.AVFoundation.tracksWithMediaType
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal class IosMediaFile(
    private val url: NSURL,
    override val mimeType: String?,
    override val name: String,
) : MediaFile {

    private val progress = ProgressChannel()

    override val sizeBytes: Long by lazy {
        val mgr = NSFileManager.defaultManager
        val attrs = mgr.attributesOfItemAtPath(url.path ?: return@lazy -1L, null) ?: return@lazy -1L
        (attrs["NSFileSize"] as? Long) ?: -1L
    }

    override val width: Int? by lazy {
        if (mimeType?.startsWith("video/") == true) {
            videoDimensions().first
        } else {
            null
        }
    }
    override val height: Int? by lazy {
        if (mimeType?.startsWith("video/") == true) {
            videoDimensions().second
        } else {
            null
        }
    }
    override val durationMs: Long? by lazy {
        if (mimeType?.startsWith("video/") != true) {
            null
        } else {
            val asset: AVAsset = AVURLAsset(uRL = url, options = null)
            val seconds = CMTimeGetSeconds(asset.duration)
            if (seconds.isNaN() || seconds < 0) null else (seconds * MS_PER_SEC).toLong()
        }
    }

    override suspend fun source(): Source = withContext(Dispatchers.Default) {
        val data = NSData.dataWithContentsOfURL(url)
            ?: error("Failed to read NSData at $url")
        val bytes = data.toByteArray()
        progress.emit(Progress(bytesRead = bytes.size.toLong(), totalBytes = bytes.size.toLong()))
        val buffer = Buffer()
        buffer.write(bytes)
        buffer
    }

    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.Default) {
        val data = NSData.dataWithContentsOfURL(url)
            ?: error("Failed to read NSData at $url")
        data.toByteArray().also {
            progress.emit(Progress(bytesRead = it.size.toLong(), totalBytes = it.size.toLong()))
        }
    }

    override suspend fun thumbnail(maxDimension: Int): ByteArray? = platformThumbnailFactory().create(loadBytes(), maxDimension)

    override fun readProgress(): Flow<Progress> = progress.asFlow()

    private fun videoDimensions(): Pair<Int?, Int?> {
        val asset = AVURLAsset(uRL = url, options = null)
        val tracks = asset.tracksWithMediaType(AVMediaTypeVideo)
        val track = (tracks as? List<*>)?.firstOrNull() as? platform.AVFoundation.AVAssetTrack
            ?: return null to null
        return track.naturalSize.useContents { width.toInt() to height.toInt() }
    }

    private companion object {
        const val MS_PER_SEC = 1000.0
    }
}

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val out = ByteArray(length)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return out
}
