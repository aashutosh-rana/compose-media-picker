/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(InternalMediaPickerApi::class)

package io.github.aashutosh.mediapicker

/**
 * Marker for a platform-specific handle (Android `ComponentActivity`,
 * iOS `UIViewController`, Desktop `Frame`, Web — none required).
 *
 * Concrete subtypes live in each platform's source set. Consumers obtain an instance
 * from the helper in their target's main module and pass it once to
 * [initializeMediaPicker] at app startup.
 *
 * Not intended to be implemented by consumers — only the per-target subtypes shipped by
 * the library are supported.
 */
public interface PlatformContext

/**
 * Wire a [PlatformContext] into the library. Must be called once before any composable
 * tries to obtain a [MediaPicker] via [rememberMediaPicker]. Calling again with a new
 * context replaces the previous registration and cleans up any pending temp files.
 *
 * Thread-safe; safe to call from any dispatcher.
 */
public fun initializeMediaPicker(context: PlatformContext) {
    io.github.aashutosh.mediapicker.internal.PlatformContextHolder.set(context)
}
