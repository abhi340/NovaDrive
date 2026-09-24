plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.room)
  alias(libs.plugins.google.services)
}

import java.util.Properties

val localProperties = Properties().apply {
    val localPropFile = rootProject.file("local.properties")
    if (localPropFile.exists()) {
        localPropFile.inputStream().use { load(it) }
    }
}

android {
  namespace = "com.example.tvdrive"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.novadrive.tv"
    minSdk = 23  // androidx.tv:tv-foundation requires 23; API 21 TVs are negligible
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    val googleTvClientId = localProperties.getProperty("google.tv.client.id", "")
    val googleTvClientSecret = localProperties.getProperty("google.tv.client.secret", "")
    val firebaseWebClientId = localProperties.getProperty("firebase.web.client.id", "")

    buildConfigField("String", "GOOGLE_TV_CLIENT_ID", "\"$googleTvClientId\"")
    buildConfigField("String", "GOOGLE_TV_CLIENT_SECRET", "\"$googleTvClientSecret\"")
    buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"$firebaseWebClientId\"")
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
    aidl = false
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  room {
    schemaDirectory("$projectDir/schemas")
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.play.services)

  // Lifecycle + Compose
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose base
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")

  // Jetpack Compose for TV
  implementation(libs.androidx.tv.foundation)
  implementation(libs.androidx.tv.material)

  // Room (with KSP)
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  // DataStore
  implementation(libs.datastore.preferences)

  // Coil — memory-capped image loading
  implementation(libs.coil.compose)

  // Media3 — selective modules only (smaller footprint)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.ui)
  implementation(libs.media3.session)
  implementation(libs.media3.datasource.okhttp)

  // Google Sign-In (Play Services — already on device, no extra JAR)
  implementation(libs.google.play.services.auth)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)

  // ZXing (QR code generator)
  implementation(libs.zxing.core)

  // OkHttp — all REST calls (Drive + Photos APIs)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Tests
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
