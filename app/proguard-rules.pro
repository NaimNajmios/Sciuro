# Sciuro ProGuard Rules

# --- SQLCipher (native library) ---
-keep class net.zetetic.database.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class * extends net.sqlcipher.database.SQLiteOpenHelper
-keep class * extends net.sqlcipher.database.SQLiteDatabase

# --- SQLDelight (generated classes accessed reflectively) ---
-keep class com.sciuro.core.ledger.db.** { *; }
-keep class * implements app.cash.sqldelight.Transacter { *; }

# --- Koin (reflection-based injection) ---
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}

# --- Ktor (networking, engine discovery) ---
-keep class io.ktor.** { *; }
-keep class * implements io.ktor.client.engine.HttpClientEngine { *; }

# --- kotlinx.serialization (@Serializable classes) ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sciuro.**$$serializer { *; }
-keepclassmembers class com.sciuro.** {
    *** Companion;
}
-keepclasseswithmembers class com.sciuro.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- AndroidX / Compose Navigation (type-safe) ---
-keep class * extends androidx.navigation.NavType { *; }

# --- Keep debug metadata for crash reporting ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile