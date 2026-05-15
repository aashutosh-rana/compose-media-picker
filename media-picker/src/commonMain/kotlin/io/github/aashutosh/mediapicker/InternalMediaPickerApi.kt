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

/**
 * Marker for symbols that are public for cross-source-set wiring but are NOT part of the
 * stable API surface. They are excluded from binary-compatibility-validator via
 * `apiValidation.nonPublicMarkers` in `media-picker/build.gradle.kts`.
 *
 * Consumers should never call symbols annotated with this; doing so opts out of stability
 * guarantees.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "Internal media-picker API — not subject to semantic versioning.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
public annotation class InternalMediaPickerApi
