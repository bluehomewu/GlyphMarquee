# Nothing Phone Glyph Marquee Toy 📱✨

> 🌐 **語言 / Language**：[English](README.md) | 繁體中文

![Version](https://img.shields.io/badge/Version-v1.5.0-blue)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Device](https://img.shields.io/badge/Device-Nothing_Phone_(3)_|_Phone_(4a)_Pro-red)
![SDK](https://img.shields.io/badge/GlyphMatrix_SDK-2.0-orange)

這是一個專為 **Nothing Phone (3)** 與 **Nothing Phone (4a) Pro** 設計的客製化 **Glyph Toy**，使用官方 Glyph Matrix SDK 2.0 開發。
將手機背面的 LED 點陣矩陣變身為完全可自訂的「跑馬燈 (Running Text)」顯示器。

| 裝置 | 矩陣尺寸 | 支援功能 |
|------|---------|---------|
| Nothing Phone (3) | 25×25 | Glyph Toy 全功能 + AOD |
| Nothing Phone (4a) Pro | 13×13 | AOD only |

## ✨ 功能特色 (Features)

*   **多裝置支援**：
    *   自動偵測裝置（`Common.getDeviceMatrixLength()`），對應 `Glyph.DEVICE_23112`（Phone 3）或 `Glyph.DEVICE_25111p`（Phone 4a Pro）。
    *   渲染尺寸、文字大小皆依矩陣大小動態調整，無硬編碼。
*   **流暢跑馬燈**：在 Glyph Matrix 上呈現滑順的文字/符號/Emoji 捲動效果。
*   **完全客製化**：
    *   📝 **自訂內容**：可輸入文字、符號或 Emoji。Phone (4a) Pro（13×13）建議使用符號或 Emoji 以獲得最佳顯示效果。
    *   ⚡ **調整速度**：捲動速度 10ms–200ms。
    *   💡 **調整亮度**：LED 亮度 0–255。
    *   🔄 **捲動方向**：支援左、右、上、下四個方向。
*   **支援 AOD (Always-On Display)**：
    *   兩款裝置均支援 Always-On Display 模式，螢幕鎖定時由系統週期性觸發動畫。
    *   Phone (4a) Pro 為 AOD 專屬裝置。
    *   ⏱ **AOD 計時自動關閉**：可設定 AOD 跑馬燈在執行一段時間後自動熄滅（1 / 2 / 5 / 10 / 30 分鐘，或永遠顯示），節省電量。下次喚醒螢幕後再次進入 AOD 時，會重新執行。
*   **智慧生命週期管理**：
    *   切換至其他玩具時自動暫停並讓出控制權。
    *   AOD 結束或玩具被取消選取時熄燈並停止動畫。
*   **雙語介面**：
    *   內建 **繁體中文 (Traditional Chinese)** 與 **英文 (English)**，依系統語言自動切換。

## 🛠 事前準備 (Prerequisites)

*   **硬體**：Nothing Phone (3)（`Glyph.DEVICE_23112`）或 Nothing Phone (4a) Pro（`Glyph.DEVICE_25111p`）。
*   **軟體**：Android Studio（建議 Ladybug 或更新版本）。
*   **Build Tools**：AGP 9.2.0、Kotlin 2.2.10、Gradle 9.4.1。
*   **SDK**：`GlyphMatrixSDK.aar` v2.0（取自 [Nothing Developer Programme](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)）。

## 🚀 安裝與設定 (Installation & Setup)

1.  **複製專案 (Clone)**：
    ```bash
    git clone https://github.com/bluehomewu/GlyphMarquee.git
    ```

2.  **加入 SDK**：
    *   確認 `app/libs/GlyphMatrixSDK.aar`（SDK 2.0）已存在於專案中。
    *   如需更新，從 [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) 下載最新 `.aar` 並替換。

3.  **同步 Gradle**：
    *   用 Android Studio 開啟專案。
    *   點擊 "Sync Project with Gradle Files"。

4.  **編譯並執行**：
    *   連接你的 Nothing Phone (3) 或 Nothing Phone (4a) Pro。
    *   執行 App（`Shift + F10`）。

5.  **啟用玩具（Phone (3) 專用）**：
    *   進入 **設定 → Glyph Interface → Glyph Toys**，將「Glyph 跑馬燈」加入玩具列表。
    *   也可在 App 內點擊 **「Open Glyph Toys Manager」** 按鈕直接跳轉。

6.  **啟用 AOD（Phone (4a) Pro）**：
    *   進入 **設定 → Glyph Interface → Flip to Glyph → Always-on Glyph Toy**，選擇此玩具。

## 📖 使用說明 (Usage)

1.  **開啟 App**：從應用程式抽屜打開「Glyph 跑馬燈」。
2.  **設定參數**：
    *   輸入要顯示的文字、符號或 Emoji。
    *   Phone (4a) Pro 上輸入框會提示建議使用符號或 Emoji（13×13 矩陣無法清楚呈現文字）。
    *   調整**捲動速度**、**亮度**、**方向**與 **AOD 自動關閉時間**。
3.  **套用**：點擊 **「套用並儲存」**。
4.  **享受成果**：翻轉手機至背面，或鎖定螢幕觸發 AOD。

## 🏗 技術架構 (Technical Architecture)

### 1. `MarqueeService.kt`（核心服務）
*   **角色**：處理 Glyph Matrix 邏輯的背景 `Service`。
*   **裝置偵測**：`onServiceConnected` 呼叫 `Common.getDeviceMatrixLength()` 取得矩陣邊長（Phone 3 = 25，Phone 4a Pro = 13），並以 `Glyph.DEVICE_23112` / `Glyph.DEVICE_25111p` 註冊。
*   **繪圖邏輯**（SDK 2.0 API）：
    *   以 `Canvas`/`Paint` 在虛擬 Bitmap 上繪製文字，`textSize = matrixLength × 0.8f` 自動縮放。
    *   每幀依 `scrollOffset` 從 Bitmap 採樣 `matrixLength×matrixLength` 視窗，轉為 `IntArray`。
    *   像素數據包成 `Bitmap` → `GlyphMatrixObject.Builder().setImageSource(bitmap).build()` → `GlyphMatrixFrame.Builder().addTop(obj).build(context)` → `setMatrixFrame(frame)`。
*   **生命週期**：
    *   `onBind`：啟動動畫迴圈（Phone 3 直接觸發；Phone 4a Pro 由 AOD 事件驅動）。
    *   `onUnbind`：返回 `false`；500 ms 延遲確認後停止動畫並熄燈（防止 AOD 過渡期短暫重連造成閃爍）。
    *   `ACTION_SCREEN_ON` BroadcastReceiver：Nothing 系統不在每次螢幕喚醒時呼叫 `onUnbind`，因此改以廣播偵測螢幕亮起，重置 AOD timeout 狀態，確保下次休眠時正常執行。
*   **AOD**：透過 `Messenger` 監聽 `GlyphToy.EVENT_AOD`。系統約每分鐘發送一次事件；僅第一次事件啟動動畫與計時器，後續重複事件在計時器倒數期間被忽略（防止畫面跳回起點）。
*   **AOD 計時關閉**：
    *   `aodTimeoutRunnable`：計時結束後停止動畫並熄燈，同時設置 `aodTimedOut = true` 封鎖同一 session 的後續 AOD 事件。
    *   `aodTimeoutScheduled` flag：防止重複 AOD 事件重排計時器。
    *   `aodTimedOut` flag：timeout 後封鎖所有 AOD 事件，直到 `ACTION_SCREEN_ON` 重置。

### 2. `MainActivity.kt`（使用者介面）
*   啟動時呼叫 `Common.getDeviceMatrixLength()` 一次，以 `isPhone4aPro` 統一控制：
    *   輸入框 helper text 提示（Phone 4a Pro 才顯示）。
    *   預設文字：Phone 3 顯示 `HELLO NOTHING (3)`，Phone 4a Pro 顯示 `HELLO NOTHING (4a) Pro`。
    *   「Open Glyph Toys Manager」按鈕（Phone 4a Pro 隱藏，因不支援完整 Glyph Toys 功能）。
*   使用 `SharedPreferences` 持久化儲存設定（文字、速度、亮度、方向、AOD 自動關閉時間）。
*   發送 `UPDATE_CONFIG` Intent 通知背景 Service 即時更新。

### 3. `AndroidManifest.xml`（註冊資訊）
*   註冊 Service 並設定 Metadata，讓系統識別為 Glyph Toy。
*   **關鍵 Metadata**：
    *   `com.nothing.glyph.toy.name`：玩具顯示名稱。
    *   `com.nothing.glyph.toy.image`：圓形預覽圖示。
    *   `com.nothing.glyph.toy.aod_support = 1`：啟用 AOD 支援（Phone 4a Pro 必要）。
    *   `NothingKey`：SDK 授權金鑰。

## 📦 版本記錄 (Changelog)

### v1.3.0
*   **新增**：AOD 計時自動關閉（1 / 2 / 5 / 10 / 30 分鐘或永遠顯示），節省電量；下次喚醒螢幕後重新進入 AOD 時會重新執行。
*   **修正**：AOD 動畫例外發生後動畫永久停止但 GlyphMatrix 仍亮著——改用 `finally` 確保 `postDelayed` 一定執行。
*   **修正**：AOD 過渡期系統短暫重連造成畫面閃爍與像素位移——`onUnbind` 改為 500 ms 延遲確認後才停止，`triggerAodAnimation()` 加入 200 ms 延遲讓 GlyphMatrix 服務穩定後再推送第一幀。
*   **修正**：AOD 系統每分鐘重複發送事件導致動畫跳回開頭——計時器已啟動時忽略後續重複事件，不重啟動畫。
*   **修正**：AOD timeout 觸發後動畫又自行重啟——加入 `aodTimedOut` flag，timeout 後封鎖同一 session 的所有 AOD 事件。
*   **修正**：Nothing 系統不在每次螢幕喚醒時呼叫 `onUnbind`，導致 timeout flag 永不重置、後續 AOD session 無法顯示——改用 `ACTION_SCREEN_ON` BroadcastReceiver 偵測螢幕亮起並重置狀態。

### v1.2.0
*   **新增**：支援 Nothing Phone (4a) Pro（`Glyph.DEVICE_25111p`，13×13 矩陣，AOD only）。
*   **新增**：Phone (4a) Pro 輸入框提示（建議使用符號或 Emoji）。
*   **新增**：裝置專屬預設跑馬燈文字。
*   **新增**：「Open Glyph Toys Manager」按鈕（Phone 3 限定）。
*   **升級**：GlyphMatrix SDK 1.x → **2.0**（新增 `GlyphMatrixObject`、`GlyphMatrixFrameWithMarquee`、`GlyphMatrixUtils`）。
*   **升級**：AGP 8.x → 9.1.0、Kotlin 2.0.x → 2.2.10、Gradle 8.x → 9.3.1。
*   **修正**：渲染 API 改用 `GlyphMatrixObject`（SDK 2.0 文件化 API），取代 `addTop(IntArray)`。
*   **修正**：文字大小改為 `matrixLength × 0.8f`，確保 Emoji 完整顯示於矩陣內。

### v1.1.1
*   新增捲動方向控制（上、下、左、右）。
*   新增亮度控制。
*   加入 Lottie 動畫預覽支援（Nothing Playground）。

## 🤝 貢獻 (Contribution)

歡迎 Fork 此專案並提交 Pull Requests！

## 📄 授權 (License)

本專案為開源專案。
*注意：GlyphMatrix SDK 版權歸 Nothing Technology Limited 所有。*

---
*Built with ❤️ for the Nothing Community.*
