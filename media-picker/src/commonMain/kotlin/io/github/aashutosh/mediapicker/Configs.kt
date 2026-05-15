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

/** Options for [MediaPicker.pickImage]. */
public data class ImagePickerConfig(
    /** If non-null, the picked image is resized + re-encoded to JPEG using these parameters. */
    public val compression: CompressionConfig? = null,
    /** Apply EXIF orientation rotation so consumers always get an upright image. Default `true`. */
    public val applyExifRotation: Boolean = true,
)

/** Options for [MediaPicker.pickImages]. */
public data class MultiImagePickerConfig(
    /** Upper bound on selectable items. Platforms may cap this further. Default 10. */
    public val maxItems: Int = DEFAULT_MAX_ITEMS,
    public val compression: CompressionConfig? = null,
    public val applyExifRotation: Boolean = true,
) {
    init {
        require(maxItems in 1..MAX_ALLOWED_ITEMS) {
            "maxItems must be between 1 and $MAX_ALLOWED_ITEMS, was $maxItems"
        }
    }

    public companion object {
        public const val DEFAULT_MAX_ITEMS: Int = 10
        public const val MAX_ALLOWED_ITEMS: Int = 100
    }
}

/** Options for [MediaPicker.pickVideo]. */
public data class VideoPickerConfig(
    /** If non-null and the picked video exceeds this, the picker returns [MediaPickerResult.Error]. */
    public val maxSizeBytes: Long? = null,
)

/** Options for [MediaPicker.pickFile]. */
public data class FilePickerConfig(
    /** MIME filter list. Default is a single wildcard entry that allows every file. */
    public val mimeTypes: List<String> = listOf("*/*"),
    public val allowMultiple: Boolean = false,
)

/** Options for [MediaPicker.captureImage]. */
public data class CameraCaptureConfig(
    /** If `true`, the captured image is also saved to the device gallery. */
    public val saveToGallery: Boolean = false,
    /** Hint to the camera app to start with the front-facing camera. Honored when supported. */
    public val preferFrontCamera: Boolean = false,
)

/** Options for [MediaPicker.captureVideo]. */
public data class VideoCaptureConfig(
    /** Hint only — many camera apps ignore this. */
    public val maxDurationSeconds: Int? = null,
    public val quality: VideoQuality = VideoQuality.High,
    public val saveToGallery: Boolean = false,
)

/** In-library image compression / resize parameters. */
public data class CompressionConfig(
    /** Longest edge in pixels after resize. Aspect ratio is preserved. */
    public val maxDimension: Int = DEFAULT_MAX_DIMENSION,
    /** JPEG quality from 1 to 100. Default 85 — visually lossless for most photos. */
    public val qualityPercent: Int = DEFAULT_QUALITY,
) {
    init {
        require(maxDimension > 0) { "maxDimension must be positive, was $maxDimension" }
        require(qualityPercent in 1..MAX_QUALITY) {
            "qualityPercent must be 1..$MAX_QUALITY, was $qualityPercent"
        }
    }

    public companion object {
        public const val DEFAULT_MAX_DIMENSION: Int = 1920
        public const val DEFAULT_QUALITY: Int = 85
        public const val MAX_QUALITY: Int = 100
    }
}

/** Video capture quality hint. */
public enum class VideoQuality { Low, High }
