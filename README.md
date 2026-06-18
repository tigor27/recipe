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

## Make SMS links open the app (Android App Links)

SMS apps can auto-linkify `https://` URLs, but to open the app directly (instead of browser), Android must verify your domain.

1. The app currently uses links like:

```text
https://iicloud.tech/open?title=...&notes=...
```

2. Host this file on your domain at `https://iicloud.tech/.well-known/assetlinks.json`:

```json
[
  {
	"relation": ["delegate_permission/common.handle_all_urls"],
	"target": {
	  "namespace": "android_app",
	  "package_name": "com.example.recipecook",
	  "sha256_cert_fingerprints": [
		"81:5B:46:FF:52:AF:20:5B:38:DE:56:4F:99:78:D0:B8:7C:BD:C6:CD:AB:03:FA:C5:51:26:4D:FB:30:71:B2:4B"
	  ]
	}
  }
]
```

3. Reinstall the app, then test:

```bash
adb shell am start -a android.intent.action.VIEW -d "https://iicloud.tech/open?title=Test&notes=Hello"
```

