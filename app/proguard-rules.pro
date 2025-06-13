# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp Rules
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Retrofit and OkHttp classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Модели для Retrofit и GSON
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Тензорфлоу Лайт
-keep class org.tensorflow.lite.** { *; }

# Сохраняем Generic Signatures для дженериков
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes InnerClasses,EnclosingMethod,Signature

# Правила для LiveData и ViewModel
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <methods>;
}
-keepclassmembers class android.arch.** { *; }
-keep class * implements androidx.lifecycle.LiveData { *; }

# Правила для сохранения Generic types в ModelView и LiveData
-keepclassmembers class ** {
    androidx.lifecycle.MutableLiveData *;
    androidx.lifecycle.LiveData *;
}

# GSON
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
# Keep anonymous subclasses of TypeToken (important for List<T> type resolution)
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class * implements java.io.Serializable { *; }

# Модели данных приложения
-keep class com.martist.vitamove.workout.data.models.** { *; }
-keep class com.martist.vitamove.db.entity.** { *; }
-keep class com.martist.vitamove.models.** { *; }
-keep class com.martist.vitamove.workout.data.models.cache.** { *; }
-keep class com.martist.vitamove.workout.data.models.room.** { *; }

# EventBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-dontwarn android.arch.util.paging.CountedDataSource
-dontwarn androidx.room.paging.LimitOffsetDataSource