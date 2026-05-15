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
 * Discriminated result of a [MediaPicker] operation. The library never throws across the
 * public boundary except for programmer errors (e.g. invalid config values caught at the
 * call site). All platform / I/O failures are surfaced as [Error] or [PermissionDenied];
 * user-driven cancellation is [Cancelled]; targets that don't support an operation return
 * [Unsupported] cleanly so consumers can feature-detect without `try/catch`.
 */
public sealed interface MediaPickerResult<out T> {
    /** User completed the operation and the result is available. */
    public data class Success<T>(public val data: T) : MediaPickerResult<T>

    /** User dismissed the picker / camera app. */
    public data object Cancelled : MediaPickerResult<Nothing>

    /**
     * Required platform permission was denied. [permanentlyDenied] is `true` if the user
     * has disabled the runtime prompt (Android "Don't ask again" / iOS Settings revoked).
     */
    public data class PermissionDenied(
        public val permission: String,
        public val permanentlyDenied: Boolean,
    ) : MediaPickerResult<Nothing>

    /** The current platform / device does not support this operation. */
    public data object Unsupported : MediaPickerResult<Nothing>

    /** Unexpected platform-level failure. The wrapped [throwable] is non-null. */
    public data class Error(public val throwable: Throwable) : MediaPickerResult<Nothing>
}
