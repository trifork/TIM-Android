# General (make debugging easier etc)
-dontobfuscate
-optimizations code/simplification/arithmetic,code/simplification/cast,field/*,class/merging/*
-optimizationpasses 5
-keepattributes SourceFile,LineNumberTable

-keepclasseswithmembernames class com.trifork.timandroid.appauth.**{
     *;
}

-keepclasseswithmembernames class com.trifork.timandroid.biometric.**{
     *;
}

-keepclasseswithmembernames class com.trifork.timandroid.helpers.TIMLogger{
     *;
}


-keepclasseswithmembernames class com.trifork.timandroid.models.**{
     *;
}


-keepclasseswithmembernames class com.trifork.timandroid.TIM {
    *;
}

-keepclasseswithmembernames class com.trifork.timandroid.TIMAppBackgroundMonitor {
    *;
}

-keepclasseswithmembernames class com.trifork.timandroid.TIMAuth {
    *;
}

-keepclasseswithmembernames class com.trifork.timandroid.TIMDataStorage {
    *;
}

-keepclasseswithmembernames class com.trifork.timandroid.helpers.** {
    *;
}
