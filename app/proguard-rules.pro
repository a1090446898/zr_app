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
# 保留 BluetoothConn 及其内部类
-keep class com.gsoft.inventory.utils.BluetoothConn { *; }
-keep class com.gsoft.inventory.utils.BluetoothConn$BluetoothConnHolder { *; }
# 保留所有Activity、Service、BroadcastReceiver等组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# 保留组件的构造方法和生命周期方法
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembernames class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留Manifest中声明的组件（防止混淆后系统无法识别）
-keep class com.gsoft.inventory.** { *; }

# ButterKnife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewBinder { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# Gson（保留模型类，替换为你的实体类包名）
-keep class com.gsoft.inventory.entity.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# ZXing扫码库
-keep class com.journeyapps.barcodescanner.** { *; }

# 华为扫码SDK
-keep class com.huawei.hms.scan.** { *; }

# 权限注解框架
-keep class permissions.dispatcher.** { *; }
-keepclasseswithmembernames class * {
    @permissions.dispatcher.* <methods>;
}

# 保留JNI方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留反射用到的类（替换为实际使用反射的类名）
# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static** valueOf(java.lang.String);
}

# 保留注解
-keepattributes *Annotation*
-keep class * extends java.lang.annotation.Annotation { *; }

# 保留 Sugar ORM 模型类（替换为你的实体类包路径）
-keep class com.gsoft.inventory.entity.** { *; }
-keep class com.gsoft.inventory.data.** { *; }
-keep class com.gsoft.inventory.utils.** { *; }

# 保留 Sugar ORM 注解及构造方法
-keepattributes *Annotation*
-keepclassmembers class com.gsoft.inventory.entity.** {
    public <init>();
}

# 保留 Sugar ORM 核心类（防止库本身被混淆）
-keep class com.orm.** { *; }
