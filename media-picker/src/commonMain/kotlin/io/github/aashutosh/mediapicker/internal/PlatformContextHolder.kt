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

import io.github.aashutosh.mediapicker.InternalMediaPickerApi
import io.github.aashutosh.mediapicker.PlatformContext
import kotlinx.atomicfu.atomic

/**
 * Single source of truth for the registered [PlatformContext]. Stored as an atomic
 * reference so [io.github.aashutosh.mediapicker.initializeMediaPicker] is safe to call
 * from any thread.
 *
 * Engines read the context lazily inside `rememberMediaPicker` — not at composition init,
 * which lets the consumer initialize after Compose has already started in tests.
 */
@InternalMediaPickerApi
public object PlatformContextHolder {
    private val ref = atomic<PlatformContext?>(null)

    public fun set(context: PlatformContext) {
        ref.value = context
    }

    public fun get(): PlatformContext? = ref.value

    public fun require(): PlatformContext = checkNotNull(ref.value) {
        "MediaPicker not initialized — call initializeMediaPicker(...) before invoking " +
            "rememberMediaPicker()."
    }

    /** Test-only: reset state between cases. */
    public fun clear() {
        ref.value = null
    }
}
