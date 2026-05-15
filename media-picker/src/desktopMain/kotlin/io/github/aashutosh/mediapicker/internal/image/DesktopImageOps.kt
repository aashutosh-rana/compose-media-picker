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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@OptIn(InternalMediaPickerApi::class)
internal class DesktopCompressor : Compressor {
    override suspend fun compress(input: ByteArray, config: CompressionConfig): ByteArray = withContext(Dispatchers.Default) {
        val src = ImageIO.read(ByteArrayInputStream(input)) ?: return@withContext input
        val resized = resize(src, config.maxDimension)
        encodeJpeg(resized, config.qualityPercent)
    }
}

@OptIn(InternalMediaPickerApi::class)
internal class DesktopExifRotator : ExifRotator {
    // ImageIO on stock JVM does not honor EXIF orientation. Decoding through Java2D and
    // re-encoding via JPEG embeds whatever orientation the source declared. For
    // pixel-correct rotation we'd need metadata-extractor or twelvemonkeys-imageio. For
    // now this is a no-op — desktop pickers typically receive already-rotated files.
    override suspend fun applyOrientation(input: ByteArray): ByteArray = input
}

@OptIn(InternalMediaPickerApi::class)
internal class DesktopThumbnailFactory : ThumbnailFactory {
    override suspend fun create(input: ByteArray, maxDimension: Int): ByteArray? = withContext(Dispatchers.Default) {
        val src = ImageIO.read(ByteArrayInputStream(input)) ?: return@withContext null
        val resized = resize(src, maxDimension)
        encodeJpeg(resized, THUMB_QUALITY)
    }

    private companion object {
        const val THUMB_QUALITY = 80
    }
}

private fun resize(src: BufferedImage, maxDimension: Int): BufferedImage {
    val w = src.width
    val h = src.height
    val longest = maxOf(w, h)
    if (longest <= maxDimension) return src
    val ratio = maxDimension.toDouble() / longest
    val newW = (w * ratio).toInt().coerceAtLeast(1)
    val newH = (h * ratio).toInt().coerceAtLeast(1)
    val out = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
    val g = out.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(src.getScaledInstance(newW, newH, Image.SCALE_SMOOTH), 0, 0, null)
    g.dispose()
    return out
}

private fun encodeJpeg(image: BufferedImage, qualityPercent: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    ImageIO.createImageOutputStream(out).use { ios ->
        writer.output = ios
        val param = writer.defaultWriteParam.apply {
            compressionMode = ImageWriteParam.MODE_EXPLICIT
            compressionQuality = qualityPercent / 100f
        }
        writer.write(null, IIOImage(image, null, null), param)
        writer.dispose()
    }
    return out.toByteArray()
}

@OptIn(InternalMediaPickerApi::class)
public actual fun platformCompressor(): Compressor = DesktopCompressor()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformExifRotator(): ExifRotator = DesktopExifRotator()

@OptIn(InternalMediaPickerApi::class)
public actual fun platformThumbnailFactory(): ThumbnailFactory = DesktopThumbnailFactory()
