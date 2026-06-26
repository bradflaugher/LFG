# Release build is not minified (see build.gradle.kts), so this file is mostly
# a no-op today. Kept as a placeholder for when isMinifyEnabled flips to true.

# Hilt / Dagger generated classes.
-keep class dagger.hilt.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Kotlin reflection used by some libs (richtext, etc).
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# Keep proto-generated classes.
-keep class com.bradflaugher.lfg.proto.** { *; }
