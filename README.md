# Memorium

Memory card game, recovered from `3.apk` (v1.0.2, 2014). Two builds share the same assets:

- `web/` — **Compose Multiplatform web app** (Kotlin/Wasm), a Compose rewrite of the game
- `app/` — the original libGDX Android app

## Web (Compose Multiplatform, Kotlin/Wasm)

    gradlew :web:wasmJsBrowserDevelopmentRun      # dev server with live reload, opens the browser
    gradlew :web:wasmJsBrowserDistribution        # -> web/build/dist/wasmJs/productionExecutable/

The production folder is a static site: upload it to any static host (GitHub Pages, Netlify, nginx…).
It must be served over HTTP. Opening `index.html` from disk won't load the `.wasm` files.
It needs a browser with WebAssembly GC support (current Chrome, Edge, Firefox and Safari 18.2+).

Layout of `web/src/wasmJsMain/kotlin/com/cacao/memorium/`:

- `Main.kt` / `App.kt` — entry point, asset loading, portrait viewport (letterboxed on wide screens), screen switching
- `LogoScreen` → `MenuScreen` (title / settings / records panels in one vertical strip) → `GameScreen`
- `Scene.kt` — absolute-positioning primitives (`Stage`, `Sprite`, `Label`, `pressable`), libGDX easings
- `BitmapFont.kt` — renders the original BMFont (`font/b.fnt`, Latin + Cyrillic)
- `Audio.kt` (HTML audio), `Settings.kt` (`localStorage`, same keys as the Android preferences), `Strings.kt` (en / ru by browser language)

Textures, font and sounds are not copied: the web module serves `app/src/main/assets` as its resources.

Web-specific behaviour:

- swipe with a finger or the mouse, or use the mouse wheel, to switch menu panels; `Esc` works like Android's Back
- browsers block sound until the first click or key press, so the music starts after the first interaction

## Android (libGDX)

Open the folder in Android Studio, or run:

    gradlew :app:assembleDebug            # -> app/build/outputs/apk/debug/app-debug.apk
    gradlew :app:installDebug             # install on a connected phone

- `app/src/main/java/com/cacao/libgdxgame/` — game code
  - `MainActivity` — Android entry point
  - `MyGame` — creates the screens, music, shared atlas
  - `LogoScreen` → `FirstScreen` (menu: title / settings / records panels) → `GameScreen`
  - `LanguagesManager` — strings from `assets/data/languages.xml` (en / ru)
- `app/src/main/assets/` — textures, font, sounds (from the original APK)

Changes vs. the original:

- libGDX 1.0.0 → 1.14.2, targetSdk 18 → 36, added 64-bit native libs (arm64-v8a, x86_64)
- AdMob removed (old Play Services SDK); `MainActivity.showAdMob()` is a no-op stub
- Records persist under the same preferences keys (`mySettings`)
