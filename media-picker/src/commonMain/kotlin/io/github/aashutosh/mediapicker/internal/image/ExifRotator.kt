/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.internal.image

import io.github.aashutosh.mediapicker.InternalMediaPickerApi

/**
 * Reads EXIF orientation metadata and returns a JPEG with pixels rotated so consumers can
 * render the bytes directly without honoring EXIF. Returns [input] unchanged if no
 * rotation is needed or the file isn't a JPEG with EXIF.
 */
@InternalMediaPickerApi
public interface ExifRotator {
    public suspend fun applyOrientation(input: ByteArray): ByteArray
}

@InternalMediaPickerApi
public expect fun platformExifRotator(): ExifRotator
