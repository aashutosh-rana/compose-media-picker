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

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.CaptureVideo
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import io.github.aashutosh.mediapicker.CameraCaptureConfig
import io.github.aashutosh.mediapicker.FilePickerConfig
import io.github.aashutosh.mediapicker.ImagePickerConfig
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig
import io.github.aashutosh.mediapicker.internal.image.postProcessImage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Android implementation of [MediaPickerEngine].
 *
 * Each picker / capture method registers a one-shot [ActivityResultLauncher] against the
 * activity's [ActivityResultRegistry], suspends on a [suspendCancellableCoroutine], and
 * unregisters in `invokeOnCancellation` so dispose / coroutine cancellation cleans up
 * deterministically.
 *
 * No `Context`, `Activity`, or `Uri` is held outside the engine's own composition-scoped
 * lifetime — the auto-discovery in `rememberMediaPicker()` re-keys the engine if the
 * hosting `ComponentActivity` ever changes.
 */
internal actual class MediaPickerEngine actual constructor(context: InternalPlatformContext) {

    private val androidCtx = context as AndroidPlatformContext
    private val keyCounter = AtomicInteger(0)
    private val tempFiles = TempFileRegistry(androidCtx.activity.applicationContext)

    actual fun attach() {
        tempFiles.sweepStaleSync()
    }

    actual fun detach() {
        tempFiles.cleanup()
    }

    actual suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile> {
        val uri = pickVisual(PickVisualMedia.ImageOnly) ?: return MediaPickerResult.Cancelled
        val processed = toMediaFile(uri).postProcessImage(config.compression, config.applyExifRotation)
        return MediaPickerResult.Success(processed)
    }

    actual suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>> {
        val uris = pickMultipleVisual(PickVisualMedia.ImageOnly, config.maxItems)
        if (uris.isNullOrEmpty()) return MediaPickerResult.Cancelled
        val processed = uris.map {
            toMediaFile(it).postProcessImage(config.compression, config.applyExifRotation)
        }
        return MediaPickerResult.Success(processed)
    }

    actual suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile> {
        val uri = pickVisual(PickVisualMedia.VideoOnly) ?: return MediaPickerResult.Cancelled
        val file = toMediaFile(uri)
        if (config.maxSizeBytes != null && file.sizeBytes > config.maxSizeBytes) {
            return MediaPickerResult.Error(IllegalStateException("Video exceeds maxSizeBytes"))
        }
        return MediaPickerResult.Success(file)
    }

    actual suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile> {
        val mimes = config.mimeTypes.toTypedArray()
        return if (config.allowMultiple) {
            val uris = launchActivityResult<Array<String>, List<Uri>>(OpenMultipleDocuments(), mimes)
            if (uris.isNullOrEmpty()) {
                MediaPickerResult.Cancelled
            } else {
                MediaPickerResult.Success(toMediaFile(uris.first()))
            }
        } else {
            val uri = launchActivityResult<Array<String>, Uri?>(OpenDocument(), mimes)
            if (uri == null) {
                MediaPickerResult.Cancelled
            } else {
                MediaPickerResult.Success(toMediaFile(uri))
            }
        }
    }

    actual suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile> {
        val (file, uri) = tempFiles.newCaptureFile("jpg")
        val ok = launchActivityResult<Uri, Boolean>(TakePicture(), uri) ?: false
        if (!ok || file.length() == 0L) {
            file.delete()
            return MediaPickerResult.Cancelled
        }
        if (config.saveToGallery) {
            saveToGallery(uri, isImage = true)
        }
        return MediaPickerResult.Success(toMediaFile(uri, mimeOverride = "image/jpeg"))
    }

    actual suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile> {
        val (file, uri) = tempFiles.newCaptureFile("mp4")
        val ok = launchActivityResult<Uri, Boolean>(CaptureVideo(), uri) ?: false
        if (!ok || file.length() == 0L) {
            file.delete()
            return MediaPickerResult.Cancelled
        }
        if (config.saveToGallery) {
            saveToGallery(uri, isImage = false)
        }
        return MediaPickerResult.Success(toMediaFile(uri, mimeOverride = "video/mp4"))
    }

    // -- helpers ----------------------------------------------------------------------

    private suspend fun pickVisual(type: VisualMediaType): Uri? = launchActivityResult<PickVisualMediaRequest, Uri?>(
        PickVisualMedia(),
        PickVisualMediaRequest.Builder().setMediaType(type).build(),
    )

    private suspend fun pickMultipleVisual(type: VisualMediaType, max: Int): List<Uri>? {
        // PickMultipleVisualMedia's constructor requires maxItems > 1 — it throws
        // IllegalArgumentException("Max items must be higher than 1") otherwise. When the
        // caller wants exactly one item, fall back to the single-pick contract.
        if (max <= 1) {
            val uri = pickVisual(type) ?: return null
            return listOf(uri)
        }
        val req = PickVisualMediaRequest.Builder().setMediaType(type).build()
        return launchActivityResult<PickVisualMediaRequest, List<Uri>>(
            PickMultipleVisualMedia(max),
            req,
        )
    }

    private suspend fun <I, O> launchActivityResult(contract: ActivityResultContract<I, O>, input: I): O? =
        suspendCancellableCoroutine { cont ->
            val activity: ComponentActivity = androidCtx.activity
            val key = "media-picker-${keyCounter.incrementAndGet()}"
            var launcher: ActivityResultLauncher<I>? = null
            launcher = activity.activityResultRegistry.register(key, contract) { result ->
                launcher?.unregister()
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation {
                launcher?.unregister()
            }
            launcher.launch(input)
        }

    private fun saveToGallery(source: Uri, isImage: Boolean) {
        val activity = androidCtx.activity
        val resolver = activity.contentResolver
        val collection = if (isImage) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, queryName(resolver, source) ?: "MediaPicker")
            put(MediaStore.MediaColumns.MIME_TYPE, if (isImage) "image/jpeg" else "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isImage) "Pictures/MediaPicker" else "Movies/MediaPicker",
                )
            }
        }
        val target = resolver.insert(collection, values) ?: return
        runCatching {
            resolver.openInputStream(source).use { input ->
                resolver.openOutputStream(target).use { output ->
                    if (input != null && output != null) input.copyTo(output)
                }
            }
        }
    }

    private fun toMediaFile(uri: Uri, mimeOverride: String? = null): MediaFile {
        val resolver = androidCtx.activity.contentResolver
        val mime = mimeOverride ?: queryMimeType(resolver, uri)
        return AndroidMediaFile(
            resolver = resolver,
            uri = uri,
            mimeType = mime,
        )
    }
}
