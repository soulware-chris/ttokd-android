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

-verbose # 로그 봄
-dontoptimize # 압축 하지 않음 그냥 하지말자...
-dontshrink  # 사용하지 않는 메소드를 유지하라

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# 안드로이드 jar파일이나 프로젝트의 경우 아래의 keep옵션들이 필요
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
#-keep class net.htmlparser.jericho.** { *; }

-ignorewarnings

################################################################
# Google Play Services Library
################################################################
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
################################################################

#-libraryjars libs/miapslib.jar
#-keep public class * { public protected *; }

-keep class com.minkutil.encryption.** { *; }
-keep class kr.co.miaps.bridge.** { public *; }
-keep class kr.co.thinkm.miaps.* {*;}

#-keep class org.apache.http.**

#-keep interface org.apache.http.**

#-keep class  org.apache.http.** { public *; }

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-keep class sun.misc.Unsafe { *; }
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

##---------------End: proguard configuration for Gson  ----------


-keep class com.minkutil.encryption.**
-keep class kr.co.miaps.bridge.** { public *; }

-keep class kr.co.thinkm.miaps.* {*;}


# TradeSign
-keep class tradesign.pki.** {public *; }
-keep class tradesign.crypto.provider.**
-keep class tradesign.crypto.spec.**
-keep class com.ktnet.asn1comp.** { public *; }
-keep class com.oss.asn1.** { public *;}

##--------------- my:D  ----------

-keep class io.snplab.myd.core.** { public *;}

# AndroidX.Security
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
  <fields>;
}

## Ktor
-keepclassmembers class io.ktor.** { volatile <fields>; }