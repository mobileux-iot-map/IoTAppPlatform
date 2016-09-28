# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/shheo/IoTAppDev/sdk/tools/proguard/proguard-android.txt
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


-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

#REQUIRED FOR ALLJOYN TO FUNCTION WHEN USED WITH PROGUARD
#(NOTE: could use -keep class org.alljoyn.bus** {*;} but the
# alljoyn samples use org.alljoyn.bus.samples so if you checked the .apk
# obfuscated code would not be seen)
-dontwarn org.alljoyn.bus.**
-keepattributes *Annotation*
-keepattributes Signature
-keep class org.alljoyn.bus.annotation.** { *; }
-keep class org.alljoyn.bus.ifaces.** { *; }
-keep class org.alljoyn.bus.AnnotationBusException** { *; }
-keep class org.alljoyn.bus.AuthListener** { *; }
-keep class org.alljoyn.bus.BusAttachment** { *; }
-keep class org.alljoyn.bus.BusException** { *; }
-keep class org.alljoyn.bus.BusListener** { *; }
-keep class org.alljoyn.bus.BusObject** { *; }
-keep class org.alljoyn.bus.BusObjectListener** { *; }
-keep class org.alljoyn.bus.ErrorReplyBusException** { *; }
-keep class org.alljoyn.bus.InterfaceDescription** { *; }
-keep class org.alljoyn.bus.IntrospectionListener** { *; }
-keep class org.alljoyn.bus.IntrospectionWithDescriptionListener** { *; }
-keep class org.alljoyn.bus.KeyStoreListener** { *; }
-keep class org.alljoyn.bus.MarshalBusException** { *; }
-keep class org.alljoyn.bus.MessageContext** { *; }
-keep class org.alljoyn.bus.MsgArg** { *; }
-keep class org.alljoyn.bus.Mutable** { *; }
-keep class org.alljoyn.bus.OnJoinSessionListener** { *; }
-keep class org.alljoyn.bus.ProxyBusObject** { *; }
-keep class org.alljoyn.bus.SecurityViolationListener** { *; }
-keep class org.alljoyn.bus.SessionListener** { *; }
-keep class org.alljoyn.bus.SessionOpts** { *; }
-keep class org.alljoyn.bus.SessionPortListener** { *; }
-keep class org.alljoyn.bus.SignalEmitter** { *; }
-keep class org.alljoyn.bus.Signature** { *; }
-keep class org.alljoyn.bus.Status** { *; }
-keep class org.alljoyn.bus.Variant** { *; }
-keep class org.alljoyn.bus.VariantTypeReference** { *; }
-keep class org.alljoyn.** { *; }
#Keep all BusObjects
-keep class * implements org.alljoyn.bus.BusObject { *; }
-keepclassmembers class * implements org.alljoyn.bus.BusObject {*;}
#------------------------------------------------------------------------------
#USER NEEDS TO MODIFY WITH VALUES FROM THEIR APPLICATION - CHAT USED AS AN EXAMPLE
#Need to keep all AllJoyn interfaces and classmembers of the AllJoyn interfaces
# interface for alljoyn clustering
-keep interface kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity.alljoyn.ClusteringModule
-keepclassmembers interface kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity.alljoyn.ClusteringModule { *; }
-keepclassmembers class * implements kr.ac.kaist.resl.cmsp.iotapp.engine.connectivity.alljoyn.ClusteringModule { *; }
# interfaces for IoT-App services
-keep interface * extends kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ThingService
-keepclassmembers interface * extends kr.ac.kaist.resl.cmsp.iotapp.library.service.general.ThingService { *; }
# osgi and external libraries
-keep class org.slf4j.** { *; }
-keep class org.apache.** { *; }
-keep class org.osgi.** { *; }
-dontwarn org.osgi.**
#------------------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService

-keepclasseswithmembernames class * {
   native <methods>;
}

-keepclasseswithmembernames class * {
   public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembernames class * {
   public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
   public static **[] values();
   public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
   public static final android.os.Parcelable$Creator *;
}