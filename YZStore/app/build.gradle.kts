plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.yzstore"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.yzstore"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation ("com.google.firebase:firebase-auth:23.1.0")
    implementation ("com.google.android.gms:play-services-auth:20.7.0")
    implementation (platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation ("com.google.firebase:firebase-firestore:24.7.2")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.squareup.picasso:picasso:2.71828")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.firebase:firebase-database:21.0.0")
}
