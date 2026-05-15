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
import io.github.aashutosh.mediapicker.FilePickerConfig
import io.github.aashutosh.mediapicker.ImagePickerConfig
import io.github.aashutosh.mediapicker.InternalMediaPickerApi
import io.github.aashutosh.mediapicker.IosPlatformContext
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.PlatformContext
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSFileManager
import platform.Foundation.NSItemProvider
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerViewController
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraDevice
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIModalPresentationFormSheet
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UniformTypeIdentifiers.UTTypeMovie
import kotlin.coroutines.resume

@OptIn(InternalMediaPickerApi::class, ExperimentalForeignApi::class)
public actual class MediaPickerEngine public actual constructor(context: PlatformContext) {

    private val iosCtx = context as? IosPlatformContext
        ?: error("iOS target requires IosPlatformContext, got ${context::class.simpleName}")

    // Strong refs while a picker is in flight; nulled in finally / detach.
    private var phDelegate: PhPickerDelegate? = null
    private var docDelegate: DocumentPickerDelegate? = null
    private var camDelegate: CameraDelegate? = null

    public actual fun attach() {
        // Nothing to do — UI hooks are per-call.
    }

    public actual fun detach() {
        phDelegate = null
        docDelegate = null
        camDelegate = null
    }

    public actual suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile> =
        pickFromPhPicker(PHPickerFilter.imagesFilter(), maxSelection = 1)?.firstOrNull()
            ?.let { MediaPickerResult.Success(it) }
            ?: MediaPickerResult.Cancelled

    public actual suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>> {
        val files = pickFromPhPicker(PHPickerFilter.imagesFilter(), maxSelection = config.maxItems)
        return if (files.isNullOrEmpty()) {
            MediaPickerResult.Cancelled
        } else {
            MediaPickerResult.Success(files)
        }
    }

    public actual suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile> {
        val files = pickFromPhPicker(PHPickerFilter.videosFilter(), maxSelection = 1)
        val file = files?.firstOrNull() ?: return MediaPickerResult.Cancelled
        if (config.maxSizeBytes != null && file.sizeBytes > config.maxSizeBytes) {
            return MediaPickerResult.Error(IllegalStateException("Video exceeds maxSizeBytes"))
        }
        return MediaPickerResult.Success(file)
    }

    public actual suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile> {
        val types: List<UTType> = config.mimeTypes
            .mapNotNull { UTType.typeWithMIMEType(it) }
            .ifEmpty { listOf(UTTypeItem) }

        val urls = suspendCancellableCoroutine<List<NSURL>?> { cont ->
            runOnMain {
                val controller = UIDocumentPickerViewController(forOpeningContentTypes = types)
                controller.allowsMultipleSelection = config.allowMultiple
                controller.modalPresentationStyle = UIModalPresentationFormSheet
                val delegate = DocumentPickerDelegate(
                    onPick = { picked ->
                        cont.resumeIfActive(picked)
                        docDelegate = null
                    },
                    onCancel = {
                        cont.resumeIfActive(null)
                        docDelegate = null
                    },
                )
                docDelegate = delegate
                controller.delegate = delegate
                iosCtx.viewController.presentViewController(controller, animated = true, completion = null)
                cont.invokeOnCancellation {
                    runOnMain { controller.dismissViewControllerAnimated(true, completion = null) }
                    docDelegate = null
                }
            }
        } ?: return MediaPickerResult.Cancelled

        if (urls.isEmpty()) return MediaPickerResult.Cancelled
        val first = urls.first()
        return MediaPickerResult.Success(toMediaFile(first, mimeOverride = null))
    }

    public actual suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile> =
        captureFromCamera(isImage = true, frontCamera = config.preferFrontCamera, saveToGallery = config.saveToGallery)

    public actual suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile> =
        captureFromCamera(isImage = false, frontCamera = false, saveToGallery = config.saveToGallery)

    // -- internals --------------------------------------------------------------------

    private suspend fun pickFromPhPicker(filter: PHPickerFilter, maxSelection: Int): List<MediaFile>? {
        val itemProviders = suspendCancellableCoroutine<List<Any>?> { cont ->
            runOnMain {
                val configuration = PHPickerConfiguration().apply {
                    setFilter(filter)
                    selectionLimit = maxSelection.toLong()
                }
                val controller = PHPickerViewController(configuration = configuration)
                val delegate = PhPickerDelegate { results ->
                    cont.resumeIfActive(results)
                    phDelegate = null
                }
                phDelegate = delegate
                controller.delegate = delegate
                iosCtx.viewController.presentViewController(controller, animated = true, completion = null)
                cont.invokeOnCancellation {
                    runOnMain { controller.dismissViewControllerAnimated(true, completion = null) }
                    phDelegate = null
                }
            }
        } ?: return null

        if (itemProviders.isEmpty()) return null
        return itemProviders.mapNotNull { provider ->
            (provider as? NSItemProvider)?.let { loadFile(it) }
        }
    }

    private suspend fun loadFile(provider: NSItemProvider): MediaFile? = suspendCancellableCoroutine { cont ->
        val typeId = when {
            provider.hasItemConformingToTypeIdentifier(UTTypeMovie.identifier) -> UTTypeMovie.identifier
            provider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier) -> UTTypeImage.identifier
            else -> UTTypeItem.identifier
        }
        provider.loadFileRepresentationForTypeIdentifier(typeId) { url, error ->
            if (url == null || error != null) {
                cont.resumeIfActive(null)
                return@loadFileRepresentationForTypeIdentifier
            }
            // The OS deletes the source URL after the completion handler returns;
            // copy into our sandbox so [MediaFile] can read it later.
            val copied = copyToSandbox(url)
            val name = provider.suggestedName ?: copied.lastPathComponent ?: "media"
            val mime = mimeForUti(typeId)
            cont.resumeIfActive(toMediaFile(copied, mime, displayName = name))
        }
    }

    private suspend fun captureFromCamera(isImage: Boolean, frontCamera: Boolean, saveToGallery: Boolean): MediaPickerResult<MediaFile> {
        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            return MediaPickerResult.Unsupported
        }
        @Suppress("UnusedPrivateProperty")
        val ignored = saveToGallery // TODO: integrate PHPhotoLibrary save once we have NSPhotoLibraryAddUsageDescription docs

        val info = suspendCancellableCoroutine<Map<Any?, *>?> { cont ->
            runOnMain {
                val controller = UIImagePickerController()
                controller.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                controller.cameraDevice = if (frontCamera) {
                    UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront
                } else {
                    UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear
                }
                controller.mediaTypes = listOf(
                    if (isImage) UTTypeImage.identifier else UTTypeMovie.identifier,
                )
                val delegate = CameraDelegate { capturedInfo, cancelled ->
                    cont.resumeIfActive(if (cancelled) null else capturedInfo)
                    camDelegate = null
                }
                camDelegate = delegate
                controller.delegate = delegate
                iosCtx.viewController.presentViewController(controller, animated = true, completion = null)
                cont.invokeOnCancellation {
                    runOnMain { controller.dismissViewControllerAnimated(true, completion = null) }
                    camDelegate = null
                }
            }
        } ?: return MediaPickerResult.Cancelled

        val mediaUrl = info["UIImagePickerControllerMediaURL"] as? NSURL
        val imageUrl = info["UIImagePickerControllerImageURL"] as? NSURL
        val source = mediaUrl ?: imageUrl ?: return MediaPickerResult.Error(IllegalStateException("No URL in camera result"))
        val copied = copyToSandbox(source)
        val mime = if (isImage) "image/jpeg" else "video/mp4"
        return MediaPickerResult.Success(toMediaFile(copied, mime, copied.lastPathComponent ?: "capture"))
    }

    private fun copyToSandbox(src: NSURL): NSURL {
        val fm = NSFileManager.defaultManager
        val ext = src.pathExtension?.takeIf { it.isNotEmpty() } ?: "bin"
        val target = NSURL.fileURLWithPath("${NSTemporaryDirectory()}${NSUUID().UUIDString}.$ext")
        // Best-effort: on failure, return original (it may still be valid for a short window).
        runCatching { fm.copyItemAtURL(src, target, null) }
        return if (fm.fileExistsAtPath(target.path ?: "")) target else src
    }

    private fun toMediaFile(url: NSURL, mimeOverride: String?, displayName: String? = null): MediaFile = IosMediaFile(
        url = url,
        mimeType = mimeOverride ?: mimeForUti(UTTypeItem.identifier),
        name = displayName ?: url.lastPathComponent ?: "media",
    )

    private fun mimeForUti(identifier: String): String? {
        val type = UTType.typeWithIdentifier(identifier) ?: return null
        return type.preferredMIMEType
    }
}

private fun <T> kotlinx.coroutines.CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value)
}
