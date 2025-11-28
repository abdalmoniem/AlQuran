plugins {
    `kotlin-dsl`
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
    }
}

gradlePlugin {
    plugins {
        create("androidLibraryConvention") {
            id = "com.hifnawy.pre.build"
            implementationClass = "com.hifnawy.pre.build.PreBuildPlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
}
