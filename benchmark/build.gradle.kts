import com.android.build.gradle.tasks.TestSuiteTestTask
import com.android.build.gradle.internal.tasks.AndroidTestTask
import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Task

plugins {
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.whip.benchmark"
    compileSdk = 37
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    buildTypes {
        create("benchmark") {
            isDebuggable = false
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFile("proguard-rules.pro")
            testProguardFile("proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
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
