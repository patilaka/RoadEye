# Add project specific ProGuard rules here.
-keep class com.carassistant.model.entity.** { *; }
-keepclassmembers class * extends com.carassistant.model.entity.SignEntity { *; }
