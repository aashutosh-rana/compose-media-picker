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

import java.awt.Frame
import java.lang.ref.WeakReference

/**
 * Desktop (JVM) [PlatformContext]. Pass the application's top-level [Frame] (e.g. the
 * Compose Desktop window) so file dialogs are modal to it.
 *
 * Pass `null` if you have no parent frame; dialogs will be non-modal.
 */
public class DesktopPlatformContext(parent: Frame? = null) : PlatformContext {
    private val ref = parent?.let { WeakReference(it) }
    public val parent: Frame? get() = ref?.get()
}
