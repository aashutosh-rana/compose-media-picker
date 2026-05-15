# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-15

### Added
- Initial public API: `MediaPicker`, `MediaPickerResult`, `MediaFile`, `rememberMediaPicker()`.
- Android engine using Activity Result APIs (Photo Picker, SAF, system camera).
- Auto-merged `FileProvider` so consumers add nothing to their manifest.
- iOS engine using `PHPickerViewController`, `UIDocumentPickerViewController`, and `UIImagePickerController(.camera)`.
- Desktop (JVM) engine: file picker via `java.awt.FileDialog`; webcam still + H.264 MP4 recording via sarxos + eduramiba native driver + JCodec.
- Web (wasmJs) engine: `<input type="file">` for picker; mobile-web camera via `capture`; desktop browser camera via `getUserMedia` + `MediaRecorder` overlay.
- In-library image compression, EXIF rotation, and thumbnail generation (Android, iOS, Desktop).
- Unified Compose Multiplatform sample app (`:samples:composeApp`) covering all 4 targets + Xcode host project under `samples/iosApp/`.
- Benchmark module measuring compression + thumbnail throughput.
- CI on GitHub Actions (Linux + macOS); Maven Central publishing pipeline via Vanniktech's plugin.
