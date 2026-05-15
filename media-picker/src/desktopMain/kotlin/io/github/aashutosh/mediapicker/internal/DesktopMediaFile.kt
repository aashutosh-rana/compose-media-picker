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

import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.Progress
import io.github.aashutosh.mediapicker.internal.image.platformThumbnailFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.net.URLConnection
import javax.imageio.ImageIO

internal class DesktopMediaFile(
    private val file: File,
    override val mimeType: String? = URLConnection.guessContentTypeFromName(file.name),
) : MediaFile {

    private val progress = ProgressChannel()

    override val name: String get() = file.name
    override val sizeBytes: Long get() = file.length()

    override val width: Int? by lazy { readImageDimensions().first }
    override val height: Int? by lazy { readImageDimensions().second }
    override val durationMs: Long? = null // Desktop has no built-in video metadata reader

    override suspend fun source(): Source = withContext(Dispatchers.IO) {
        file.inputStream().asSource().buffered()
    }

    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.IO) {
        val total = file.length()
        file.inputStream().use { input ->
            val out = ByteArray(total.toInt())
            var read = 0
            val buf = ByteArray(BUFFER)
            while (read < out.size) {
                val n = input.read(buf)
                if (n <= 0) break
                System.arraycopy(buf, 0, out, read, n)
                read += n
                progress.emit(Progress(bytesRead = read.toLong(), totalBytes = total))
            }
            out
        }
    }

    override suspend fun thumbnail(maxDimension: Int): ByteArray? = withContext(Dispatchers.Default) {
        if (mimeType?.startsWith("image/") != true) return@withContext null
        platformThumbnailFactory().create(loadBytes(), maxDimension)
    }

    override fun readProgress(): Flow<Progress> = progress.asFlow()

    private fun readImageDimensions(): Pair<Int?, Int?> {
        if (mimeType?.startsWith("image/") != true) return null to null
        return runCatching {
            ImageIO.createImageInputStream(file).use { stream ->
                val readers = ImageIO.getImageReaders(stream)
                if (!readers.hasNext()) return null to null
                val reader = readers.next()
                reader.input = stream
                val w = reader.getWidth(0)
                val h = reader.getHeight(0)
                reader.dispose()
                w to h
            }
        }.getOrDefault(null to null)
    }

    private companion object {
        const val BUFFER = 32 * 1024
    }
}
