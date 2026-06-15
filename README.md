# Recipe Cook (Android)

A simple Android app built from scratch with Kotlin + Jetpack Compose.

## Features

- List recipes for cooking
- Add a new recipe (title + notes/ingredients)
- Randomly pick one recipe from the list

## Project structure

- `app/src/main/java/com/example/recipecook/MainActivity.kt` - UI and screen logic
- `app/src/main/java/com/example/recipecook/RecipeViewModel.kt` - recipe state and random selection logic
- `app/src/test/java/com/example/recipecook/RecipeViewModelTest.kt` - unit tests

## Requirements

- Android Studio Iguana or newer (or compatible)
- Android SDK 35
- JDK 17

## Run in Android Studio

1. Open this folder as a project.
2. Let Gradle sync finish.
3. Run the `app` configuration on an emulator or Android device.

## Run tests from terminal

```bash
cd /Users/igor.rakoch/dev/coreignite-dev/coreignite-workspace/recipe-cook-android
./gradlew test
```

If the Gradle wrapper is not present yet, generate it first:

```bash
cd /Users/igor.rakoch/dev/coreignite-dev/coreignite-workspace/recipe-cook-android
gradle wrapper
./gradlew test
```

