import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.retro.fx7000g"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.retro.fx7000g"
        minSdk = 24
        targetSdk = 35
        versionCode = 29
        versionName = "2.9.0"
    }

    // Name the built APK e.g. "Casio FX-7000G v1.0.0.apk"
    applicationVariants.all {
        val versionName = this.versionName
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "Casio FX-7000G v$versionName.apk"
        }
    }

    signingConfigs {
        val keystoreProperties = Properties()
        val keystorePropertiesFile = rootProject.file("gradle/keystore.properties").takeIf { it.exists() }
            ?: rootProject.file("keystore.properties").takeIf { it.exists() }
        keystorePropertiesFile?.let { file ->
            file.inputStream().use { input -> keystoreProperties.load(input) }
        }
        create("release") {
            val home = System.getProperty("user.home")
            val defaultStore = file("$home/.keystores/calc-u-later.keystore").takeIf { it.exists() }
                ?: file("$home/.keystores/calc-u-later.jks")
            val resolvedStorePath = (keystoreProperties["android.releaseSigningStoreFile"]
                ?: keystoreProperties["storeFile"])?.toString()
                ?.replace(Regex("^~(?=/|$)"), home)

            storeFile = resolvedStorePath?.let { file(it) } ?: defaultStore
            storePassword = (keystoreProperties["release.keystore.password"]
                ?: keystoreProperties["storePassword"])?.toString() ?: "sayijiwan"
            keyAlias = (keystoreProperties["release.key.alias"]
                ?: keystoreProperties["keyAlias"])?.toString() ?: "homebrew"
            keyPassword = (keystoreProperties["release.key.password"]
                ?: keystoreProperties["keyPassword"])?.toString() ?: "sayijiwan"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
