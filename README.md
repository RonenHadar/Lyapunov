# Lyapunov

An explorer for the Lyapunov fractal (also called the Markus–Lyapunov fractal), written in Kotlin with
Compose Multiplatform. The same code runs as a desktop app and as an Android app.

## Features

- **Zoom:** drag a rectangle across the image to zoom into that area. Double-click (or double-tap on
  Android) zooms 2× on a point. **Back** returns to the previous view, **Reset** to the full fractal.
- **Color palettes:** four built-in palettes, plus your own. **New** makes an editable copy of the
  selected palette. In the editor you can rename it, add, remove and move color stops, and pick each
  color with hue / saturation / brightness sliders or a hex code. Your palettes are saved and come back
  the next time you open the app. Switching palettes recolors instantly, without recomputing.
- **Sequence and iterations:** change the A/B forcing sequence (try `BBBBBBAAAAAA` for "Zircon Zity")
  and the number of iterations used to estimate the exponent.
- A quick low-resolution preview appears first, then the full-resolution image. Rendering uses all
  CPU cores.

## How the fractal is made

Each pixel is a pair of numbers (a, b): a grows to the right, b grows upward. The logistic map
x → r·x·(1 − x) is iterated, with r taking the value a or b according to a repeating sequence of A's
and B's. The Lyapunov exponent λ measures whether nearby orbits converge (λ < 0, stable) or fly
apart (λ > 0, chaos). Stable pixels are colored from the palette's gradient (left end = edge of
chaos, right end = most stable); chaotic pixels get the palette's chaos color.

## Running

You need JDK 17 or newer. The first build downloads Gradle and the dependencies.

**Desktop**

```
./gradlew :composeApp:run
```

Package a native installer with `./gradlew :composeApp:packageDistributionForCurrentOS`.

**Android**

Open the project in Android Studio and run the `composeApp` configuration, or with a device connected:

```
./gradlew :composeApp:installDebug
```

The Android build needs the Android SDK. Android Studio sets it up; otherwise create
`local.properties` with `sdk.dir=/path/to/Android/sdk`.

**Tests**

```
./gradlew :composeApp:desktopTest
```

## Project layout

```
composeApp/src/
  commonMain/   shared code: fractal math, palettes, all of the UI
    fractal/    Lyapunov exponent, parallel renderer, zoom regions
    palette/    palette model, built-in palettes, saving, color helpers
    ui/         fractal view, control panel, palette editor
  desktopMain/  desktop entry point, image conversion, palette file (~/.lyapunov/palettes.txt)
  androidMain/  Android activity, image conversion, palette file (app storage)
  commonTest/   unit tests
```

## Versions

Kotlin 2.1.21, Compose Multiplatform 1.8.2, Android Gradle Plugin 8.7.3, Gradle 8.14.3. Versions are
set in `gradle/libs.versions.toml`.
