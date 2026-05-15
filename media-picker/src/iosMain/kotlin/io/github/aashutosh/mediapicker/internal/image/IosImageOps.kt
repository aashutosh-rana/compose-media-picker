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
import io.github.aashutosh.mediapicker.internal.toByteArray
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation

@OptIn(ExperimentalForeignApi::class, InternalMediaPickerApi::class)
internal class IosCompressor : Compressor {
    override suspend fun compress(input: ByteArray, config: CompressionConfig): ByteArray = withContext(Dispatchers.Default) {
        val image = input.toUiImage() ?: return@withContext input
        val resized = image.resized(longestEdge = config.maxDimension)
        val jpegQuality = config.qualityPercent.toDouble() / 100.0
        val data = UIImageJPEGRepresentation(resized, jpegQuality)
            ?: return@withContext input
        data.toByteArray()
    }
}

@OptIn(ExperimentalForeignApi::class, InternalMediaPickerApi::class)
internal class IosExifRotator : ExifRotator {
    override suspend fun applyOrientation(input: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        // UIImage(data:) honors EXIF on render; re-encoding via UIImageJPEGRepresentation
        // bakes the orientation into the pixels.
        val image = input.toUiImage() ?: return@withContext input
        UIImageJPEGRepresentation(image, JPEG_REENCODE_QUALITY)?.toByteArray() ?: input
    }

    private companion object {
        const val JPEG_REENCODE_QUALITY = 0.92
    }
}

@OptIn(ExperimentalForeignApi::class, InternalMediaPickerApi::class)
internal class IosThumbnailFactory : ThumbnailFactory {
    override suspend fun create(input: ByteArray, maxDimension: Int): ByteArray? = withContext(Dispatchers.Default) {
        val image = input.toUiImage() ?: return@withContext null
        val resized = image.resized(longestEdge = maxDimension)
        UIImageJPEGRepresentation(resized, THUMB_QUALITY)?.toByteArray()
    }

    private companion object {
        const val THUMB_QUALITY = 0.8
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toUiImage(): UIImage? {
    if (isEmpty()) return null
    val nsData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
    return UIImage.imageWithData(nsData)
}

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.resized(longestEdge: Int): UIImage {
    val (w, h) = this.size.useContents { width to height }
    val longest = maxOf(w, h)
    if (longest <= longestEdge) return this
    val ratio = longestEdge.toDouble() / longest
    val targetWidth = w * ratio
    val targetHeight = h * ratio
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, scale = 1.0)
    this.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val out = UIGraphicsGetImageFromCurrentImageContext() ?: this
    UIGraphicsEndImageContext()
    return out
}

@OptIn(InternalMediaPickerApi::class)
public actual fun platformCompressor(): Compressor = IosCompressor()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformExifRotator(): ExifRotator = IosExifRotator()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformThumbnailFactory(): ThumbnailFactory = IosThumbnailFactory()
