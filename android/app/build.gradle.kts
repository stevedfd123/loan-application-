plugins {
    id("com.android.application")
}

android {
    namespace = "com.stevedfd123.loanapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.stevedfd123.loanapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Signed with the debug keystore so the APK installs straight away
        // without an "unsigned package" error. Swap in a real release keystore
        // before publishing to the Play Store.
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Keep the HTML/CSS/JS as the single source of truth: the root index.html
    // is copied into the APK on every build, so the app and the GitHub Pages
    // site can never drift apart.
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

/**
 * Copies the published web app (../../index.html) into the APK's assets folder.
 * Hooked into the build so it always runs before anything is compiled.
 */
val syncWebApp by tasks.registering(Copy::class) {
    from(rootProject.file("../index.html"))
    into(layout.projectDirectory.dir("src/main/assets/www"))
}

tasks.named("preBuild") {
    dependsOn(syncWebApp)
}

dependencies {
    // No third-party dependencies: the app is a plain WebView shell.
}
