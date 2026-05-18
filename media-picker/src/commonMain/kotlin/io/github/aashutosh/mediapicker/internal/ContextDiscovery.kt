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

import androidx.compose.runtime.Composable

/**
 * Per-target discovery of the current Compose host. Returns `null` only in degenerate
 * setups (no `ComponentActivity` on Android, no active scene on iOS, etc.); the public
 * [io.github.aashutosh.mediapicker.rememberMediaPicker] turns `null` into a helpful
 * `IllegalStateException` with a pointer to the README.
 */
@Composable
internal expect fun discoverPlatformContext(): InternalPlatformContext?
