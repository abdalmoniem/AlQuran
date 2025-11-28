import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

/**
 * The package name of the application.
 *
 * This constant holds the package name used throughout the application.
 * It is primarily used for identifying the application's namespace in
 * Android and is essential for intents, broadcasting, and other system
 * interactions.
 */
private val packageName = "com.hifnawy.alquran"

/**
 * The file object representing the local.properties file in the root project directory.
 *
 * This file is used to store custom configuration properties for the project, such as
 * signing configurations, debugging options, and other local settings. The properties
 * are loaded during the build process to configure the build environment.
 *
 * @see Properties
 * @see FileInputStream
 */
private val localPropertiesFile = rootProject.file("local.properties")

/**
 * A flag indicating whether debugging is enabled in the release variant.
 *
 * @see ApplicationBuildType.isDebuggable
 */
private var isDebuggingEnabled = false

/**
 * A flag indicating whether signing is enabled in the release variant.
 *
 * @see ApplicationBuildType.signingConfig
 */
private var isSigningConfigEnabled = false

plugins {
    alias(libs.plugins.pre.build)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.application)
}

android {
    when {
        localPropertiesFile.exists() -> {
            val keystoreProperties = Properties().apply { load(FileInputStream(localPropertiesFile)) }
            val signingProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")

            isSigningConfigEnabled =
                    signingProperties.all { property -> property in keystoreProperties.keys } && rootProject.file(keystoreProperties["storeFile"] as String).exists()

            when {
                !isSigningConfigEnabled -> signingProperties
                    .filter { property -> property !in keystoreProperties.keys }
                    .forEach { missingKey -> project.logger.warn("WARNING: missing key in '${localPropertiesFile.absolutePath}': $missingKey") }

                else                    -> signingConfigs {
                    project.logger.lifecycle("INFO: keystore: ${rootProject.file(keystoreProperties["storeFile"] as String).absolutePath}")

                    create("release") {
                        storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                        storePassword = keystoreProperties["storePassword"] as String
                        keyAlias = keystoreProperties["keyAlias"] as String
                        keyPassword = keystoreProperties["keyPassword"] as String
                    }
                }
            }

            isDebuggingEnabled = keystoreProperties.getProperty("isDebuggingEnabled")?.toBoolean() ?: false
        }

        else                         -> project.logger.warn("WARNING: local.properties not found, add local.properties in root directory to enable signing.")
    }

    defaultConfig {
        namespace = packageName
        applicationId = packageName

        minSdk = 28
        targetSdk = 36
        versionCode = 9
        versionName = "0.0.7.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileSdk {
        version = release(36)
    }

    sourceSets.forEach { sourceSet ->
        sourceSet.java.srcDir("src/$sourceSet.name")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = isDebuggingEnabled

            project.logger.lifecycle("INFO: $name isDebuggable: $isDebuggingEnabled")

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfigs.findByName("release")?.also { signingConfiguration ->
                when {
                    isSigningConfigEnabled -> {
                        signingConfig = signingConfiguration
                        project.logger.lifecycle("INFO: $name buildType is signed with release signing config.")
                        project.logger.lifecycle("INFO: $name signing config is located in ${signingConfiguration.storeFile?.absolutePath}")
                    }

                    else                   -> project.logger.warn("WARNING: $name buildType is not signed, add signing config in local.properties to enable signing.")
                }
            } ?: project.logger.error("ERROR: $name signing config not found, add signing config in local.properties")
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            project.logger.lifecycle("INFO: $name isDebuggable: $isDebuggable")

            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfigs.findByName("release")?.also { signingConfiguration ->
                when {
                    isSigningConfigEnabled -> {
                        signingConfig = signingConfiguration
                        project.logger.lifecycle("INFO: $name buildType is signed with release signing config.")
                        project.logger.lifecycle("INFO: $name signing config is located in ${signingConfiguration.storeFile?.absolutePath}")
                    }

                    else                   -> project.logger.lifecycle(
                            "INFO: $name buildType is signed with default signing config, " +
                            "add signing config in local.properties to enable signing."
                    )
                }
            } ?: project.logger.lifecycle("INFO: $name buildType is signed with default signing config, add signing config in local.properties to enable signing.")

            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            this as BaseVariantOutputImpl
            val baseName = "${applicationId}_${variant.buildType.name}_v${android.defaultConfig.versionName}"

            setProperty("archivesBaseName", baseName)

            outputFileName = "$baseName.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xwhen-guards")
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    buildFeatures {
        compose = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.timber)
    implementation(libs.material)
    implementation(libs.hoko.blur)
    implementation(project(":shared"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.gson.extras)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.named("preBuild") {
    dependsOn(preBuildPlugin.generateSampleData)
}
