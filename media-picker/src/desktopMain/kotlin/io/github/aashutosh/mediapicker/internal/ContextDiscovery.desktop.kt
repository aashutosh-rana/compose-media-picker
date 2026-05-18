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
import androidx.compose.runtime.remember
import java.awt.Frame

internal data class DesktopPlatformContext(val parent: Frame?) : InternalPlatformContext

/**
 * Compose Desktop's [androidx.compose.ui.window.LocalWindow] CompositionLocal is internal,
 * so we recover the hosting frame from AWT itself: prefer the focused frame, fall back to
 * the first visible one. Composition only runs after `Window { ... }` has registered its
 * frame, so [Frame.getFrames] is populated by the time this is called.
 */
@Composable
internal actual fun discoverPlatformContext(): InternalPlatformContext? = remember {
    val frames = Frame.getFrames()
    val parent: Frame? = frames.firstOrNull { it.isFocused }
        ?: frames.firstOrNull { it.isVisible }
        ?: frames.firstOrNull()
    DesktopPlatformContext(parent)
}
