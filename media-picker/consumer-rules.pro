# Consumer ProGuard rules for io.github.aashutosh:compose-media-picker
# Kept intentionally minimal — the library is pure Kotlin + AndroidX with no
# reflection, dynamic class loading, or JNI. Standard R8 rules are sufficient.

# Keep the FileProvider subclass: referenced from the merged AndroidManifest.xml.
-keep class io.github.aashutosh.mediapicker.internal.MediaPickerFileProvider { *; }
