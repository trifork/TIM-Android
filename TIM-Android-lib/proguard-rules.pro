# General (make debugging easier etc)
-dontobfuscate
-optimizations code/simplification/arithmetic,code/simplification/cast,field/*,class/merging/*
-optimizationpasses 5
-keepattributes SourceFile,LineNumberTable

-keep class com.trifork.timandroid.appauth.**{
     *;
}

-keep class com.trifork.timandroid.biometric.**{
     *;
}

-keep class com.trifork.timandroid.helpers.TIMLogger{
     *;
}


-keep class com.trifork.timandroid.models.**{
     *;
}


-keep class com.trifork.timandroid.TIM {
    *;
}

-keep class com.trifork.timandroid.TIMAppBackgroundMonitor {
    *;
}

-keep class com.trifork.timandroid.TIMAuth {
    *;
}

-keep class com.trifork.timandroid.TIMDataStorage {
    *;
}
