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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MediaPickerResultTest {

    @Test
    fun successCarriesData() {
        val r: MediaPickerResult<String> = MediaPickerResult.Success("hello")
        assertIs<MediaPickerResult.Success<String>>(r)
        assertEquals("hello", r.data)
    }

    @Test
    fun cancelledAndUnsupportedAreSingletons() {
        assertTrue(MediaPickerResult.Cancelled === MediaPickerResult.Cancelled)
        assertTrue(MediaPickerResult.Unsupported === MediaPickerResult.Unsupported)
    }

    @Test
    fun permissionDeniedRecordsScope() {
        val r = MediaPickerResult.PermissionDenied(
            permission = "android.permission.READ_MEDIA_IMAGES",
            permanentlyDenied = true,
        )
        assertEquals("android.permission.READ_MEDIA_IMAGES", r.permission)
        assertTrue(r.permanentlyDenied)
    }

    @Test
    fun errorWrapsThrowable() {
        val cause = IllegalStateException("boom")
        val r = MediaPickerResult.Error(cause)
        assertEquals(cause, r.throwable)
    }
}
