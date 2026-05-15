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
import kotlin.test.assertFailsWith

class ConfigsTest {

    @Test
    fun multiImageDefaults() {
        val c = MultiImagePickerConfig()
        assertEquals(MultiImagePickerConfig.DEFAULT_MAX_ITEMS, c.maxItems)
    }

    @Test
    fun multiImageRejectsZeroMax() {
        assertFailsWith<IllegalArgumentException> { MultiImagePickerConfig(maxItems = 0) }
    }

    @Test
    fun multiImageRejectsOversizedMax() {
        assertFailsWith<IllegalArgumentException> {
            MultiImagePickerConfig(maxItems = MultiImagePickerConfig.MAX_ALLOWED_ITEMS + 1)
        }
    }

    @Test
    fun compressionDefaults() {
        val c = CompressionConfig()
        assertEquals(CompressionConfig.DEFAULT_MAX_DIMENSION, c.maxDimension)
        assertEquals(CompressionConfig.DEFAULT_QUALITY, c.qualityPercent)
    }

    @Test
    fun compressionRejectsBadValues() {
        assertFailsWith<IllegalArgumentException> { CompressionConfig(maxDimension = 0) }
        assertFailsWith<IllegalArgumentException> { CompressionConfig(qualityPercent = 0) }
        assertFailsWith<IllegalArgumentException> { CompressionConfig(qualityPercent = 101) }
    }
}
