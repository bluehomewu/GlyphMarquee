# ============================================================
# GlyphMarquee ProGuard / R8 Rules
# ============================================================

# Preserve stack trace line numbers for crash debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Nothing / Ketchum SDK ───────────────────────────────────
# Keep all public API classes so the SDK reflection/JNI bindings
# remain intact after shrinking.
-keep class com.nothing.ketchum.** { *; }
-keep interface com.nothing.ketchum.** { *; }
-dontwarn com.nothing.ketchum.**

# ── GlyphMatrix SDK (AAR) ───────────────────────────────────
-keep class com.nothing.glyphmatrix.** { *; }
-keep interface com.nothing.glyphmatrix.** { *; }
-dontwarn com.nothing.glyphmatrix.**

# ── Application classes ─────────────────────────────────────
-keep class tw.bluehomewu.glyphmarquee.** { *; }

# ── Android Service / Activity lifecycle ────────────────────
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver

# ── Enum (required for Kotlin enums) ────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Serialisation / SharedPreferences keys ──────────────────
# (No custom Parcelables at the moment; add if introduced later)

# ── Kotlin metadata (needed for reflection & coroutines) ────
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── OkHttp ──────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Markwon (Markdown renderer) ─────────────────────────────
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

