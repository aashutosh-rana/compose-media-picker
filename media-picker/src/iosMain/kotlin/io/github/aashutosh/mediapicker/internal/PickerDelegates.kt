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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/**
 * PHPicker delegate that funnels selections into [onComplete] as a list of NSItemProvider
 * objects (left as `Any` here — the engine knows how to decode them).
 *
 * Held strongly by the engine for the lifetime of one pick call, then nulled.
 */
@OptIn(ExperimentalForeignApi::class)
internal class PhPickerDelegate(
    private val onComplete: (results: List<Any>) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        @Suppress("UNCHECKED_CAST")
        val results = didFinishPicking as List<PHPickerResult>
        picker.dismissViewControllerAnimated(true, completion = null)
        onComplete(results.map { it.itemProvider })
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class DocumentPickerDelegate(
    private val onPick: (urls: List<NSURL>) -> Unit,
    private val onCancel: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {

    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        @Suppress("UNCHECKED_CAST")
        onPick(didPickDocumentsAtURLs as List<NSURL>)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancel()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class CameraDelegate(
    private val onComplete: (info: Map<Any?, *>?, cancelled: Boolean) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onComplete(didFinishPickingMediaWithInfo, false)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onComplete(null, true)
    }
}
