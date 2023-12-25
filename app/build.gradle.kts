plugins {
    id("com.android.application")
}

android {
    namespace = "com.asameer.killappnow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.asameer.killappnow"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging {
        resources {
            // Not needed as long as we don't use reflection with Kotlin
            excludes.add("**/*.kotlin_builtins")
            excludes.add("**/*.kotlin_module")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    // Set the APK file name here
    applicationVariants.all {
        val variant = this
        variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    val outputFileName = "KillAppNow_${variant.versionName}.apk"
                    println("OutputFileName: $outputFileName")
                    output.outputFileName = outputFileName
                }
    }}

dependencies {

    implementation("androidx.annotation:annotation:1.7.1")
    compileOnly ("de.robv.android.xposed:api:82")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}