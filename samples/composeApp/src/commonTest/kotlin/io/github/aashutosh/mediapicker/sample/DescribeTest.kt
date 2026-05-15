/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.sample

import io.github.aashutosh.mediapicker.MediaPickerResult
import kotlin.test.Test
import kotlin.test.assertTrue

class DescribeTest {

    @Test
    fun describesCancelled() {
        assertTrue(describeForTest(MediaPickerResult.Cancelled).contains("Cancelled", ignoreCase = true))
    }

    @Test
    fun describesUnsupported() {
        assertTrue(describeForTest(MediaPickerResult.Unsupported).contains("Unsupported", ignoreCase = true))
    }

    @Test
    fun describesPermissionDenied() {
        val r = MediaPickerResult.PermissionDenied(permission = "test.permission", permanentlyDenied = false)
        assertTrue(describeForTest(r).contains("Permission denied"))
        assertTrue(describeForTest(r).contains("test.permission"))
    }
}

/** Mirrors the internal `describe` used in App.kt. Tested separately so the production helper can stay private. */
private fun describeForTest(result: MediaPickerResult<*>): String = when (result) {
    is MediaPickerResult.Success<*> -> "Picked successfully."
    MediaPickerResult.Cancelled -> "Cancelled."
    MediaPickerResult.Unsupported -> "Unsupported on this device."
    is MediaPickerResult.PermissionDenied -> "Permission denied: ${result.permission}"
    is MediaPickerResult.Error -> "Error: ${result.throwable.message}"
}
