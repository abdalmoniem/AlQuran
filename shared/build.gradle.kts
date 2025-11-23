import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

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
 * A flag indicating whether signing is enabled in the release variant.
 *
 * @see com.android.build.api.dsl.ApplicationBuildType.signingConfig
 */
private var isSigningConfigEnabled = false

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hifnawy.alquran.shared"
    compileSdk = 36

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
        }

        else                         -> project.logger.warn("WARNING: local.properties not found, add local.properties in root directory to enable signing.")
    }

    defaultConfig {
        minSdk = 28
        lint.targetSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            consumerProguardFiles("consumer-proguard-rules.pro")

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

            consumerProguardFiles("consumer-proguard-rules.pro")

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
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.timber)
    implementation(libs.androidx.media)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
