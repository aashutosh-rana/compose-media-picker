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
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

internal data class IosPlatformContext(val viewController: UIViewController) : InternalPlatformContext

@Composable
internal actual fun discoverPlatformContext(): InternalPlatformContext? = remember {
    topViewController()?.let(::IosPlatformContext)
}

/**
 * Locates the topmost view controller suitable for modal presentation. Walks the scene
 * → window → root VC → `presentedViewController` chain. We intentionally do NOT filter
 * by `activationState` because at the time the first composition runs the scene is often
 * still `foregroundInactive`; by the time the user actually triggers a picker the scene
 * has become active, but auto-discovery has already cached its result.
 */
private fun topViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
    for (s in scenes) {
        val scene = s as? UIWindowScene ?: continue
        val window = scene.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            ?: scene.windows.firstOrNull() as? UIWindow
            ?: continue
        var vc = window.rootViewController ?: continue
        while (true) {
            val presented = vc.presentedViewController ?: return vc
            vc = presented
        }
    }
    // Fallback: pre-scene API still works on iOS 13+ when running outside a scene context
    // (rare in modern apps; included for robustness).
    @Suppress("DEPRECATION")
    val legacyWindow = UIApplication.sharedApplication.keyWindow ?: return null
    var vc = legacyWindow.rootViewController ?: return null
    while (true) {
        val presented = vc.presentedViewController ?: return vc
        vc = presented
    }
}
