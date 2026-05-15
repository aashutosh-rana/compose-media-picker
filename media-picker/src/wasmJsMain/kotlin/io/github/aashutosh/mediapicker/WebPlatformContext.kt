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
 * Web (wasmJs) [PlatformContext]. No handle required — the engine creates a transient
 * `<input type="file">` element each pick. Construct once and pass to [initializeMediaPicker].
 */
public class WebPlatformContext : PlatformContext
