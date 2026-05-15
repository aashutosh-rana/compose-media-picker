/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(io.github.aashutosh.mediapicker.InternalMediaPickerApi::class)

package io.github.aashutosh.mediapicker.internal

import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jcodec.api.awt.AWTSequenceEncoder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.coroutines.resume

/**
 * Note on the spec exception: the project's stated non-goals say "never build a custom
 * camera UI". This desktop path is a deliberate carve-out — desktop OSes don't have a
 * system "camera app" the way Android and iOS do, so the only way to honor
 * `captureImage()` here is to host the preview ourselves. The implementation stays
 * deliberately small (one Swing window, sarxos `Webcam.getDefault()`, no recording
 * pipeline) so the cost in dependency weight and surface area is contained.
 *
 * On platforms where `Webcam.getDefault()` returns `null` (no camera) or `open()` fails
 * (driver mismatch — common on first-gen Apple Silicon), this returns `null` and the
 * engine surfaces [io.github.aashutosh.mediapicker.MediaPickerResult.Unsupported] to the
 * caller, matching the behavior of every other "no camera available" path in the library.
 */
private val nativeDriverInstalled = AtomicBoolean(false)

/** Replace sarxos's BridJ-based default driver with eduramiba's JNA driver — once per JVM. */
private fun ensureNativeDriver() {
    if (nativeDriverInstalled.compareAndSet(false, true)) {
        runCatching { Webcam.setDriver(NativeDriver()) }
    }
}

internal suspend fun captureStillFromDesktopWebcam(): File? = withContext(Dispatchers.IO) {
    ensureNativeDriver()
    val webcam: Webcam = Webcam.getDefault() ?: return@withContext null
    if (!webcam.open()) return@withContext null

    val captured = suspendCancellableCoroutine<File?> { cont ->
        SwingUtilities.invokeLater {
            CameraDialog(webcam) { file -> if (cont.isActive) cont.resume(file) }.show()
        }
    }
    runCatching { if (webcam.isOpen) webcam.close() }
    captured
}

internal suspend fun captureVideoFromDesktopWebcam(): File? = withContext(Dispatchers.IO) {
    ensureNativeDriver()
    val webcam: Webcam = Webcam.getDefault() ?: return@withContext null
    if (!webcam.open()) return@withContext null

    val captured = suspendCancellableCoroutine<File?> { cont ->
        SwingUtilities.invokeLater {
            VideoCaptureDialog(webcam) { file -> if (cont.isActive) cont.resume(file) }.show()
        }
    }
    runCatching { if (webcam.isOpen) webcam.close() }
    captured
}

/**
 * Minimal preview window. Repaints the latest frame from [webcam] every 33ms (~30fps) into a
 * JPanel; "Capture" encodes the current frame as a JPEG temp file and resolves; "Cancel"
 * (and the window close button) resolve `null`.
 */
private class CameraDialog(
    private val webcam: Webcam,
    private val onResult: (File?) -> Unit,
) {
    private val frame = JFrame("Take Photo")
    private var resolved = false

    fun show() {
        val previewSize = webcam.viewSize ?: Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)

        val previewPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                webcam.image?.let { img -> g.drawImage(img, 0, 0, width, height, null) }
            }
        }
        previewPanel.preferredSize = previewSize

        val captureBtn = JButton("Capture").apply {
            addActionListener { resolveWith(snapshot()) }
        }
        val cancelBtn = JButton("Cancel").apply {
            addActionListener { resolveWith(null) }
        }
        val controls = JPanel().apply {
            add(captureBtn)
            add(cancelBtn)
        }

        val timer = Timer(REPAINT_INTERVAL_MS) { previewPanel.repaint() }
        timer.start()

        frame.apply {
            layout = BorderLayout()
            add(previewPanel, BorderLayout.CENTER)
            add(controls, BorderLayout.SOUTH)
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent) {
                    timer.stop()
                    runCatching { if (webcam.isOpen) webcam.close() }
                    if (!resolved) {
                        resolved = true
                        onResult(null)
                    }
                }
            })
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun snapshot(): File? {
        val image = webcam.image ?: return null
        val out = Files.createTempFile("media_picker_capture_", ".jpg").toFile()
        ImageIO.write(image, "jpg", out)
        return out
    }

    private fun resolveWith(file: File?) {
        if (resolved) return
        resolved = true
        onResult(file)
        frame.dispose()
    }

    private companion object {
        const val DEFAULT_WIDTH = 640
        const val DEFAULT_HEIGHT = 480
        const val REPAINT_INTERVAL_MS = 33 // ~30fps
    }
}

/**
 * Recording counterpart of [CameraDialog]. Streams frames from [webcam] into a JCodec
 * sequence encoder at [FPS] frames/sec, producing an H.264 MP4 in the system temp dir.
 *
 * Buttons: Record (toggles to Stop while recording) + Cancel. The window's close button
 * is equivalent to Cancel and discards any partial recording.
 */
private class VideoCaptureDialog(
    private val webcam: Webcam,
    private val onResult: (File?) -> Unit,
) {
    private val frame = JFrame("Record Video")
    private var resolved = false
    private var recording = false
    private var encoder: AWTSequenceEncoder? = null
    private var outputFile: File? = null
    private var captureTimer: Timer? = null
    private var elapsedTimer: Timer? = null
    private var startNs: Long = 0
    private lateinit var recordBtn: JButton
    private lateinit var elapsedLabel: JLabel
    private lateinit var repaintTimer: Timer

    fun show() {
        val previewSize = webcam.viewSize ?: Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)

        val previewPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                webcam.image?.let { img -> g.drawImage(img, 0, 0, width, height, null) }
            }
        }
        previewPanel.preferredSize = previewSize

        recordBtn = JButton("Record").apply { addActionListener { toggleRecording() } }
        val cancelBtn = JButton("Cancel").apply { addActionListener { cancelAndDiscard() } }
        elapsedLabel = JLabel("00:00").apply { isVisible = false }

        val controls = JPanel(FlowLayout()).apply {
            add(recordBtn)
            add(cancelBtn)
            add(elapsedLabel)
        }

        repaintTimer = Timer(REPAINT_INTERVAL_MS) { previewPanel.repaint() }
        repaintTimer.start()

        frame.apply {
            layout = BorderLayout()
            add(previewPanel, BorderLayout.CENTER)
            add(controls, BorderLayout.SOUTH)
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent) {
                    repaintTimer.stop()
                    discardRecording()
                    runCatching { if (webcam.isOpen) webcam.close() }
                    if (!resolved) {
                        resolved = true
                        onResult(null)
                    }
                }
            })
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun toggleRecording() {
        if (!recording) startRecording() else stopRecordingAndResolve()
    }

    private fun startRecording() {
        val file = Files.createTempFile("media_picker_video_", ".mp4").toFile()
        val enc = runCatching { AWTSequenceEncoder.createSequenceEncoder(file, FPS) }
            .getOrNull() ?: return resolveWith(null)

        outputFile = file
        encoder = enc
        recording = true
        startNs = System.nanoTime()
        recordBtn.text = "Stop"
        elapsedLabel.isVisible = true

        captureTimer = Timer(CAPTURE_INTERVAL_MS) {
            val frameImage: BufferedImage? = webcam.image
            if (frameImage != null) {
                runCatching { encoder?.encodeImage(frameImage) }
            }
        }.also { it.start() }

        elapsedTimer = Timer(ELAPSED_TICK_MS) { updateElapsed() }.also { it.start() }
    }

    private fun stopRecordingAndResolve() {
        val file = stopRecordingInternal() ?: return resolveWith(null)
        resolveWith(file)
    }

    private fun cancelAndDiscard() {
        discardRecording()
        resolveWith(null)
    }

    private fun discardRecording() {
        val file = stopRecordingInternal()
        file?.delete()
    }

    private fun stopRecordingInternal(): File? {
        if (!recording) return null
        recording = false
        captureTimer?.stop()
        captureTimer = null
        elapsedTimer?.stop()
        elapsedTimer = null
        runCatching { encoder?.finish() }
        encoder = null
        return outputFile.also { outputFile = null }
    }

    private fun updateElapsed() {
        val secs = ((System.nanoTime() - startNs) / NS_PER_SEC).toInt()
        elapsedLabel.text = "${secs / SECS_PER_MIN}:${(secs % SECS_PER_MIN).toString().padStart(2, '0')}"
    }

    private fun resolveWith(file: File?) {
        if (resolved) return
        resolved = true
        onResult(file)
        frame.dispose()
    }

    private companion object {
        const val DEFAULT_WIDTH = 640
        const val DEFAULT_HEIGHT = 480
        const val REPAINT_INTERVAL_MS = 33
        const val FPS = 15
        const val CAPTURE_INTERVAL_MS = 1000 / FPS
        const val ELAPSED_TICK_MS = 250
        const val NS_PER_SEC = 1_000_000_000L
        const val SECS_PER_MIN = 60
    }
}
