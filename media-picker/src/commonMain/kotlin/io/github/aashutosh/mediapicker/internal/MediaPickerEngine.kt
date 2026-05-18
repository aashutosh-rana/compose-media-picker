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
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig

/**
 * Platform-specific implementation surface. Internal because the only consumer is
 * [DefaultMediaPicker] — there's no public picker setup API in 0.2.x, and a consumer
 * faking the engine should fake the public [io.github.aashutosh.mediapicker.MediaPicker]
 * interface instead.
 */
internal expect class MediaPickerEngine(context: InternalPlatformContext) {

    fun attach()

    fun detach()

    suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile>

    suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>>

    suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile>

    suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile>

    suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile>

    suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile>
}
