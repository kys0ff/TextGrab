# Add project specific ProGuard rules here.

# --- ML Kit Text Recognition ---
# ML Kit downloads recognizer models via optional dependencies. Keep their entry
# points so R8 does not strip the dynamically-loaded script recognizers.
-keep class com.google.mlkit.vision.text.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_text_common.** { *; }
-dontwarn com.google.mlkit.vision.text.**

# Keep the Arabic script recognizer options referenced reflectively.
-keep class com.google.mlkit.vision.text.arabic.** { *; }

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**

# Keep our service / tile / activity entry points referenced only from the manifest.
-keep class off.kys.textgrab.accessibility.TextGrabAccessibilityService { *; }
-keep class off.kys.textgrab.tile.TextGrabTileService { *; }
-keep class off.kys.textgrab.ocr.ScreenCaptureService { *; }
