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
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.files.File

internal class WebMediaFile(
    private val file: File,
) : MediaFile {

    private val progress = ProgressChannel()

    override val name: String = file.name
    override val mimeType: String? = file.type.ifEmpty { null }
    override val sizeBytes: Long = file.size.toDouble().toLong()
    override val width: Int? = null
    override val height: Int? = null
    override val durationMs: Long? = null

    override suspend fun source(): Source = withContext(Dispatchers.Default) {
        val bytes = loadBytes()
        Buffer().also { it.write(bytes) }
    }

    override suspend fun loadBytes(): ByteArray = withContext(Dispatchers.Default) {
        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        val buffer = file.arrayBuffer().await<org.khronos.webgl.ArrayBuffer>()
        val view = Int8Array(buffer)
        val out = ByteArray(view.length)
        for (i in 0 until view.length) {
            out[i] = view[i]
        }
        progress.emit(Progress(bytesRead = out.size.toLong(), totalBytes = out.size.toLong()))
        out
    }

    override suspend fun thumbnail(maxDimension: Int): ByteArray? = platformThumbnailFactory().create(loadBytes(), maxDimension)

    override fun readProgress(): Flow<Progress> = progress.asFlow()
}

@JsFun("(file) => file.arrayBuffer()")
private external fun fileArrayBuffer(file: File): kotlin.js.Promise<org.khronos.webgl.ArrayBuffer>

private fun File.arrayBuffer(): kotlin.js.Promise<org.khronos.webgl.ArrayBuffer> = fileArrayBuffer(this)
