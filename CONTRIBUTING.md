# Contributing

Thanks for your interest in compose-media-picker.

## Development setup

Requirements:
- JDK 17
- Android SDK with platform 35 + an emulator for instrumented tests
- Xcode 15+ for iOS work
- Node 18+ for the web sample

Common commands:
```bash
./gradlew build                                  # everything that doesn't need a device
./gradlew :media-picker:check :media-picker:apiCheck
./gradlew detekt ktlintCheck
./gradlew :samples:android:installDebug          # requires connected device / emulator
./gradlew :samples:desktop:run
./gradlew :samples:web:wasmJsBrowserDevelopmentRun
./gradlew :benchmark:run
```

## Style

- `kotlin.explicitApi()` is on — every new public symbol needs an explicit visibility modifier and KDoc.
- Detekt + ktlint must pass with zero warnings.
- Public API is gated by `binary-compatibility-validator`. If your PR changes the API, run `./gradlew :media-picker:apiDump` and commit the regenerated dump.
- Internal helpers should be annotated `@InternalMediaPickerApi` if they need to be public for cross-source-set wiring.

## Submitting changes

1. Open an issue first for non-trivial work so we can align on direction.
2. Write tests. The `:media-picker:commonTest` source set is the home for platform-agnostic logic; engine-specific tests live in `androidInstrumentedTest` / `iosTest`.
3. Update `CHANGELOG.md` under the "Unreleased" heading.
4. Run the full check locally before pushing:
   ```bash
   ./gradlew build detekt ktlintCheck :media-picker:apiCheck
   ```

## Releases

Releases are tag-driven. Push a tag matching `v0.1.0` (no extra prefix), and the
`publish-release.yml` workflow handles the rest: artifact upload to Maven Central via
Vanniktech's plugin, Dokka regeneration, and GitHub Pages publish.
