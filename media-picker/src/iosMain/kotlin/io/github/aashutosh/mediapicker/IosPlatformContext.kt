/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package io.github.aashutosh.mediapicker

import platform.UIKit.UIViewController
import kotlin.native.ref.WeakReference

/**
 * iOS [PlatformContext]. Holds the presenting [UIViewController] weakly so the singleton
 * registration in [initializeMediaPicker] can't retain it.
 *
 * Pass the root controller (typically `ComposeUIViewController { … }`) at app start.
 */
public class IosPlatformContext(viewController: UIViewController) : PlatformContext {
    private val ref = WeakReference(viewController)

    public val viewController: UIViewController
        get() = checkNotNull(ref.get()) {
            "IosPlatformContext outlived its UIViewController. Re-initialize the picker."
        }
}
