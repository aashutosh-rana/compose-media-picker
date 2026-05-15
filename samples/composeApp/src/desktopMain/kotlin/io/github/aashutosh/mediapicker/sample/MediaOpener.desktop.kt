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
import java.awt.Desktop
import java.nio.file.Files

@Composable
actual fun rememberMediaOpener(): MediaOpener = remember { DesktopMediaOpener() }

private class DesktopMediaOpener : MediaOpener {
    override suspend fun open(file: MediaFile) {
        withContext(Dispatchers.IO) {
            val ext = file.name.substringAfterLast('.', "bin").ifEmpty { "bin" }
            val out = Files.createTempFile("media_picker_open_", ".$ext").toFile()
            out.outputStream().use { it.write(file.loadBytes()) }
            if (Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.OPEN)
            ) {
                runCatching { Desktop.getDesktop().open(out) }
            }
        }
    }
}
