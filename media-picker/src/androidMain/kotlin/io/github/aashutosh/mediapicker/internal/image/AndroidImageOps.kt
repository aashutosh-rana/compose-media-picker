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

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import io.github.aashutosh.mediapicker.CompressionConfig
import io.github.aashutosh.mediapicker.InternalMediaPickerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(InternalMediaPickerApi::class)
internal class AndroidCompressor : Compressor {
    override suspend fun compress(input: ByteArray, config: CompressionConfig): ByteArray = withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, config.maxDimension)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size, decodeOpts)
            ?: return@withContext input
        val scaled = scaleToMax(bitmap, config.maxDimension)
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, config.qualityPercent, out)
            if (scaled !== bitmap) scaled.recycle()
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private fun sampleSize(width: Int, height: Int, max: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= max || h / 2 >= max) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }

    private fun scaleToMax(bitmap: Bitmap, max: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val longest = maxOf(w, h)
        if (longest <= max) return bitmap
        val ratio = max.toFloat() / longest
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}

@OptIn(InternalMediaPickerApi::class)
internal class AndroidExifRotator : ExifRotator {
    override suspend fun applyOrientation(input: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        val orientation = ByteArrayInputStream(input).use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        if (orientation == ExifInterface.ORIENTATION_NORMAL) return@withContext input

        val matrix = matrixFor(orientation) ?: return@withContext input
        val bitmap = BitmapFactory.decodeByteArray(input, 0, input.size) ?: return@withContext input
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        ByteArrayOutputStream().use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_REENCODE_QUALITY, out)
            rotated.recycle()
            out.toByteArray()
        }
    }

    @Suppress("MagicNumber")
    private fun matrixFor(orientation: Int): Matrix? {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f)
                m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(270f)
                m.postScale(-1f, 1f)
            }
            else -> return null
        }
        return m
    }

    private companion object {
        const val JPEG_REENCODE_QUALITY = 92
    }
}

@OptIn(InternalMediaPickerApi::class)
internal class AndroidThumbnailFactory : ThumbnailFactory {
    override suspend fun create(input: ByteArray, maxDimension: Int): ByteArray? = withContext(Dispatchers.Default) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input, 0, input.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        val sample = sample(bounds.outWidth, bounds.outHeight, maxDimension)
        val decoded = BitmapFactory.decodeByteArray(
            input,
            0,
            input.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: return@withContext null
        val scaled = scale(decoded, maxDimension)
        ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, out)
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }

    private fun sample(w: Int, h: Int, max: Int): Int {
        var s = 1
        var cw = w
        var ch = h
        while (cw / 2 >= max && ch / 2 >= max) {
            s *= 2
            cw /= 2
            ch /= 2
        }
        return s
    }

    private fun scale(bitmap: Bitmap, max: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= max) return bitmap
        val ratio = max.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt().coerceAtLeast(1),
            (bitmap.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val THUMB_QUALITY = 80
    }
}

@OptIn(InternalMediaPickerApi::class)
public actual fun platformCompressor(): Compressor = AndroidCompressor()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformExifRotator(): ExifRotator = AndroidExifRotator()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformThumbnailFactory(): ThumbnailFactory = AndroidThumbnailFactory()
