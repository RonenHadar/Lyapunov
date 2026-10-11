# Lyapunov — project notes for Claude

## What this is
An explorer for the Lyapunov (Markus–Lyapunov) fractal, written in Kotlin with Compose Multiplatform.
One shared codebase, two targets: **desktop (JVM)** and **Android**. Owner: Ronen Hadar (GitHub `RonenHadar`).

GitHub: https://github.com/RonenHadar/Lyapunov (branch `main`). The initial commit
`b5ec33c "Lyapunov fractal explorer for desktop and Android"` is already pushed there.

## Current state (important)
- The code was written in a cloud sandbox that could not download Gradle/Maven dependencies,
  so it has **never been compiled**. Expect some compile errors on the first build; fix them.
- This local folder was created by copying files, **not** by `git clone`, so it has no `.git` yet.
  To turn it into a checkout of the existing repo without losing files:
  ```
  git init -b main
  git remote add origin https://github.com/RonenHadar/Lyapunov.git
  git fetch origin
  git reset origin/main
  git branch -u origin/main
  ```
  `CLAUDE.md` (this file) is not in the repo yet; commit it.
- Machine: Windows (PowerShell). Needs JDK 17+ for Gradle. Android builds need the Android SDK
  (`local.properties` with `sdk.dir=...`, or open the project in Android Studio).

## Features the user asked for (v1)
1. Display the Lyapunov fractal on desktop and Android.
2. Create custom color palettes for displaying the fractal.
3. Zoom into areas of the fractal and redraw that area.
"This is for start" — more features will come.

## How it works
- Pixel = (a, b): a grows left→right, b grows bottom→top. Default region a,b ∈ [2, 4].
- Logistic map x → r·x·(1−x), r alternates a/b following a repeating A/B sequence (default "AB").
  200 warm-up iterations, then 600 (adjustable) to estimate the Lyapunov exponent λ.
- λ < 0 (stable): colored from the palette gradient via t = 1 − e^λ (0 = edge of chaos, 1 = most stable).
  λ ≥ 0 (chaos): the palette's chaos color.
- Rendering runs on all cores (coroutines, Dispatchers.Default): 8×8-block preview first, then full
  resolution; views over 2 MP render at lower resolution and scale up. Palette changes only recolor.

## Code layout (`composeApp/src/`)
- `commonMain/.../fractal/LyapunovRenderer.kt`: exponent math and the parallel renderer. `FractalParams` holds the sequence and iteration counts.
- `commonMain/.../fractal/Region.kt`: parameter-space rectangle: `fitAspect`, `select` (drag-zoom), `zoomAround`.
- `commonMain/.../palette/`: `Palette`/`ColorStop`/`Colorizer`, ARGB/HSV/hex helpers, `PaletteRepository`
  (4 built-ins: Classic, Ocean, Fire, Zircon; user palettes saved as text: `id|name|#CHAOS|pos:#HEX,...`).
- `commonMain/.../ui/`: `FractalView` (drag to zoom, double-tap 2×), `ControlPanel` (Back/Reset, sequence,
  iterations, palette list with New/Edit/Delete), `PaletteEditor` (stops + HSV sliders + hex).
- `commonMain/.../App.kt`: layout (side panel when width > 720dp, else panel below) and zoom history stack.
- `commonMain/.../Platform.kt`: `expect` for palette load/save and IntArray→ImageBitmap.
  Desktop: Skia raster, palettes in `~/.lyapunov/palettes.txt`. Android: `Bitmap`, app `filesDir`.
- `commonTest/.../LyapunovTest.kt`: math, region, codec, color tests.
- Package / applicationId: `org.armya.lyapunov` (matches the owner's older repo `PhysicalUnits`, `org.armya.physical_units`).

## Versions (`gradle/libs.versions.toml`)
Kotlin 2.1.21, Compose Multiplatform 1.8.2, AGP 8.7.3, Gradle wrapper 8.14.3, coroutines 1.10.2,
compileSdk/targetSdk 35, minSdk 24. Chosen as a known-compatible set; upgrade only after it builds.

## Commands
- Run desktop: `.\gradlew.bat :composeApp:run`
- Tests: `.\gradlew.bat :composeApp:desktopTest`
- Android debug install: `.\gradlew.bat :composeApp:installDebug`
- Desktop installer: `.\gradlew.bat :composeApp:packageDistributionForCurrentOS`

## Ideas not built yet
Save/export the image as PNG, presets/bookmarks for interesting regions, pinch-zoom and pan on Android,
GPU rendering, color cycling animation.
