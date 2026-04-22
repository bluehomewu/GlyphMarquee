# Nothing Phone Glyph Marquee Toy 📱✨

> 🌐 **Language / 語言**：English | [繁體中文](README_zhTW.md)

![Version](https://img.shields.io/badge/Version-v1.5.2-blue)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Device](https://img.shields.io/badge/Device-Nothing_Phone_(3)_|_Phone_(4a)_Pro-red)
![SDK](https://img.shields.io/badge/GlyphMatrix_SDK-2.0-orange)

A custom **Glyph Toy** built for **Nothing Phone (3)** and **Nothing Phone (4a) Pro** using the official Glyph Matrix SDK 2.0.
Turns the LED dot-matrix panel on the back of your phone into a fully customisable **scrolling text (marquee)** display.

| Device | Matrix Size | Supported Modes |
|--------|-------------|-----------------|
| Nothing Phone (3) | 25×25 | Full Glyph Toy + AOD |
| Nothing Phone (4a) Pro | 13×13 | AOD only |

## ✨ Features

*   **Multi-device support**:
    *   Auto-detects the device via `Common.getDeviceMatrixLength()`, registering as `Glyph.DEVICE_23112` (Phone 3) or `Glyph.DEVICE_25111p` (Phone 4a Pro).
    *   Render size and font size are dynamically scaled to the matrix — no hardcoded values.
*   **Smooth marquee**: Renders fluid text / symbol / Emoji scrolling on the Glyph Matrix.
*   **Fully customisable**:
    *   📝 **Custom content**: Enter any text, symbol, or Emoji. Phone (4a) Pro (13×13) — symbols or Emoji recommended for best legibility.
    *   ⚡ **Scroll speed**: 10 ms – 200 ms per frame.
    *   💡 **Brightness**: LED brightness 0 – 255.
    *   🔄 **Scroll direction**: Left, Right, Up, or Down.
*   **Always-On Display (AOD) support**:
    *   Both devices support AOD mode — the system periodically triggers the animation while the screen is locked.
    *   Phone (4a) Pro is AOD-only.
    *   ⏱ **AOD auto-off timer**: Configure the marquee to stop after a set duration (1 / 2 / 5 / 10 / 30 minutes, or Always On) to save battery. Resets each time the screen wakes and AOD starts again.
*   **Smart lifecycle management**:
    *   Automatically pauses and releases control when another toy is selected.
    *   Turns off LEDs and stops animation when AOD ends or the toy is deselected.
*   **Bilingual UI**:
    *   Supports **Traditional Chinese** and **English**, switching automatically with the system language.

## 🛠 Prerequisites

*   **Hardware**: Nothing Phone (3) (`Glyph.DEVICE_23112`) or Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`).
*   **Software**: Android Studio (Ladybug or later recommended).
*   **Build Tools**: AGP 9.2.0, Kotlin 2.2.10, Gradle 9.4.1.
*   **SDK**: `GlyphMatrixSDK.aar` v2.0 (from the [Nothing Developer Programme](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)).

## 🚀 Installation & Setup

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/bluehomewu/GlyphMarquee.git
    ```

2.  **Add the SDK**:
    *   Verify that `app/libs/GlyphMatrixSDK.aar` (SDK 2.0) exists in the project.
    *   To update, download the latest `.aar` from [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) and replace the existing file.

3.  **Sync Gradle**:
    *   Open the project in Android Studio.
    *   Click **"Sync Project with Gradle Files"**.

4.  **Build & Run**:
    *   Connect your Nothing Phone (3) or Nothing Phone (4a) Pro.
    *   Run the app (`Shift + F10`).

5.  **Enable the toy (Phone (3) only)**:
    *   Go to **Settings → Glyph Interface → Glyph Toys** and add "Glyph Marquee" to the toy list.
    *   Alternatively, tap **"Open Glyph Toys Manager"** inside the app.

6.  **Enable AOD (Phone (4a) Pro)**:
    *   Go to **Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy** and select this toy.

## 📖 Usage

1.  **Open the app** from the app drawer.
2.  **Configure**:
    *   Enter the text, symbol, or Emoji to display.
    *   On Phone (4a) Pro the input field shows a hint recommending symbols or Emoji (the 13×13 matrix cannot clearly render text).
    *   Adjust **scroll speed**, **brightness**, **direction**, and **AOD auto-off timer**.
3.  **Apply**: Tap **"Apply & Save"**.
4.  **Enjoy**: Flip the phone face-down, or lock the screen to trigger AOD.

## 🏗 Technical Architecture

### 1. `MarqueeService.kt` (Core Service)
*   **Role**: A background `Service` handling all Glyph Matrix logic.
*   **Device detection**: `onServiceConnected` calls `Common.getDeviceMatrixLength()` to get the matrix side length (Phone 3 = 25, Phone 4a Pro = 13) and registers with `Glyph.DEVICE_23112` / `Glyph.DEVICE_25111p`.
*   **Rendering** (SDK 2.0 API):
    *   Draws text onto a virtual `Bitmap` via `Canvas`/`Paint`; `textSize = matrixLength × 0.8f` auto-scales.
    *   Each frame, samples a `matrixLength×matrixLength` window from the bitmap at `scrollOffset`, converts to `IntArray`.
    *   Pixel data is packed as `Bitmap` → `GlyphMatrixObject.Builder().setImageSource(bitmap).build()` → `GlyphMatrixFrame.Builder().addTop(obj).build(context)` → `setMatrixFrame(frame)`.
*   **Lifecycle**:
    *   `onBind`: Starts the animation loop (Phone 3 triggers immediately; Phone 4a Pro is driven by AOD events).
    *   `onUnbind`: Returns `false`; stops animation and turns off LEDs after a 500 ms delay (prevents flicker caused by brief system reconnects during AOD transitions).
    *   `ACTION_SCREEN_ON` BroadcastReceiver: Nothing's system does not call `onUnbind` on every screen wake, so a broadcast receiver detects screen-on and resets the AOD timeout state.
*   **AOD**: Listens to `GlyphToy.EVENT_AOD` via `Messenger`. The system sends this event roughly once per minute; only the first event starts the animation and timer — subsequent events are ignored while the timer is running (prevents the animation from jumping back to the start).
*   **AOD auto-off**:
    *   `aodTimeoutRunnable`: Stops animation and turns off LEDs when the timer expires; sets `aodTimedOut = true` to block further AOD events in the same session.
    *   `aodTimeoutScheduled` flag: Prevents duplicate AOD events from restarting the timer.
    *   `aodTimedOut` flag: Blocks all AOD events after timeout until `ACTION_SCREEN_ON` resets it.

### 2. `MainActivity.kt` (UI)
*   Calls `Common.getDeviceMatrixLength()` once at startup; the `isPhone4aPro` flag controls:
    *   Input field helper text (Phone 4a Pro only).
    *   Default marquee text: Phone 3 → `HELLO NOTHING (3)`, Phone 4a Pro → `HELLO NOTHING (4a) Pro`.
    *   "Open Glyph Toys Manager" button (hidden on Phone 4a Pro, which does not support the full Glyph Toys UI).
*   Persists settings (text, speed, brightness, direction, AOD timer) via `SharedPreferences`.
*   Sends `UPDATE_CONFIG` Intent to the background Service for live updates.

### 3. `AndroidManifest.xml` (Registration)
*   Registers the Service and sets Metadata so the system recognises it as a Glyph Toy.
*   **Key Metadata**:
    *   `com.nothing.glyph.toy.name`: Toy display name.
    *   `com.nothing.glyph.toy.image`: Circular preview icon.
    *   `com.nothing.glyph.toy.aod_support = 1`: Enables AOD support (required for Phone 4a Pro).
    *   `NothingKey`: SDK licence key.

## 📦 Changelog

### v1.5.2
*   **Fix**: Restore Nothing Phone (3) screen-on marquee behavior by recovering the animation after screen wake and using app-matrix rendering when the system does not rebind the toy service.

### v1.5.0
*   **New**: Professional UI redesign — `NestedScrollView` layout, `MaterialCardView` settings group, Material3 `Slider` replacing `SeekBar`, `ExposedDropdownMenu` replacing `Spinner`, `MaterialDivider` dividers, all hardcoded colours replaced with theme attributes.

### v1.4.1
*   **Fix**: Replaced deprecated `ProgressDialog` with an inline progress bar inside the release dialog.
*   **Fix**: Restored empty `dialog_release.xml` that caused a fatal XML parse build error.

### v1.4.0
*   **New**: In-app updater with Markdown release notes — tap the version label to check for updates; silent check on startup.
*   **New**: Markwon 4.6.2 + OkHttp 4.12.0 dependencies.
*   **Improvement**: Enabled R8 full-mode shrinking for release builds.
*   **i18n**: Synced Traditional Chinese strings; fixed English strings that were incorrectly written in Chinese.

### v1.3.0
*   **New**: AOD auto-off timer (1 / 2 / 5 / 10 / 30 minutes or Always On) to save battery; resets on next screen wake.
*   **Fix**: Animation permanently stopped after AOD exception but LEDs stayed on — `finally` block ensures `postDelayed` always runs.
*   **Fix**: Screen flicker and pixel shift caused by brief system reconnects during AOD transition — `onUnbind` now waits 500 ms before stopping; `triggerAodAnimation()` waits 200 ms before pushing the first frame.
*   **Fix**: System's repeated AOD events caused animation to jump back to the start — subsequent events are ignored while the timer is running.
*   **Fix**: `aodTimedOut` flag added to block AOD events after timeout within the same session.
*   **Fix**: Nothing's system does not call `onUnbind` on every screen wake, causing the timeout flag to never reset — replaced with `ACTION_SCREEN_ON` BroadcastReceiver.

### v1.2.0
*   **New**: Support for Nothing Phone (4a) Pro (`Glyph.DEVICE_25111p`, 13×13 matrix, AOD only).
*   **New**: Input hint for Phone (4a) Pro recommending symbols or Emoji.
*   **New**: Device-specific default marquee text.
*   **New**: "Open Glyph Toys Manager" button (Phone 3 only).
*   **Upgrade**: GlyphMatrix SDK 1.x → **2.0**.
*   **Upgrade**: AGP 8.x → 9.1.0, Kotlin 2.0.x → 2.2.10, Gradle 8.x → 9.3.1.
*   **Fix**: Rendering API updated to `GlyphMatrixObject` (SDK 2.0 documented API).
*   **Fix**: Font size changed to `matrixLength × 0.8f` to ensure Emoji fits within the matrix.

### v1.1.1
*   Added scroll direction control (Up, Down, Left, Right).
*   Added brightness control.
*   Added Lottie animation preview support (Nothing Playground).

## 🤝 Contributing

Fork this project and submit Pull Requests — contributions are welcome!

## 📄 License

This project is open source.
*Note: The GlyphMatrix SDK is copyright Nothing Technology Limited.*

---
*Built with ❤️ for the Nothing Community.*
