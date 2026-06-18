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

## SMS Sharing with Clickable Deep Links

Share recipes via SMS with **clickable links** that open the Recipe app:

### How it works

1. **Generate clickable SMS link**
   - Link format: `https://iicloud.tech/open?title=RecipeName&notes=Ingredients`
   - SMS apps auto-detect and linkify `https://` URLs  (**blue, underlined, tappable**)

2. **On first tap**
   - Android shows "Open with" app chooser
   - User selects "Recipe Cook" → "Always"

3. **After first tap**
   - Links open directly in Recipe app (no app chooser)
   - No server setup or domain verification required

### Why this approach works (without server hosting)

- ✅ Links are **clickable in SMS** (HTTPS is auto-linkified by SMS apps)
- ✅ Opens **Recipe app directly** (Android intent resolution)
- ✅ **No assetlinks.json or domain verification** needed
- ✅ **No server setup** required
- ℹ️ First tap may show app chooser (standard Android behavior

### Testing

```bash
# HTTPS link (clickable in SMS, primary format)
adb shell am start -a android.intent.action.VIEW \
  -d "https://iicloud.tech/open?title=MyRecipe&notes=Ingredients"

# Custom scheme fallback (not auto-linkified but works)
adb shell am start -a android.intent.action.VIEW \
  -d "recipecook://recipe/open?title=MyRecipe&notes=Ingredients"
```
