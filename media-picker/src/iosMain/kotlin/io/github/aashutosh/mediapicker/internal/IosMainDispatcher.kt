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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread

/** Posts [block] to the main run loop. Safe to call from any thread. */
@OptIn(ExperimentalForeignApi::class)
internal fun runOnMain(block: () -> Unit) {
    if (NSThread.isMainThread) {
        block()
    } else {
        NSOperationQueue.mainQueue.addOperationWithBlock(block)
    }
}
