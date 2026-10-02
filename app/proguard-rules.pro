# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.planecatcher.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
