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

import kotlinx.coroutines.flow.Flow
import kotlinx.io.Source

/**
 * Lightweight reference to a picked / captured media file. Bytes are not loaded until
 * explicitly requested — see [source], [loadBytes], and [thumbnail].
 *
 * Implementations are platform-specific (Android `Uri`-backed, iOS `NSURL`-backed, etc.)
 * but always honor the same contract: every I/O method dispatches off-main, and reads are
 * cancellable via the calling coroutine's `Job`.
 */
public interface MediaFile {
    /** Display name as reported by the OS (e.g. `IMG_20260514_103045.jpg`). */
    public val name: String

    /** Best-effort MIME type. `null` if the OS couldn't determine it. */
    public val mimeType: String?

    /** Size in bytes. `-1` if unknown without reading the file. */
    public val sizeBytes: Long

    /** Pixel width for images and videos. `null` for non-visual files. */
    public val width: Int?

    /** Pixel height for images and videos. `null` for non-visual files. */
    public val height: Int?

    /** Duration in milliseconds for videos. `null` for non-video files. */
    public val durationMs: Long?

    /**
     * Returns a streaming [Source] over the file contents. Caller owns the source and
     * must `close()` it. Off-main; cancellation closes the underlying handle.
     */
    public suspend fun source(): Source

    /**
     * Convenience: loads the entire file into memory off-main. Avoid for files larger
     * than a few MB — prefer [source].
     */
    public suspend fun loadBytes(): ByteArray

    /**
     * Generates a thumbnail bitmap (JPEG-encoded bytes) sized so its longest edge is at
     * most [maxDimension] pixels. Returns `null` if the platform cannot generate a
     * thumbnail for this file type.
     */
    public suspend fun thumbnail(maxDimension: Int = THUMBNAIL_DEFAULT_DIMENSION): ByteArray?

    /**
     * Cold [Flow] that emits [Progress] updates while a subsequent [loadBytes] /
     * [source]-consumption is in flight. Consumers collect this on a UI scope before
     * starting the read.
     */
    public fun readProgress(): Flow<Progress>

    public companion object {
        public const val THUMBNAIL_DEFAULT_DIMENSION: Int = 256
    }
}

/** Progress event for streamed reads. [totalBytes] is `-1` if size is unknown. */
public data class Progress(
    public val bytesRead: Long,
    public val totalBytes: Long,
)
