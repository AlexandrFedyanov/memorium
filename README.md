# Memorium

**▶ Play it in the browser: https://alexandrfedyanov.github.io/memorium/**

A memory card game: flip two cards, find the pairs, beat your own records. It has doors that open,
sounds, records for score, time and moves, and an easy and a hard mode.

## The story

This game was my first attempt at programming. It was written in 2014, back when Kotlin wasn't a thing
on Android yet, phones were slow and clunky, and "architecture" meant one class with
everything in it.

Then the source code was lost without a trace. All that survived was one copy of the APK, found
somewhere in a dusty corner of a hard drive. It refused to install on any modern phone. So the
game was **decompiled** back into Java, patched until it built again, and brought back to life.

## Why it's on GitHub

To serve as a study aid: **how never, under any circumstances, to write code.** It's a hand-picked
collection of the worst programming practices, preserved for future generations in
`app/src/main/java/` (it has been tidied up slightly while being revived from the decompiler output,
so the real thing was even worse). Highlights:

- 🏷️ **Naming as an extreme sport.** Classes called `actor` and `dors` (lower case, and yes, "dors" means
  doors). A `CardListner`, a `MyAtorListener`, an array of `presedCards`, a `recordTabel`. The spell
  checker filed a complaint and was ignored.
- 🧠 **One class, one responsibility: everything.** `FirstScreen` is ~700 lines of layout maths,
  input handling, animations, persistence and menu navigation, all in one place.
- 🚩 **Magic numbers as the state machine.** `backFlag` is 0, 1 or 2, and `complexity` is 0 or 1. What
  they mean is written down in Javadoc comments that came later, so in 2014 you just had to remember.
- 🎯 **Touch handling by brute force.** Swiping between menu panels is decided by a chain of eight
  `if / else if` branches comparing `y` with `displayHeight / 15.0f`. It works, and nobody
  remembers why.
- 🔤 **Shared mutable font.** Pressing a button makes its text bigger by changing the scale of the *one
  font every label shares*. So pressing one button made all the other text on screen bigger too.
  That's not a bug, it's a feature called "global feedback".
- ⏱️ **Time travel.** The game timer starts on the *second* move and is adjusted at the end with
  `timer--` to make up for a one-second delay. Off-by-one errors are fixed with more off-by-one errors.
- 🎲 **Shuffling, the hopeful way.** Cards get their faces from `Math.random()` in a `do { } while`
  loop that keeps trying until it finds a picture that hasn't been used twice yet.
- 🔇 **The "music" button turns off all sound.** Every sound effect checks `isMusicOn`.
- 🧷 **Debouncing with timers.** After each tap, the listeners are removed from every card and put
  back 50 ms later by a scheduled task.
- 💸 **Ads.** The original showed an AdMob banner. That SDK is so old it no longer exists,
  so the ads went to a better place too.

If you're learning to code: read it, laugh, and then do the opposite. If you're interviewing me: I've
grown, I promise.

## What's in the repo

| | |
|---|---|
| `web/` | The game rewritten as a **Compose Multiplatform web app** (Kotlin/Wasm), deployed to GitHub Pages. A modern rewrite that keeps the original look, rules, animations and sounds. |
| `app/` | The resurrected original **libGDX Android app** (decompiled Java, updated to run on today's phones). The museum of bad practices described above. |

Both builds use the same assets in `app/src/main/assets/` (textures, the hand-drawn bitmap font,
sounds). They are the only part of the original that survived exactly as it was.

## Web version (Compose Multiplatform, Kotlin/Wasm)

    gradlew :web:wasmJsBrowserDevelopmentRun      # dev server with live reload, opens the browser
    gradlew :web:wasmJsBrowserDistribution        # static site -> web/build/dist/wasmJs/productionExecutable/

Every push to `main` builds the site and deploys it to GitHub Pages (`.github/workflows/pages.yml`).
It needs a browser with WebAssembly GC support (current Chrome, Edge, Firefox, Safari 18.2+).

Code in `web/src/wasmJsMain/kotlin/com/cacao/memorium/`:

- `App.kt` — asset loading, portrait viewport (letterboxed on wide screens), screen switching
- `LogoScreen` → `MenuScreen` (settings / title / records panels in one vertical strip) → `GameScreen`
- `Scene.kt` — absolute positioning (`Stage`, `Sprite`, `Label`, `pressable`) and the libGDX easing curves
- `BitmapFont.kt` — draws the original BMFont (Latin + Cyrillic)
- `Audio.kt` (HTML audio), `Settings.kt` (`localStorage`, same keys as the Android version), `Strings.kt` (en / ru)

Controls: tap or click the cards. Swipe, drag with the mouse or use the mouse wheel to switch menu
panels. `Esc` works like Android's Back button. Browsers block sound until you click once, so the
music starts after your first click.

## Android version (libGDX)

    gradlew :app:assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
    gradlew :app:installDebug       # install on a connected phone

What it took to bring it back:

- libGDX 1.0.0 → 1.14.2, targetSdk 18 → 36, added 64-bit native libraries (arm64-v8a, x86_64)
- AdMob removed; `MainActivity.showAdMob()` is now a stub that does nothing
- Records are still saved under the original preferences keys (`mySettings`), in case you have a
  2014 high score to protect
