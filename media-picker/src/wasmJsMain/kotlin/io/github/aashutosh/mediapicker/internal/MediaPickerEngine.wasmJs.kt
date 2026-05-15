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

import io.github.aashutosh.mediapicker.CameraCaptureConfig
import io.github.aashutosh.mediapicker.FilePickerConfig
import io.github.aashutosh.mediapicker.ImagePickerConfig
import io.github.aashutosh.mediapicker.InternalMediaPickerApi
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.MultiImagePickerConfig
import io.github.aashutosh.mediapicker.PlatformContext
import io.github.aashutosh.mediapicker.VideoCaptureConfig
import io.github.aashutosh.mediapicker.VideoPickerConfig
import io.github.aashutosh.mediapicker.WebPlatformContext
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileList
import kotlin.coroutines.resume

@OptIn(InternalMediaPickerApi::class)
public actual class MediaPickerEngine public actual constructor(context: PlatformContext) {

    init {
        require(context is WebPlatformContext) {
            "Web target requires WebPlatformContext, got ${context::class.simpleName}"
        }
    }

    public actual fun attach() {}
    public actual fun detach() {}

    public actual suspend fun pickImage(config: ImagePickerConfig): MediaPickerResult<MediaFile> =
        showInput(accept = "image/*", multiple = false, capture = null)?.firstOrNull()
            ?.let { MediaPickerResult.Success(WebMediaFile(it)) }
            ?: MediaPickerResult.Cancelled

    public actual suspend fun pickImages(config: MultiImagePickerConfig): MediaPickerResult<List<MediaFile>> {
        val files = showInput(accept = "image/*", multiple = true, capture = null)
            ?: return MediaPickerResult.Cancelled
        val capped = files.take(config.maxItems).map(::WebMediaFile)
        return if (capped.isEmpty()) MediaPickerResult.Cancelled else MediaPickerResult.Success(capped)
    }

    public actual suspend fun pickVideo(config: VideoPickerConfig): MediaPickerResult<MediaFile> {
        val file = showInput(accept = "video/*", multiple = false, capture = null)?.firstOrNull()
            ?: return MediaPickerResult.Cancelled
        if (config.maxSizeBytes != null && file.size.toDouble().toLong() > config.maxSizeBytes) {
            return MediaPickerResult.Error(IllegalStateException("Video exceeds maxSizeBytes"))
        }
        return MediaPickerResult.Success(WebMediaFile(file))
    }

    public actual suspend fun pickFile(config: FilePickerConfig): MediaPickerResult<MediaFile> {
        val files = showInput(
            accept = config.mimeTypes.joinToString(","),
            multiple = config.allowMultiple,
            capture = null,
        ) ?: return MediaPickerResult.Cancelled
        val first = files.firstOrNull() ?: return MediaPickerResult.Cancelled
        return MediaPickerResult.Success(WebMediaFile(first))
    }

    public actual suspend fun captureImage(config: CameraCaptureConfig): MediaPickerResult<MediaFile> {
        val facing = if (config.preferFrontCamera) "user" else "environment"
        if (isMobileBrowser()) {
            // Mobile: <input capture> hands the request to the OS camera app. Cheap, no permissions
            // beyond the file picker grant, no custom UI.
            val files = showInput(accept = "image/*", multiple = false, capture = facing)
                ?: return MediaPickerResult.Cancelled
            val first = files.firstOrNull() ?: return MediaPickerResult.Cancelled
            return MediaPickerResult.Success(WebMediaFile(first))
        }
        // Desktop: <input capture> is silently ignored by browsers, so getUserMedia is the only
        // way to actually open the webcam. This is a documented carve-out from the project's
        // "no custom camera UI" non-goal — see DesktopCamera.kt for the same rationale on JVM.
        val file = openWebcamOverlayCapture(facing).await<JsAny?>()

        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        val picked = file?.unsafeCast<File>() ?: return MediaPickerResult.Cancelled
        return MediaPickerResult.Success(WebMediaFile(picked))
    }

    public actual suspend fun captureVideo(config: VideoCaptureConfig): MediaPickerResult<MediaFile> {
        if (isMobileBrowser()) {
            val files = showInput(accept = "video/*", multiple = false, capture = "environment")
                ?: return MediaPickerResult.Cancelled
            val first = files.firstOrNull() ?: return MediaPickerResult.Cancelled
            return MediaPickerResult.Success(WebMediaFile(first))
        }
        // Desktop browser: drive a MediaRecorder against the live MediaStream. Same UX as the JVM
        // VideoCaptureDialog (Record / Stop / Cancel + elapsed counter) but rendered via DOM.
        val file = openWebcamVideoCapture(facing = "environment").await<JsAny?>()

        @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
        val picked = file?.unsafeCast<File>() ?: return MediaPickerResult.Cancelled
        return MediaPickerResult.Success(WebMediaFile(picked))
    }

    private fun isMobileBrowser(): Boolean {
        val ua = window.navigator.userAgent.lowercase()
        return MOBILE_UA_MARKERS.any { ua.contains(it) }
    }

    // -- helpers ----------------------------------------------------------------------

    private suspend fun showInput(accept: String, multiple: Boolean, capture: String?): List<File>? = suspendCancellableCoroutine { cont ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = accept
        input.multiple = multiple
        if (capture != null) input.setAttribute("capture", capture)
        input.style.display = "none"

        var settled = false
        fun finishWith(value: List<File>?) {
            if (settled) return
            settled = true
            input.remove()
            if (cont.isActive) cont.resume(value)
        }

        input.onchange = { _: Event ->
            finishWith(input.files?.toFileList())
        }
        // Best-effort cancel detection — when the dialog closes without a selection.
        input.oncancel = { _: Event -> finishWith(null) }

        document.body?.appendChild(input)
        input.click()
        cont.invokeOnCancellation { finishWith(null) }
    }
}

private fun FileList.toFileList(): List<File> {
    val out = ArrayList<File>(length)
    for (i in 0 until length) {
        val f = item(i)
        if (f != null) out += f
    }
    return out
}

private val MOBILE_UA_MARKERS = listOf("android", "iphone", "ipad", "ipod", "mobile")

/**
 * Builds a fixed-position overlay with a live `<video>` preview backed by
 * `getUserMedia()`, a Capture button, and a Cancel button. Resolves with the captured
 * `File` (image/jpeg) or `null` on cancel / `getUserMedia` rejection.
 *
 * The whole interaction lives in a single JS function for two reasons:
 * 1. wasmJs Kotlin can't cheaply allocate DOM elements + canvases + blobs + futures over
 *    a multi-call interop boundary, so doing it in one JS body keeps the round-trips down.
 * 2. The browser owns the camera permission prompt — keeping the request collocated with
 *    the prompt UI avoids the well-known "permission prompt dismissed before getUserMedia
 *    is awaited" race.
 */
@JsFun(
    """
    async (facing) => new Promise(async (resolve) => {
        let stream;
        try {
            stream = await navigator.mediaDevices.getUserMedia({video: {facingMode: facing}});
        } catch (e) {
            resolve(null);
            return;
        }
        const overlay = document.createElement('div');
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.85);' +
            'display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:2147483647';
        const video = document.createElement('video');
        video.autoplay = true; video.playsInline = true; video.muted = true; video.srcObject = stream;
        video.style.cssText = 'max-width:90vw;max-height:70vh;background:black;border-radius:8px';
        const controls = document.createElement('div');
        controls.style.cssText = 'margin-top:16px;display:flex;gap:12px';
        const captureBtn = document.createElement('button');
        captureBtn.textContent = 'Capture';
        const cancelBtn = document.createElement('button');
        cancelBtn.textContent = 'Cancel';
        const btnStyle = 'padding:12px 24px;font-size:16px;border:none;border-radius:6px;cursor:pointer';
        captureBtn.style.cssText = btnStyle + ';background:#1976d2;color:#fff';
        cancelBtn.style.cssText = btnStyle + ';background:#555;color:#fff';
        controls.appendChild(captureBtn);
        controls.appendChild(cancelBtn);
        overlay.appendChild(video);
        overlay.appendChild(controls);
        document.body.appendChild(overlay);
        let settled = false;
        const cleanup = () => {
            stream.getTracks().forEach(t => t.stop());
            overlay.remove();
        };
        captureBtn.onclick = () => {
            if (settled) return;
            settled = true;
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            canvas.getContext('2d').drawImage(video, 0, 0);
            canvas.toBlob((blob) => {
                cleanup();
                if (!blob) { resolve(null); return; }
                resolve(new File([blob], 'capture_' + Date.now() + '.jpg', {type: 'image/jpeg'}));
            }, 'image/jpeg', 0.9);
        };
        cancelBtn.onclick = () => {
            if (settled) return;
            settled = true;
            cleanup();
            resolve(null);
        };
    })
    """,
)
private external fun openWebcamOverlayCapture(facing: String): kotlin.js.Promise<JsAny?>

/**
 * MediaRecorder-driven video capture on desktop browsers. Reuses the same overlay pattern
 * as the still-image flow, but the Capture button toggles into Record/Stop and the
 * resulting Blob is wrapped as a `File` with a `.webm` extension (the format every modern
 * browser's MediaRecorder produces by default and that every modern browser plays back).
 */
@JsFun(
    """
    async (facing) => new Promise(async (resolve) => {
        let stream;
        try {
            stream = await navigator.mediaDevices.getUserMedia({video: {facingMode: facing}, audio: true});
        } catch (e) { resolve(null); return; }

        const overlay = document.createElement('div');
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.85);' +
            'display:flex;flex-direction:column;align-items:center;justify-content:center;z-index:2147483647';
        const video = document.createElement('video');
        video.autoplay = true; video.playsInline = true; video.muted = true; video.srcObject = stream;
        video.style.cssText = 'max-width:90vw;max-height:65vh;background:black;border-radius:8px';

        const controls = document.createElement('div');
        controls.style.cssText = 'margin-top:16px;display:flex;gap:12px;align-items:center;color:#fff;font:14px sans-serif';
        const recordBtn = document.createElement('button');
        recordBtn.textContent = 'Record';
        const cancelBtn = document.createElement('button');
        cancelBtn.textContent = 'Cancel';
        const elapsed = document.createElement('span');
        elapsed.textContent = '';
        const btnStyle = 'padding:12px 24px;font-size:16px;border:none;border-radius:6px;cursor:pointer';
        recordBtn.style.cssText = btnStyle + ';background:#d32f2f;color:#fff';
        cancelBtn.style.cssText = btnStyle + ';background:#555;color:#fff';
        controls.appendChild(recordBtn);
        controls.appendChild(cancelBtn);
        controls.appendChild(elapsed);
        overlay.appendChild(video);
        overlay.appendChild(controls);
        document.body.appendChild(overlay);

        // Pick the first supported mime type; fall back to default if MediaRecorder rejects all.
        const candidates = ['video/webm;codecs=vp9', 'video/webm;codecs=vp8', 'video/webm', 'video/mp4'];
        let mimeType = '';
        for (const c of candidates) {
            if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported && MediaRecorder.isTypeSupported(c)) {
                mimeType = c;
                break;
            }
        }
        let recorder;
        try {
            recorder = mimeType ? new MediaRecorder(stream, {mimeType}) : new MediaRecorder(stream);
        } catch (e) {
            stream.getTracks().forEach(t => t.stop());
            overlay.remove();
            resolve(null);
            return;
        }

        const chunks = [];
        recorder.ondataavailable = (e) => { if (e.data && e.data.size > 0) chunks.push(e.data); };

        let recording = false;
        let cancelled = false;
        let settled = false;
        let elapsedTimer = null;
        let startedAt = 0;
        const safeResolve = (value) => { if (!settled) { settled = true; resolve(value); } };

        recorder.onstop = () => {
            stream.getTracks().forEach(t => t.stop());
            overlay.remove();
            if (cancelled || chunks.length === 0) {
                safeResolve(null);
                return;
            }
            const blob = new Blob(chunks, {type: recorder.mimeType || 'video/webm'});
            const ext = (blob.type.indexOf('mp4') >= 0) ? 'mp4' : 'webm';
            safeResolve(new File([blob], 'capture_' + Date.now() + '.' + ext, {type: blob.type || 'video/webm'}));
        };

        const updateElapsed = () => {
            const secs = Math.floor((Date.now() - startedAt) / 1000);
            const mm = Math.floor(secs / 60);
            const ss = String(secs % 60).padStart(2, '0');
            elapsed.textContent = mm + ':' + ss;
        };

        recordBtn.onclick = () => {
            if (!recording) {
                recording = true;
                recordBtn.textContent = 'Stop';
                recordBtn.style.background = '#555';
                startedAt = Date.now();
                elapsedTimer = setInterval(updateElapsed, 250);
                recorder.start();
            } else {
                if (elapsedTimer) clearInterval(elapsedTimer);
                recording = false;
                recorder.stop();
            }
        };
        cancelBtn.onclick = () => {
            cancelled = true;
            if (elapsedTimer) clearInterval(elapsedTimer);
            if (recording) {
                recording = false;
                recorder.stop(); // triggers onstop, which sees cancelled=true and resolves null
            } else {
                stream.getTracks().forEach(t => t.stop());
                overlay.remove();
                safeResolve(null);
            }
        };
    })
    """,
)
private external fun openWebcamVideoCapture(facing: String): kotlin.js.Promise<JsAny?>
