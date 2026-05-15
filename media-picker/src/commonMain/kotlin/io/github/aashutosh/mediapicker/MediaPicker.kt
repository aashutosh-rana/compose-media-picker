/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker

/**
 * Unified media-picker entry point. Obtain an instance via [rememberMediaPicker] inside a
 * composable; never construct directly.
 *
 * All methods are `suspend` and cancellable. A cancelled coroutine produces no result —
 * pending platform pickers are dismissed and any temp files are cleaned up.
 *
 * Camera methods ([captureImage], [captureVideo]) delegate to the system camera app and
 * require no `CAMERA` permission from the consumer. On platforms without a system camera
 * (e.g. desktop), they return [MediaPickerResult.Unsupported].
 *
 * Example:
 * ```
 * val picker = rememberMediaPicker()
 * val scope = rememberCoroutineScope()
 * Button(onClick = {
 *     scope.launch {
 *         when (val r = picker.pickImage()) {
 *             is MediaPickerResult.Success -> display(r.data)
 *             is MediaPickerResult.Cancelled -> {}
 *             else -> showError(r)
 *         }
 *     }
 * }) { Text("Pick image") }
 * ```
 */
public interface MediaPicker {
    /** Picks a single image from the system gallery / photo picker. */
    public suspend fun pickImage(config: ImagePickerConfig = ImagePickerConfig()): MediaPickerResult<MediaFile>

    /** Picks up to [MultiImagePickerConfig.maxItems] images from the system gallery. */
    public suspend fun pickImages(config: MultiImagePickerConfig = MultiImagePickerConfig()): MediaPickerResult<List<MediaFile>>

    /** Picks a single video from the system gallery / photo picker. */
    public suspend fun pickVideo(config: VideoPickerConfig = VideoPickerConfig()): MediaPickerResult<MediaFile>

    /** Picks an arbitrary file via the system document picker. */
    public suspend fun pickFile(config: FilePickerConfig = FilePickerConfig()): MediaPickerResult<MediaFile>

    /**
     * Launches the system camera app to capture a still image.
     * Requires no `CAMERA` permission from the consumer — the camera app owns it.
     */
    public suspend fun captureImage(config: CameraCaptureConfig = CameraCaptureConfig()): MediaPickerResult<MediaFile>

    /**
     * Launches the system camera app to capture a video.
     * [VideoCaptureConfig.maxDurationSeconds] and [VideoCaptureConfig.quality] are hints
     * that may be ignored by the camera app.
     */
    public suspend fun captureVideo(config: VideoCaptureConfig = VideoCaptureConfig()): MediaPickerResult<MediaFile>
}
