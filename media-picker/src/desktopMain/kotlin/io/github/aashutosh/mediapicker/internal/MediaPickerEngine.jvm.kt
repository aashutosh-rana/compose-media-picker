/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.internal

import io.github.aashutosh.mediapicker.CameraCaptureConfig
import io.github.aashutosh.mediapicker.DesktopPlatformContext
import io.github.aashutosh.mediapicker.FilePickerConfig
import io.github.aashutosh.mediapicker.ImagePickerConfig
import io.github.aashutosh.mediapicker.InternalMediaPickerApi
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.PlatformContext
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@OptIn(InternalMediaPickerApi::class)
public actual class MediaPickerEngine public actual constructor(context: PlatformContext) {

    private val desktopCtx = context as? DesktopPlatformContext
        ?: error("Desktop target requires DesktopPlatformContext, got ${context::class.simpleName}")

    public actual fun attach() {}
    public actual fun detach() {}

    public actual suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile> =
        pickWithDialog(IMAGE_EXTENSIONS, multi = false)?.firstOrNull()
            ?.let { MediaPickerResult.Success(toMediaFile(it)) }
            ?: MediaPickerResult.Cancelled

    public actual suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>> {
        val files = pickWithDialog(IMAGE_EXTENSIONS, multi = true) ?: return MediaPickerResult.Cancelled
        val capped = files.take(config.maxItems).map { toMediaFile(it) }
        return if (capped.isEmpty()) {
            MediaPickerResult.Cancelled
        } else {
            MediaPickerResult.Success(capped)
        }
    }

    public actual suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile> {
        val file = pickWithDialog(VIDEO_EXTENSIONS, multi = false)?.firstOrNull()
            ?: return MediaPickerResult.Cancelled
        if (config.maxSizeBytes != null && file.length() > config.maxSizeBytes) {
            return MediaPickerResult.Error(IllegalStateException("Video exceeds maxSizeBytes"))
        }
        return MediaPickerResult.Success(toMediaFile(file))
    }

    public actual suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile> {
        val exts = config.mimeTypes.flatMap { mimeToExtensions(it) }.distinct()
        val files = pickWithDialog(exts, config.allowMultiple) ?: return MediaPickerResult.Cancelled
        return if (files.isEmpty()) {
            MediaPickerResult.Cancelled
        } else {
            MediaPickerResult.Success(toMediaFile(files.first()))
        }
    }

    /**
     * Desktop has no system camera app, so we drive the default webcam ourselves and host
     * a small Swing preview window. Returns [MediaPickerResult.Unsupported] when no
     * webcam is present or the driver fails to open it (e.g. another process is holding
     * the device).
     */
    public actual suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile> {
        val file = runCatching { captureStillFromDesktopWebcam() }.getOrNull()
            ?: return MediaPickerResult.Unsupported
        return MediaPickerResult.Success(toMediaFile(file))
    }

    /**
     * Records H.264 MP4 at 15fps from the default webcam via a Swing preview window with
     * Record / Stop / Cancel controls. Returns [MediaPickerResult.Unsupported] when no
     * camera is available or the JCodec encoder fails to initialize.
     */
    public actual suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile> {
        val file = runCatching { captureVideoFromDesktopWebcam() }.getOrNull()
            ?: return MediaPickerResult.Unsupported
        return MediaPickerResult.Success(toMediaFile(file))
    }

    // -- helpers ----------------------------------------------------------------------

    private suspend fun pickWithDialog(allowedExtensions: List<String>, multi: Boolean): List<File>? = withContext(Dispatchers.IO) {
        val parent: Frame? = desktopCtx.parent
        val dialog = FileDialog(parent, "Pick file", FileDialog.LOAD)
        dialog.isMultipleMode = multi
        if (allowedExtensions.isNotEmpty()) {
            dialog.setFilenameFilter { _, name ->
                val lower = name.lowercase()
                allowedExtensions.any { lower.endsWith(".${it.lowercase()}") }
            }
        }
        dialog.isVisible = true
        val selected = dialog.files?.toList().orEmpty()
        if (selected.isEmpty()) null else selected
    }

    private fun toMediaFile(file: File): MediaFile = DesktopMediaFile(file)

    private fun mimeToExtensions(mime: String): List<String> = when {
        mime == "*/*" -> emptyList() // no filter
        mime.startsWith("image/") -> IMAGE_EXTENSIONS
        mime.startsWith("video/") -> VIDEO_EXTENSIONS
        mime == "application/pdf" -> listOf("pdf")
        else -> emptyList()
    }

    private companion object {
        val IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic")
        val VIDEO_EXTENSIONS = listOf("mp4", "mov", "mkv", "webm", "avi")
    }
}
