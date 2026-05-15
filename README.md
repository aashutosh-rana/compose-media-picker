# compose-media-picker

A unified media-picker for Compose Multiplatform. One commonMain call site, every
target. No `expect/actual` leaks into your code, no custom camera UI, no `CAMERA`
permission to declare.

| Target | Pick image / video / file | System camera |
|---|---|---|
| Android | ✅ Photo Picker / SAF | ✅ `ACTION_IMAGE_CAPTURE` / `ACTION_VIDEO_CAPTURE` |
| iOS | ✅ PHPicker / Document Picker | ✅ `UIImagePickerController(.camera)` |
| Desktop (JVM) | ✅ `FileDialog` | ⚠️ `Unsupported` (no system camera) |
| Web (wasmJs) | ✅ `<input type="file">` | ✅ on mobile-web via `capture` attribute |

## Install

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.aashutosh-rana:compose-media-picker:0.1.0")
}
```

Repo: <https://github.com/aashutosh-rana/compose-media-picker>.

## Quickstart

```kotlin
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeMediaPicker(AndroidPlatformContext(this))   // once
        setContent {
            val picker = rememberMediaPicker()
            val scope = rememberCoroutineScope()
            Button(onClick = {
                scope.launch {
                    when (val r = picker.captureImage()) {
                        is MediaPickerResult.Success -> handle(r.data)
                        else -> Unit
                    }
                }
            }) { Text("Take photo") }
        }
    }
}
```

The library declares its own `FileProvider` under `${applicationId}.mediakit.fileprovider`
via manifest merging — you don't need to add anything to your `AndroidManifest.xml`.
You also don't need the `CAMERA` permission, because the system camera app owns it.

iOS, Desktop, and Web have one-line initialisation in the same shape:

```kotlin
// iOS — pass the root UIViewController
initializeMediaPicker(IosPlatformContext(rootViewController))

// Desktop — pass the parent Frame (optional)
initializeMediaPicker(DesktopPlatformContext(parent = window))

// Web — no handle required
initializeMediaPicker(WebPlatformContext())
```

## Result model

Every call returns a `MediaPickerResult<T>`:

```kotlin
sealed interface MediaPickerResult<out T> {
    data class Success<T>(val data: T) : MediaPickerResult<T>
    data object Cancelled : MediaPickerResult<Nothing>
    data class PermissionDenied(val permission: String, val permanentlyDenied: Boolean) : MediaPickerResult<Nothing>
    data object Unsupported : MediaPickerResult<Nothing>
    data class Error(val throwable: Throwable) : MediaPickerResult<Nothing>
}
```

`Unsupported` lets you feature-detect cleanly:

```kotlin
when (val r = picker.captureImage()) {
    is Success -> show(r.data)
    Unsupported -> showMessage("No camera available")
    Cancelled -> Unit
    is Error -> showError(r.throwable)
    is PermissionDenied -> showPermissionDialog()
}
```

## Streaming reads

`MediaFile` is a lightweight reference — bytes are not loaded until you ask:

```kotlin
val file: MediaFile = (result as Success).data
file.source().use { source ->                       // streaming, off-main, cancellable
    // …
}

scope.launch {
    file.readProgress().collect { p -> updateBar(p) }
}
```

## Sample app

A single Compose Multiplatform sample app — `:samples:composeApp` — runs on all four
targets from one shared `commonMain` Compose UI:

```bash
# Android (with emulator running)
./gradlew :samples:composeApp:installDebug
adb shell am start -n io.github.aashutosh.mediapicker.sample/.MainActivity

# Desktop
./gradlew :samples:composeApp:run

# Web (browser dev server on http://localhost:8080)
./gradlew :samples:composeApp:wasmJsBrowserDevelopmentRun

# iOS — see samples/iosApp/README.md for the xcodegen + Xcode workflow
```

The iOS host lives in `samples/iosApp/` as a SwiftUI shell that embeds the
`ComposeApp.framework` produced by `:samples:composeApp`. The Compose UI itself is the
same Kotlin code that runs on Android, Desktop, and Web.

## Non-goals

- Custom camera UI / preview — always delegate to the system camera app
- Image cropping or editing
- Cloud upload helpers
- Background uploads

Apps that need any of the above should reach for CameraX / dedicated libraries directly.

## License

[Apache 2.0](LICENSE).
