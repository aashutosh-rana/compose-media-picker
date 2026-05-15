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

import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

/**
 * Android [PlatformContext]. Hold a [ComponentActivity] weakly so the singleton
 * registration in [initializeMediaPicker] can't leak it.
 *
 * Usage:
 * ```
 * class MyActivity : ComponentActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         initializeMediaPicker(AndroidPlatformContext(this))
 *         setContent { /* ... rememberMediaPicker() ... */ }
 *     }
 * }
 * ```
 */
public class AndroidPlatformContext(activity: ComponentActivity) : PlatformContext {
    private val activityRef: WeakReference<ComponentActivity> = WeakReference(activity)

    public val activity: ComponentActivity
        get() = checkNotNull(activityRef.get()) {
            "AndroidPlatformContext outlived its ComponentActivity. Re-initialize the picker."
        }
}
