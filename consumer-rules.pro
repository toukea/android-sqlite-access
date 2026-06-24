# istat-access-sqlite — consumer ProGuard/R8 rules.
# Shipped automatically to apps that depend on this library (consumerProguardFiles).
# Required for constructor-based hydration of immutable entities to survive minification.

# Keep the reflection metadata the ORM reads at runtime for the @Column strategy.
# These only retain RUNTIME-retention annotations (already reflectively readable by design),
# so they do not weaken name obfuscation.
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# NOTE: -keepattributes MethodParameters is intentionally NOT shipped here.
# It would retain original constructor parameter NAMES app-wide, which is unnecessary for the
# @Column(name=...) strategy and slightly aids reverse-engineering of any module compiled with
# -parameters. Apps that opt into the parameter-NAME deduction strategy must add BOTH of these to
# their own module (they already need -parameters to compile):
#     tasks.withType(JavaCompile).configureEach { options.compilerArgs << '-parameters' }
#     # proguard-rules.pro:
#     -keepattributes MethodParameters

# Keep the hydration annotations themselves (inner annotations use the $ separator).
-keep @interface istat.android.data.access.sqlite.SQLiteModel$CreatorConstructor
-keep @interface istat.android.data.access.sqlite.SQLiteModel$Persistable

# Keep constructors and fields of @Persistable entities:
#  - <init>(...)  : the creator constructor invoked during hydration,
#  - <fields>     : column names are derived from field names.
-keepclassmembers @istat.android.data.access.sqlite.SQLiteModel$Persistable class * {
    <init>(...);
    <fields>;
}
