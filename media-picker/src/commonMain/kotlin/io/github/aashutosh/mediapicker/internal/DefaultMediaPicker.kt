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
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPicker
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig
import kotlinx.coroutines.CancellationException

/**
 * The single funnel through which every public picker call passes. Wraps engine calls in
 * [runCatching] so consumers never see an uncaught platform exception, while still
 * letting structured-concurrency cancellation propagate.
 */
@OptIn(io.github.aashutosh.mediapicker.InternalMediaPickerApi::class)
internal class DefaultMediaPicker(
    private val engine: MediaPickerEngine,
) : MediaPicker {

    override suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile> = funnel { engine.pickImage(config) }

    override suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>> =
        funnel { engine.pickImages(config) }

    override suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile> = funnel { engine.pickVideo(config) }

    override suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile> = funnel { engine.pickFile(config) }

    override suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile> = funnel { engine.captureImage(config) }

    override suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile> = funnel { engine.captureVideo(config) }

    private inline fun <T> funnel(block: () -> MediaPickerResult<T>): MediaPickerResult<T> = try {
        block()
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        MediaPickerResult.Error(t)
    }
}
