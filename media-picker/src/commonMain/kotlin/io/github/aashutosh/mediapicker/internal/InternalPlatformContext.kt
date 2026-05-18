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

/**
 * Marker for the per-platform host handle that [MediaPickerEngine] needs. Implementations
 * (one per target) are package-private to the library and carry the concrete handle:
 *
 * - Android: `ComponentActivity`
 * - iOS: `UIViewController`
 * - Desktop: `Frame` (the Compose window)
 * - Web: nothing — empty data class
 *
 * Discovery is per-composition via [discoverPlatformContext], so the handle is never
 * retained globally and equality is identity-based — keying `remember` on the context
 * recreates the engine if the host changes.
 */
internal interface InternalPlatformContext
