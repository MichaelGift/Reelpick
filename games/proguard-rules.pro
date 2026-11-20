# kotlinx.serialization DTOs used by Retrofit
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class com.myth.playpick.data.remote.dto.**$$serializer { *; }
-keepclassmembers class com.myth.playpick.data.remote.dto.** { *** Companion; }
-keepclasseswithmembers class com.myth.playpick.data.remote.dto.** { kotlinx.serialization.KSerializer serializer(...); }
