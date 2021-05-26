# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Developer\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}


# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

#############################################
#
# Android开发中一些需要保留的公共部分
#
#############################################

# 保留我们使用的四大组件，自定义的Application等等这些类不被混淆
# 因为这些子类都有可能被外部调用
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Appliction
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService


# 保留support下的所有类及其内部类
-keep class android.support.** {*;}

# 保留继承的
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.v7.**
-keep public class * extends android.support.annotation.**

# 保留R下面的资源
-keep class **.R$* {*;}

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留在Activity中的方法参数是view的方法，
# 这样以来我们在layout中写的onClick就不会被影响
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# webView处理，项目中没有使用到webView忽略即可
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
    public *;
}

-keepclassmembers class * extends android.webkit.webViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.webViewClient {
    public void *(android.webkit.webView, jav.lang.String);
}


#############################################
#
# 项目中特殊处理部分
#
#############################################

#-----------处理自己的库---------------
-dontwarn com.core.base.**

#-----------处理反射类---------------



#-----------处理js交互---------------
#-keep public class com.core.base.js.Native2JS
-keepclassmembers class com.core.base.js.Native2JS {
    public *;
}



#-----------处理实体类---------------
# 在开发的时候我们可以将所有的实体类放在一个包内，这样我们写一次混淆就行了。
#-keep class com.blankj.data.bean.**{ *; }

#-----------处理第三方依赖库---------

#okhttp
-dontwarn javax.annotation.**

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** {*;}
-keep class okio.** {*;}

-dontwarn com.google.android.**
-keep class com.google.android.** {*;}

-dontwarn com.appsflyer.**
-keep class com.appsflyer.** {*;}

-dontwarn com.facebook.**
#-keep class com.facebook.** {*;}

-dontwarn com.jph.takephoto.**


-keep class com.ccsky.sfish.dao.** {*;}
-keep class androidx.** {*;}
-dontwarn org.jsoup.**
#-keep class org.jsoup.** {*;}

-dontwarn com.hippo.**
#-keep class com.hippo.glview.** {*;}
#-keep class com.hippo.easyrecyclerview.** {*;}
-keep class com.hippo.a7zip.** {*;}
-keep class com.hippo.image.** {*;}
#-keep class com.hippo.tuxiang.** {*;}

-dontwarn com.crashlytics.**
#-keep class com.crashlytics.** {*;}

-dontwarn io.fabric.**
#-keep class io.fabric.** {*;}

-dontwarn de.greenrobot.dao.**
-keep class de.greenrobot.dao.** {*;}

# Gson
-keepattributes Signature # 避免混淆泛型
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
# 使用Gson时需要配置Gson的解析对象及变量都不混淆。不然Gson会找不到变量。
# 将下面替换成自己的实体类
#-keep class com.example.bean.** { *; }

#greendao
#-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
#public static java.lang.String TABLENAME;
#}
-keep class **$Properties

# If you do not use SQLCipher:
-dontwarn org.greenrobot.greendao.database.**
# If you do not use Rx:
-dontwarn rx.**


-dontwarn org.xmlpull.v1.XmlPullParser
-dontwarn org.xmlpull.v1.XmlSerializer
-keep class org.xmlpull.v1.* {*;}

-keep class com.bumptech.** { *; }

-optimizationpasses 5

-keepattributes SourceFile, LineNumberTable


-dontwarn okio.**
-dontwarn com.squareup.okhttp.**
-dontwarn okhttp3.**
-dontwarn javax.annotation.**
-dontwarn com.android.volley.toolbox.**
-dontwarn com.facebook.infer.**

# greenDAO

-keep class org.greenrobot.** { *; }
-keep interface org.greenrobot.** { *; }

#ref: https://juejin.im/post/5d5fb53b51882554a13f8b6a
-dontwarn org.greenrobot.greendao.database.**
-dontwarn org.greenrobot.greendao.rx.**

#-keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
#public static java.lang.String TABLENAME;
#}
-keep class **$Properties { *; }

# If you DO use SQLCipher:
#-keep class org.greenrobot.greendao.database.SqlCipherEncryptedHelper { *; }

# If you do NOT use SQLCipher:
-dontwarn net.sqlcipher.database.**
# If you do NOT use RxJava:
-dontwarn rx.**

# ButterKnife
# Retain generated class which implement Unbinder.
-keep public class * implements butterknife.Unbinder { public <init>(**, android.view.View); }

# Prevent obfuscation of types which use ButterKnife annotations since the simple name
# is used to reflectively look up the generated ViewBinding.
-keep class butterknife.*
-keepclasseswithmembernames class * { @butterknife.* <methods>; }
-keepclasseswithmembernames class * { @butterknife.* <fields>; }

# OkHttp3
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# rhino
-dontwarn org.mozilla.javascript.**
-dontwarn org.mozilla.classfile.**
-keep class org.mozilla.javascript.** { *; }

# jsoup
-keeppackagenames org.jsoup.nodes

# andrroid v4 v7
-dontwarn android.support.v4.**
-dontwarn android.support.v7.**

# rx
-dontwarn sun.misc.**
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
   long producerIndex;
   long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

#mongodb
-dontwarn javax.**
-dontwarn java.lang.management.**
-dontwarn io.netty.**
-dontwarn org.ietf.jgss.**
-dontwarn org.slf4j.**
-dontwarn org.xerial.snappy.**

-keep class javax.** { *; }
-keep class java.lang.management.** { *; }
-keep class io.netty.** { *; }
-keep class org.ietf.jgss.** { *; }
-keep class org.slf4j.** { *; }
-keep class org.xerial.snappy.** { *; }

# guava
-dontwarn com.google.**
-keep class com.google.** {*;}

-dontwarn java.lang.ClassValue
-keep class java.lang.ClassValue { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement { *; }

