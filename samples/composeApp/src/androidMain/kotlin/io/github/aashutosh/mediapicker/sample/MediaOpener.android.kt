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

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import io.github.aashutosh.mediapicker.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberMediaOpener(): MediaOpener {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidMediaOpener(context) }
}

private class AndroidMediaOpener(private val context: Context) : MediaOpener {
    override suspend fun open(file: MediaFile) {
        val uri = withContext(Dispatchers.IO) {
            // The library's auto-merged FileProvider exposes <app cache>/media_picker/.
            val dir = File(context.cacheDir, "media_picker").apply { mkdirs() }
            val ext = file.name.substringAfterLast('.', "bin").ifEmpty { "bin" }
            val out = File(dir, "open_${System.currentTimeMillis()}.$ext")
            out.outputStream().use { it.write(file.loadBytes()) }
            FileProvider.getUriForFile(context, "${context.packageName}.mediakit.fileprovider", out)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, file.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
