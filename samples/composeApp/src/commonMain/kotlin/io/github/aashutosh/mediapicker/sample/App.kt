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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aashutosh.mediapicker.FilePickerConfig
import io.github.aashutosh.mediapicker.MediaFile
import io.github.aashutosh.mediapicker.MediaPickerResult
import io.github.aashutosh.mediapicker.rememberMediaPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The shared Compose UI that powers every platform sample. Each platform entry point
 * ([MainActivity] on Android, `MainViewController()` on iOS, the desktop `main()`, the
 * wasmJs `main()`) calls [initializeMediaPicker] with its respective `PlatformContext` and
 * then hosts this composable.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            SampleScreen()
        }
    }
}

@Composable
private fun SampleScreen() {
    val picker = rememberMediaPicker()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Ready.") }
    val picked = remember { mutableStateListOf<MediaFile>() }

    fun handle(result: MediaPickerResult<*>) {
        status = describe(result)
        if (result is MediaPickerResult.Success<*>) {
            picked.clear()
            when (val data = result.data) {
                is MediaFile -> picked += data
                is List<*> -> data.filterIsInstance<MediaFile>().forEach { picked += it }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Button(onClick = { scope.launch { handle(picker.pickImage()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick image")
            }
            Button(onClick = { scope.launch { handle(picker.pickImages()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick multiple images")
            }
            Button(onClick = { scope.launch { handle(picker.pickVideo()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick video")
            }
            Button(
                onClick = {
                    scope.launch { handle(picker.pickFile(FilePickerConfig(mimeTypes = listOf("audio/*")))) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pick audio") }
            Button(onClick = { scope.launch { handle(picker.pickFile()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Pick any file")
            }
            Button(onClick = { scope.launch { handle(picker.captureImage()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Take photo (system camera)")
            }
            Button(onClick = { scope.launch { handle(picker.captureVideo()) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Record video (system camera)")
            }
        }

        HorizontalDivider()
        Text(
            "Picked (${picked.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(picked) { file -> PickedItem(file) }
        }
    }
}

@Composable
private fun PickedItem(file: MediaFile) {
    val thumb by produceState<ImageBitmap?>(initialValue = null, key1 = file) {
        value = withContext(Dispatchers.Default) {
            file.thumbnail(maxDimension = THUMB_MAX_DIM)?.let { bytes ->
                decodeImageBitmap(bytes)
            }
        }
    }
    val opener = rememberMediaOpener()
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThumbnailOrGlyph(thumb, file.mimeType)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Text(secondaryLine(file), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { scope.launch { opener.open(file) } }) {
                Text(actionLabel(file.mimeType))
            }
        }
    }
}

private fun actionLabel(mimeType: String?): String = when {
    mimeType == null -> "Open"
    mimeType.startsWith("video/") || mimeType.startsWith("audio/") -> "Play"
    else -> "Open"
}

@Composable
private fun ThumbnailOrGlyph(thumb: ImageBitmap?, mimeType: String?) {
    Box(
        modifier = Modifier
            .size(THUMB_SIZE_DP.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(THUMB_PLACEHOLDER_COLOR.toInt())),
        contentAlignment = Alignment.Center,
    ) {
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(text = glyphFor(mimeType), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun glyphFor(mimeType: String?): String = when {
    mimeType == null -> "?"
    mimeType.startsWith("audio/") -> "♪"
    mimeType.startsWith("video/") -> "▶"
    mimeType.startsWith("image/") -> "■"
    else -> "•"
}

private fun secondaryLine(file: MediaFile): String = buildString {
    append(file.mimeType ?: "unknown")
    append(" • ")
    append(formatBytes(file.sizeBytes))
    file.durationMs?.let { append(" • ${it / MS_PER_SECOND}s") }
    if (file.width != null && file.height != null) {
        append(" • ${file.width}×${file.height}")
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "?"
    if (bytes < ONE_KIB) return "$bytes B"
    if (bytes < ONE_MIB) {
        val v = bytes.toDouble() / ONE_KIB
        return "${(v * TENTHS).toLong() / TENTHS_F} KB"
    }
    val v = bytes.toDouble() / ONE_MIB
    return "${(v * TENTHS).toLong() / TENTHS_F} MB"
}

private fun describe(result: MediaPickerResult<*>): String = when (result) {
    is MediaPickerResult.Success<*> -> "Picked successfully."
    MediaPickerResult.Cancelled -> "Cancelled."
    MediaPickerResult.Unsupported -> "Unsupported on this device."
    is MediaPickerResult.PermissionDenied -> "Permission denied: ${result.permission}"
    is MediaPickerResult.Error -> "Error: ${result.throwable.message}"
}

private const val THUMB_MAX_DIM = 256
private const val THUMB_SIZE_DP = 72
private const val THUMB_PLACEHOLDER_COLOR: Long = 0xFFEEEEEE
private const val ONE_KIB = 1024L
private const val ONE_MIB: Long = 1024L * 1024L
private const val MS_PER_SECOND = 1000L
private const val TENTHS = 10.0
private const val TENTHS_F = 10.0
