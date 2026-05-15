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

import androidx.core.content.FileProvider

/**
 * Dedicated [FileProvider] subclass so the library's auto-merged manifest entry can't
 * collide with one the consumer (or another library) declares using the bare
 * `androidx.core.content.FileProvider` class.
 *
 * Authority is `${applicationId}.mediakit.fileprovider`.
 */
internal class MediaPickerFileProvider : FileProvider()
