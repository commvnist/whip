import com.android.build.gradle.tasks.TestSuiteTestTask
import com.android.build.gradle.internal.tasks.AndroidTestTask
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    jacoco
}

configurations.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1",
        "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.8.1",
    )
}

val releaseStoreFile = providers.environmentVariable("WHIP_KEYSTORE_FILE")
val releaseStorePassword = providers.environmentVariable("WHIP_KEYSTORE_PASSWORD")
val releaseKeyAlias = providers.environmentVariable("WHIP_KEY_ALIAS")
val releaseKeyPassword = providers.environmentVariable("WHIP_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isPresent }

android {
    namespace = "com.whip.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "commvne.com.whip.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 54
        versionName = "0.3.48"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile.get())
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "Whip Dev")
            // Coverage stays on the same disposable-emulator lane as the E2E
            // suite; scripts/coverage validates both reports independently.
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        create("benchmark") {
            initWith(getByName("release"))
            // Keep performance fixtures and benchmark-driven clears completely
            // isolated from a user's installed release and its local-only data.
            applicationIdSuffix = ".benchmark"
            versionNameSuffix = "-benchmark"
            // BaselineProfileRule records JVM descriptors from the installed
            // target. Keep benchmark descriptors stable and let R8 rewrite the
            // checked-in profile when producing the minified release APK.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    lint {
        // Toolchain and alpha Health Connect updates are intentionally reviewed separately.
        disable += setOf("AndroidGradlePluginVersion", "GradleDependency")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.androidx.window)
    implementation(libs.androidx.profileinstaller)
    ksp(libs.androidx.room.compiler)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// The E2E contract test validates this repository-level capability register.
// Declare it explicitly so Gradle cannot reuse a stale passing test result after
// the matrix changes.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    inputs.file(rootProject.file("docs/quality/e2e-coverage.tsv"))
}

val batchedAndroidCoverageDirectory =
    providers.gradleProperty("whipAndroidCoverageExecutionDataDir")
val expectedBatchedAndroidCoverageFiles =
    providers.gradleProperty("whipAndroidCoverageExpectedBatchCount")

tasks.register<JacocoReport>("createBatchedDebugAndroidTestCoverageReport") {
    group = "verification"
    description = "Creates debug instrumentation coverage from fresh, isolated batch data."
    notCompatibleWithConfigurationCache(
        "Whip validates caller-owned batched execution data at task execution.",
    )
    dependsOn("compileDebugKotlin", "compileDebugJavaWithJavac")

    val kotlinClasses =
        layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
    val javaClasses =
        layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    classDirectories.setFrom(kotlinClasses, javaClasses)
    sourceDirectories.setFrom(files("src/main/java"))
    additionalSourceDirs.setFrom(files("src/main/java"))
    executionData.setFrom(
        batchedAndroidCoverageDirectory.map { executionDataDirectory ->
            fileTree(executionDataDirectory) {
                include("batch-*.ec")
            }
        },
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/coverage/androidTest/debug/connected/report.xml"),
        )
        html.required.set(true)
        html.outputLocation.set(
            layout.buildDirectory.dir("reports/coverage/androidTest/debug/connected"),
        )
        csv.required.set(false)
    }

    doFirst {
        if (!batchedAndroidCoverageDirectory.isPresent ||
            !expectedBatchedAndroidCoverageFiles.isPresent
        ) {
            throw GradleException(
                "Both whipAndroidCoverageExecutionDataDir and " +
                    "whipAndroidCoverageExpectedBatchCount are required.",
            )
        }
        val expectedFileCount = expectedBatchedAndroidCoverageFiles.get().toIntOrNull()
        if (expectedFileCount == null || expectedFileCount <= 0) {
            throw GradleException("whipAndroidCoverageExpectedBatchCount must be positive.")
        }
        val executionDataFiles = fileTree(batchedAndroidCoverageDirectory.get()) {
            include("batch-*.ec")
        }.files.sortedBy { it.name }
        if (executionDataFiles.size != expectedFileCount || executionDataFiles.any { it.length() == 0L }) {
            throw GradleException(
                "Expected $expectedFileCount nonempty batched coverage files, found " +
                    "${executionDataFiles.size} in ${batchedAndroidCoverageDirectory.get()}.",
            )
        }
    }
}

val androidTargetGuard = rootProject.layout.projectDirectory.file("scripts/android-target-guard")
val explicitAndroidSerial = providers.environmentVariable("ANDROID_SERIAL")
val commandPath = providers.environmentVariable("PATH")

tasks.withType<TestSuiteTestTask>().configureEach {
    notCompatibleWithConfigurationCache(
        "Whip validates the live Android instrumentation target at task execution.",
    )
    val validateTarget = object : Action<Task> {
        override fun execute(guardedTask: Task) {
            val testTask = guardedTask as TestSuiteTestTask
            if (!testTask.name.startsWith("connected") ||
                testTask.testSuiteTarget.orNull != TestSuiteTestTask.CONNECTED_TEST_TEST_SUITE_TARGET_NAME
            ) {
                throw GradleException(
                    "Whip instrumentation may run only through an explicitly selected connected emulator; " +
                        "refusing ${testTask.path} target '${testTask.testSuiteTarget.orNull ?: "unknown"}'.",
                )
            }

            val validationProcess = ProcessBuilder(
                "bash",
                androidTargetGuard.asFile.absolutePath,
                "instrumentation",
            )
            explicitAndroidSerial.orNull?.let {
                validationProcess.environment()["ANDROID_SERIAL"] = it
            } ?: validationProcess.environment().remove("ANDROID_SERIAL")
            commandPath.orNull?.let { validationProcess.environment()["PATH"] = it }
            validationProcess.inheritIO()
            if (validationProcess.start().waitFor() != 0) {
                throw GradleException("Android instrumentation target validation failed for ${testTask.path}.")
            }
        }
    }
    doFirst("validateWhipAndroidInstrumentationTarget", validateTarget)
    extensions.extraProperties["whipAndroidTargetGuardAction"] = actions.first()
}

tasks.configureEach {
    if (this is AndroidTestTask) {
        notCompatibleWithConfigurationCache(
            "Whip validates the live Android instrumentation target at task execution.",
        )
        val validateTarget = object : Action<Task> {
            override fun execute(guardedTask: Task) {
                if (guardedTask !is DeviceProviderInstrumentTestTask ||
                    !guardedTask.name.startsWith("connected")
                ) {
                    throw GradleException(
                        "Whip instrumentation may run only through an explicitly selected connected emulator; " +
                            "refusing ${guardedTask.path}.",
                    )
                }

                val validationProcess = ProcessBuilder(
                    "bash",
                    androidTargetGuard.asFile.absolutePath,
                    "instrumentation",
                )
                explicitAndroidSerial.orNull?.let {
                    validationProcess.environment()["ANDROID_SERIAL"] = it
                } ?: validationProcess.environment().remove("ANDROID_SERIAL")
                commandPath.orNull?.let { validationProcess.environment()["PATH"] = it }
                validationProcess.inheritIO()
                if (validationProcess.start().waitFor() != 0) {
                    throw GradleException("Android instrumentation target validation failed for ${guardedTask.path}.")
                }
            }
        }
        doFirst("validateWhipAndroidInstrumentationTarget", validateTarget)
        extensions.extraProperties["whipAndroidTargetGuardAction"] = actions.first()
    }
}
