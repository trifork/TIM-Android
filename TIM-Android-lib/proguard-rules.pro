# General (make debugging easier etc)
-dontobfuscate
-optimizations code/simplification/arithmetic,code/simplification/cast,field/*,class/merging/*
-optimizationpasses 5
-keepattributes SourceFile,LineNumberTable
-dontwarn java.lang.invoke.StringConcatFactory

-keep class com.trifork.timandroid.** {
     public private *;
}