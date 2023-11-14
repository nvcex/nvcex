import java.util.Properties
import java.io.IOException
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("com.google.protobuf") version "0.9.4"
}

val commitCount by project.extra {
    execCommand("git rev-list --count HEAD")?.toInt()
            ?: throw GradleException("Unable to get number of commits. Make sure git is initialized.")
}

val latestTag by project.extra {
    execCommand("git describe")
            ?: throw GradleException(
                    "Unable to get version name using git describe.\n" +
                            "Make sure you have at least one annotated tag and git is initialized.\n" +
                            "You can create an annotated tag with: git tag -a 1.0 -m \"1.0\""
            )
}

// Create a variable called keystorePropertiesFile, and initialize it to your
// keystore.properties file, in the rootProject folder.
val keystorePropertiesFile = rootProject.file("keystore.properties")

// Initialize a new Properties() object called keystoreProperties.
val keystoreProperties = Properties()

// Load your keystore.properties file into the keystoreProperties object.
try {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} catch (ignore: IOException) {
}

android {
    namespace = "io.github.nvcex.android"
    compileSdk = 34

    if (!keystoreProperties.isEmpty) {
        signingConfigs {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "io.github.nvcex.android"
        minSdk = 29
        targetSdk = 34
        versionCode = commitCount
        versionName = latestTag.removePrefix("v")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        if (!keystoreProperties.isEmpty) {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            //excludes += "**"
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.0"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("com.google.protobuf:protobuf-javalite:3.25.0")
    implementation("com.google.guava:guava:32.1.3-android")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("net.arnx:jsonic:1.3.0")

    implementation("com.fasterxml.jackson.core:jackson-core:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.3")

    compileOnly("io.github.libxposed:api:100")
    implementation("io.github.libxposed:service:100-1.0.0")
}

fun execCommand(command: String): String? {
    val cmd = command.split(" ").toTypedArray()
    val process = ProcessBuilder(*cmd)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
    return process.inputStream.bufferedReader().readLine()?.trim()
}
