# ============================================
# Gson RuntimeTypeAdapterFactory Protection
# ============================================

# Keep RuntimeTypeAdapterFactory fields
-keepclassmembers class com.google.gson.typeadapters.RuntimeTypeAdapterFactory {
    private java.lang.String typeFieldName;
    private java.util.Map labelToSubtype;
    private java.util.Map subtypeToLabel;
}

# ============================================
# Kotlin Sealed Classes & Reflection
# ============================================

# Keep Kotlin metadata for sealed classes
-keep class kotlin.Metadata { *; }

# Keep all sealed classes and their metadata
-keep class * extends com.hifnawy.alquran.shared.domain.ServiceStatus { *; }
-keep class * extends com.hifnawy.alquran.shared.domain.ServiceStatus$** { *; }

# Keep all sealed subclasses in your project
-keep @kotlin.Metadata class ** extends ** {
    <init>(...);
}

# Preserve Kotlin sealed class information
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep names of sealed classes to preserve reflection
-keepnames class * extends com.hifnawy.alquran.shared.domain.ServiceStatus
-keepnames class * extends com.hifnawy.alquran.shared.domain.ServiceStatus$**

# Keep Kotlin reflection for sealed classes
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Preserve sealed class metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep all data classes used with Gson
-keepclassmembers class com.hifnawy.alquran.shared.domain.ServiceStatus$** {
    <init>(...);
    <fields>;
}

# Keep the sealed class hierarchy intact
-if class com.hifnawy.alquran.shared.domain.ServiceStatus
-keep class com.hifnawy.alquran.shared.domain.ServiceStatus$**

# ============================================
# Gson General Rules
# ============================================

# Keep Gson annotations
-keepattributes *Annotation*

# Keep generic signatures for Gson
-keepattributes Signature

# Keep fields for Gson serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep all model classes that might be serialized
-keep class com.hifnawy.alquran.shared.model.** { *; }
-keep class com.hifnawy.alquran.shared.domain.** { *; }

# ============================================
# KotlinPoet - Ignore compile-time only classes
# ============================================

# KotlinPoet references javax.lang.model classes which are compile-time only
-dontwarn javax.lang.model.element.Element
-dontwarn javax.lang.model.element.ElementKind
-dontwarn javax.lang.model.element.Modifier
-dontwarn javax.lang.model.type.TypeKind
-dontwarn javax.lang.model.type.TypeMirror
-dontwarn javax.lang.model.type.TypeVisitor
-dontwarn javax.lang.model.util.SimpleTypeVisitor8
-dontwarn javax.lang.model.util.SimpleTypeVisitor7
-dontwarn javax.lang.model.SourceVersion

# Additional javax.lang.model classes that might be referenced
-dontwarn javax.lang.model.**
