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
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.PlatformContext
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig

/**
 * Platform-specific implementation surface. This is the ONLY `expect class` in the
 * library — every public type is a pure-Kotlin interface or data class.
 *
 * Implementations live in `*Main` source sets and are responsible for:
 * - registering platform launchers / delegates on [attach]
 * - tearing them down (and cancelling pending continuations) on [detach]
 * - mapping every platform success / cancel / failure into a [MediaPickerResult]
 */
@InternalMediaPickerApi
public expect class MediaPickerEngine(context: PlatformContext) {

    public fun attach()

    public fun detach()

    public suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile>

    public suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>>

    public suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile>

    public suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile>

    public suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile>

    public suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile>
}
