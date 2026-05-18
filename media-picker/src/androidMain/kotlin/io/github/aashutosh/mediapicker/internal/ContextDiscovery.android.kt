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

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal data class AndroidPlatformContext(val activity: ComponentActivity) : InternalPlatformContext

@Composable
internal actual fun discoverPlatformContext(): InternalPlatformContext? {
    val context = LocalContext.current
    return remember(context) {
        context.findActivity()?.let(::AndroidPlatformContext)
    }
}

/**
 * Walks `ContextWrapper.baseContext` until it finds a [ComponentActivity]. Same trick
 * `androidx.activity.compose` uses to recover the hosting activity from a Compose
 * `Context`.
 */
private fun Context.findActivity(): ComponentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
