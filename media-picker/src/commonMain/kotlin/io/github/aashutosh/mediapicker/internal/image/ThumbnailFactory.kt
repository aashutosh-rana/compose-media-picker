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

/** Generates a JPEG thumbnail with longest edge ≤ [maxDimension]. */
@InternalMediaPickerApi
public interface ThumbnailFactory {
    public suspend fun create(input: ByteArray, maxDimension: Int): ByteArray?
}

@InternalMediaPickerApi
public expect fun platformThumbnailFactory(): ThumbnailFactory
