# Repository Guidelines

## Project Structure & Module Organization

This repository is an Android sandbox application built with Gradle, Java, and Kotlin. The active module is `app` (declared in `settings.gradle`). Production code lives under `app/src/main/java/com/vdian/android/lib/testforgradle/`, grouped by experiment or Android feature, such as `nfc/`, `room/`, `surface/`, and `thread/`. XML resources are in `app/src/main/res/`; shaders and other packaged files are in `app/src/main/assets/`. Local unit tests belong in `app/src/test/java/`, while device tests belong in `app/src/androidTest/java/`. `annotationtest/` contains a library-module prototype but is currently excluded from the Gradle build.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper from the repository root:

- `./gradlew :app:assembleDebug` builds the debug APK.
- `./gradlew :app:installDebug` installs it on a connected emulator or device.
- `./gradlew :app:testDebugUnitTest` runs JVM unit tests.
- `./gradlew :app:connectedDebugAndroidTest` runs instrumented JUnit/Espresso tests on a connected device.
- `./gradlew :app:lintDebug` runs Android lint for the debug variant.
- `./gradlew clean` removes generated build output.

## Coding Style & Naming Conventions

Target Java 8 and follow existing Android Studio formatting: four-space indentation, braces on the declaration line, and one public top-level type per file. Use `UpperCamelCase` for classes and activities, `lowerCamelCase` for methods and properties, and lowercase package names. Resource names must use `snake_case`, for example `activity_surface_test.xml`. Keep feature code in the matching package and prefer descriptive names over adding more generic `Test*` types. No repository-wide formatter is configured; format changed files with Android Studio before committing.

## Testing Guidelines

JUnit 4 is used for local tests; AndroidX JUnit and Espresso cover device behavior. Name test classes after the subject (`NFCWriterTest`) and test methods by behavior (`writesTextRecord`). Add JVM tests for pure logic and instrumented tests for APIs requiring Android framework state, hardware, or permissions. There is no enforced coverage threshold, but new behavior and regressions should receive focused tests.

## Commit & Pull Request Guidelines

Recent commits use short, direct Chinese summaries such as `完善demo` and `增加注释`. Keep each commit focused and use an imperative summary that names the affected feature. Pull requests should explain the purpose, list verification commands and device/API level, link relevant issues, and include screenshots or recordings for UI changes. Call out permission, NFC, camera, signing, or ProGuard impacts explicitly.

## Security & Configuration

Treat the existing `privacy` signing file and credentials as demo-only. Do not add production keys, tokens, personal endpoints, or machine-specific SDK paths to version control.
