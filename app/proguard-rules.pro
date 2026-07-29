# Add project specific ProGuard rules here.

# --- R8 Full Mode Attributes ---
# Standard attributes for crash reporting, reflection, and generic stability.
-keepattributes Signature,AnnotationDefault,EnclosingMethod,InnerClasses,SourceFile,LineNumberTable

# --- Tesseract4Android ---
# Tesseract uses JNI; keep all its classes and native methods to avoid crashes.
-keep class com.googlecode.tesseract.android.** { *; }
-keep class cz.adaptech.tesseract4android.** { *; }
-dontwarn com.googlecode.tesseract.android.**
-dontwarn cz.adaptech.tesseract4android.**

# --- Core Data Models ---
# Keep names for enums and fields used in JSON serialization (HistoryRepository).
-keepclassmembers class off.kys.textgrab.core.model.** {
    <fields>;
}
-keepclassmembers enum off.kys.textgrab.core.model.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}