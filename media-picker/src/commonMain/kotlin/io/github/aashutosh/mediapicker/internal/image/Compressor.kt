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

import io.github.aashutosh.mediapicker.CompressionConfig
import io.github.aashutosh.mediapicker.InternalMediaPickerApi

/**
 * Resizes and re-encodes an image to JPEG. Implementations dispatch onto a background
 * dispatcher; the contract is `suspend` so they MUST NOT block the caller's thread.
 */
@InternalMediaPickerApi
public interface Compressor {
    public suspend fun compress(input: ByteArray, config: CompressionConfig): ByteArray
}

/** Resolves the platform-default compressor. */
@InternalMediaPickerApi
public expect fun platformCompressor(): Compressor
