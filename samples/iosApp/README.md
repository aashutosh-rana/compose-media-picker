# iOS Sample App

This is the SwiftUI host for the shared Compose UI defined in
`samples/composeApp/src/iosMain/.../MainViewController.kt`. The Compose layer is itself
built into a Kotlin/Native framework named **ComposeApp.framework** by the
`:samples:composeApp` Gradle module; the Swift code here is a thin
`UIViewControllerRepresentable` that hands the OS a controller built in Kotlin.

## First-time setup

Three steps, all one-liners:

```bash
# 1. Ensure xcode-select points at full Xcode (not Command Line Tools).
sudo xcode-select -s /Applications/Xcode.app/Contents/Developer

# 2. Install xcodegen if you don't already have it.
brew install xcodegen

# 3. Generate the .xcodeproj from project.yml.
cd samples/iosApp
xcodegen
```

You should now have `samples/iosApp/iosApp.xcodeproj`. Open it in Xcode 15+ and pick any
iOS 15+ simulator or device.

## What happens when you build

The project's `iosApp` target has a pre-build script that runs
`./gradlew :samples:composeApp:embedAndSignAppleFrameworkForXcode`. That task is provided
by Kotlin Multiplatform — it compiles the Kotlin sources for the simulator / device arch
Xcode is currently targeting and drops the framework at
`samples/iosApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)/ComposeApp.framework`.
Xcode then embeds and code-signs it like any other framework.

The Swift side does almost nothing — see `iosApp/ContentView.swift`:

```swift
private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

The picker library is wired against the hosting `UIViewController` inside that Kotlin
function — Swift does not need to call `initializeMediaPicker` itself.

## What is NOT committed

`iosApp.xcodeproj/` is intentionally git-ignored. xcodegen regenerates it deterministically
from `project.yml`, so checking it in would only add Xcode's version-specific churn (and
merge headaches when two contributors are on different Xcode point releases). If you'd
rather check it in for your fork, just remove the entry from `.gitignore`.

## Without xcodegen

If you can't install xcodegen, the fastest alternative is:

1. In Xcode, **File → New → Project → iOS → App** (interface: SwiftUI, language: Swift).
2. Save it into `samples/iosApp/`, name it `iosApp`.
3. Drag the four files in `iosApp/` (`iOSApp.swift`, `ContentView.swift`, `Info.plist`,
   `Assets.xcassets/`) over the freshly generated ones.
4. Open the target's **Build Phases** and add a new **Run Script Phase** above
   "Compile Sources" with the body:

   ```bash
   cd "$SRCROOT/../.."
   ./gradlew :samples:composeApp:embedAndSignAppleFrameworkForXcode
   ```

5. In **Build Settings**, set `Framework Search Paths` to:
   `$(SRCROOT)/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`.
6. In **General → Frameworks, Libraries, and Embedded Content**, add
   `ComposeApp.framework` from that path with **Embed & Sign**.

That's the same wiring `project.yml` produces automatically.
