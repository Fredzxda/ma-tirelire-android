plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val baseVersion = providers.gradleProperty("appVersion").get().removeSuffix("-dev")
val stableRelease = providers.gradleProperty("stableRelease").orNull == "true"
val developmentVersion = "$baseVersion-dev"
val releaseVersion = if (stableRelease) baseVersion else developmentVersion

android {
    namespace = "org.matirelire.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.matirelire.android.demo"
        minSdk = 31
        targetSdk = 37
        versionCode = providers.gradleProperty("appVersionCode").get().toInt()
        versionName = baseVersion
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug { versionNameSuffix = "-dev" }
        release { versionNameSuffix = if (stableRelease) "" else "-dev" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

tasks.register("printAppVersion") { doLast { println(developmentVersion) } }
tasks.register("printReleaseVersion") { doLast { println(releaseVersion) } }
