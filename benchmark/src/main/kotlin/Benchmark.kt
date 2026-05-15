/*
 * Copyright 2026 Aashutosh Kumar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.aashutosh.mediapicker.benchmark

import io.github.aashutosh.mediapicker.CompressionConfig
import io.github.aashutosh.mediapicker.internal.image.platformCompressor
import io.github.aashutosh.mediapicker.internal.image.platformThumbnailFactory
import kotlinx.coroutines.runBlocking
import kotlin.system.measureNanoTime

/**
 * Micro-benchmark for the desktop image ops. Synthesizes a 4K JPEG, then measures
 * compression and thumbnailing throughput.
 *
 * Run via `./gradlew :benchmark:run`. CI captures the output as an artifact and compares
 * against `benchmark/baseline.json` (≥ 20% regression fails the build).
 */
@OptIn(io.github.aashutosh.mediapicker.InternalMediaPickerApi::class)
fun main() = runBlocking {
    val source = generateSyntheticJpeg()
    println("source: ${source.size / 1024} KiB")

    warmup {
        platformCompressor().compress(source, CompressionConfig(maxDimension = 1920, qualityPercent = 85))
    }

    val compressNs = measureNanoTime {
        repeat(ITERATIONS) {
            platformCompressor().compress(source, CompressionConfig(maxDimension = 1920, qualityPercent = 85))
        }
    }
    val thumbNs = measureNanoTime {
        repeat(ITERATIONS) {
            platformThumbnailFactory().create(source, 256)
        }
    }

    val compressMs = compressNs.toDouble() / ITERATIONS / 1_000_000
    val thumbMs = thumbNs.toDouble() / ITERATIONS / 1_000_000
    println("compress 4K → 1080p: %.2f ms/op".format(compressMs))
    println("thumbnail   256px:    %.2f ms/op".format(thumbMs))
}

private const val ITERATIONS = 20

private suspend inline fun warmup(block: suspend () -> Unit) {
    repeat(WARMUP_ITERATIONS) { block() }
}

private const val WARMUP_ITERATIONS = 5

/**
 * Generates a 3840x2160 JPEG by writing a TYPE_INT_RGB BufferedImage with a gradient.
 * Pure-JDK, no external assets.
 */
private fun generateSyntheticJpeg(): ByteArray {
    val width = 3840
    val height = 2160
    val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    @Suppress("MagicNumber")
    g.paint = java.awt.GradientPaint(0f, 0f, java.awt.Color(40, 80, 120), width.toFloat(), height.toFloat(), java.awt.Color(220, 200, 90))
    g.fillRect(0, 0, width, height)
    g.dispose()

    val out = java.io.ByteArrayOutputStream()
    javax.imageio.ImageIO.write(image, "jpeg", out)
    return out.toByteArray()
}

