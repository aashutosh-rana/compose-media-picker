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
import io.github.aashutosh.mediapicker.Progress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Per-[io.github.aashutosh.mediapicker.MediaFile] hot channel used by streaming reads to
 * publish progress events. UI collectors hop on via
 * [io.github.aashutosh.mediapicker.MediaFile.readProgress].
 *
 * Replay = 1 so a late subscriber gets the most recent event immediately.
 */
@InternalMediaPickerApi
public class ProgressChannel {
    private val flow = MutableSharedFlow<Progress>(replay = 1, extraBufferCapacity = 8)

    public fun asFlow(): SharedFlow<Progress> = flow.asSharedFlow()

    public fun emit(progress: Progress) {
        flow.tryEmit(progress)
    }
}
