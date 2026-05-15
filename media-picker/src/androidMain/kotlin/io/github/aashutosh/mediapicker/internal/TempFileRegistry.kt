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
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Tracks every camera-capture temp file produced by the engine so they can be cleaned
 * up on [detach] (composable leaves composition) and on next app start (covers
 * crash-before-cleanup).
 */
internal class TempFileRegistry(private val context: Context) {

    private val dir: File = File(context.cacheDir, CAPTURE_DIR_NAME).apply { mkdirs() }
    private val tracked = CopyOnWriteArraySet<File>()

    fun newCaptureFile(extension: String): Pair<File, Uri> {
        val file = File.createTempFile(CAPTURE_PREFIX, ".$extension", dir)
        tracked += file
        val authority = "${context.packageName}.mediakit.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        return file to uri
    }

    /** Drop tracked files on detach. */
    fun cleanup() {
        tracked.forEach { runCatching { it.delete() } }
        tracked.clear()
    }

    /** Sweep stragglers from previous sessions. Safe to call at app start. */
    fun sweepStaleSync() {
        dir.listFiles()?.forEach { runCatching { it.delete() } }
    }

    private companion object {
        const val CAPTURE_DIR_NAME = "media_picker"
        const val CAPTURE_PREFIX = "capture_"
    }
}
