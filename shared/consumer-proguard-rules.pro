# Keep all R classes completely
-keep class **.R
-keep class **.R$* {
    public static <fields>;
}

# Don't obfuscate R class field names
-keepnames class **.R$*

# Specifically protect drawable resources
-keepclassmembers class **.R$drawable {
    public static final int surah_*;
}
