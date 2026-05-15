/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aashutosh.mediapicker.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.khronos.webgl.Int8Array
import org.khronos.webgl.set

@Composable
actual fun rememberMediaOpener(): MediaOpener = remember { WebMediaOpener }

private object WebMediaOpener : MediaOpener {
    override suspend fun open(file: MediaFile) {
        val bytes = withContext(Dispatchers.Default) { file.loadBytes() }
        // `ByteArray` isn't a valid @JsFun parameter type on wasmJs — go through `Int8Array`,
        // which is an external ArrayBufferView the Blob constructor accepts directly.
        val view = Int8Array(bytes.size)
        for (i in bytes.indices) view[i] = bytes[i]
        showMediaOverlay(view, file.mimeType ?: "application/octet-stream", file.name)
    }
}

/**
 * Builds an inline preview overlay: `<video controls>` for video, `<audio controls>` for
 * audio, `<img>` for images, and a `window.open()` blob URL fallback for everything else.
 * The overlay has a single Close button that revokes the blob URL and dismisses.
 *
 * The whole DOM is built in JS for the same reason the camera overlay is — minimizing the
 * wasm↔JS interop hops while keeping all UI ownership on the JS side.
 */
@JsFun(
    """
    (bytes, mime, name) => {
        const blob = new Blob([bytes], {type: mime || 'application/octet-stream'});
        const url = URL.createObjectURL(blob);
        const overlay = document.createElement('div');
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.92);' +
            'display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:2147483647';

        let element;
        if ((mime || '').startsWith('video/')) {
            element = document.createElement('video');
            element.controls = true; element.autoplay = true; element.src = url;
            element.style.cssText = 'max-width:90vw;max-height:80vh;background:black;border-radius:8px';
        } else if ((mime || '').startsWith('audio/')) {
            element = document.createElement('audio');
            element.controls = true; element.autoplay = true; element.src = url;
        } else if ((mime || '').startsWith('image/')) {
            element = document.createElement('img');
            element.src = url;
            element.style.cssText = 'max-width:90vw;max-height:80vh;border-radius:8px';
        } else {
            // Unknown type: open in a new tab. The user can dismiss by closing that tab.
            window.open(url, '_blank');
            URL.revokeObjectURL(url);
            return;
        }

        const close = document.createElement('button');
        close.textContent = 'Close';
        close.style.cssText = 'margin-top:16px;padding:12px 24px;font-size:16px;background:#555;' +
            'color:#fff;border:none;border-radius:6px;cursor:pointer';
        close.onclick = () => { URL.revokeObjectURL(url); overlay.remove(); };

        const label = document.createElement('div');
        label.textContent = name || '';
        label.style.cssText = 'color:#fff;margin-bottom:8px;font:14px sans-serif';

        overlay.appendChild(label);
        overlay.appendChild(element);
        overlay.appendChild(close);
        document.body.appendChild(overlay);
    }
    """,
)
private external fun showMediaOverlay(bytes: Int8Array, mime: String, name: String)
