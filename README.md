# Nothing Phone (3) Glyph Marquee Toy 📱✨

![Project Status](https://img.shields.io/badge/Status-Completed-success)
![Platform](https://img.shields.io/badge/Platform-Android-green)
![Device](https://img.shields.io/badge/Device-Nothing_Phone_(3)-red)

這是一個專為 **Nothing Phone (3)** 設計的客製化 **Glyph Toy**，使用了官方的 Glyph Matrix SDK 開發。
此專案能將手機背面的 25x25 LED 點陣矩陣，變身為一個完全可自訂的「跑馬燈 (Running Text)」顯示器。

## ✨ 功能特色 (Features)

*   **流暢跑馬燈**：在 25x25 Glyph Matrix 上呈現滑順的文字捲動效果。
*   **完全客製化**：
    *   📝 **自訂文字**：可輸入任何文字（建議使用英文或數字以獲得最佳顯示效果）。
    *   ⚡ **調整速度**：可拖拉滑桿調整捲動速度 (10ms - 200ms)。
    *   💡 **調整亮度**：可控制 LED 的亮度強弱 (0 - 255)。
*   **支援 AOD (隨顯螢幕)**：
    *   支援 Always-On Display 模式，當螢幕鎖定或關閉時仍可持續運作。
    *   提供連續播放模式，增添手機待機時的視覺風格。
*   **智慧生命週期管理**：
    *   當切換至其他玩具（如相機、計時器）時，會自動暫停並讓出控制權。
    *   防止背景執行時的資源佔用與燈光閃爍衝突。
*   **雙語介面**：
    *   內建 **繁體中文 (Traditional Chinese)** 與 **英文 (English)**，依據系統語言自動切換。

## 🛠 事前準備 (Prerequisites)

*   **硬體**：Nothing Phone (3) (Device ID: `23112`)。
*   **軟體**：Android Studio (建議使用 Ladybug 或更新版本)。
*   **SDK**：`GlyphMatrixSDK.aar` (需從 [Nothing Developer Programme](https://github.com/Nothing-Developer-Programme) 取得)。

## 🚀 安裝與設定 (Installation & Setup)

1.  **複製專案 (Clone)**：
    ```bash
    git clone https://github.com/bluehomewu/GlyphMarquee.git
    ```

2.  **加入 SDK**：
    *   在專案中建立 `app/libs/` 資料夾。
    *   將下載好的 `GlyphMatrixSDK.aar` 檔案放入此資料夾。

3.  **同步 Gradle**：
    *   用 Android Studio 開啟專案。
    *   點擊 "Sync Project with Gradle Files"。

4.  **編譯並執行**：
    *   連接你的 Nothing Phone (3)。
    *   執行 App (`Shift + F10`).

5.  **啟用玩具**：
    *   在手機上進入 **設定 (Settings)** -> **Glyph Interface** -> **Glyph Toys** (或 Library)。
    *   在清單中找到 **"Glyph 跑馬燈"** (Glyph Marquee) 並將其加入常用的玩具列表中。

## 📖 使用說明 (Usage)

1.  **開啟 App**：從應用程式抽屜打開 "Glyph 跑馬燈"。
2.  **設定參數**：
    *   輸入你想要顯示的文字。
    *   調整 **捲動速度** (向左越快，向右越慢)。
    *   調整 **亮度控制**。
3.  **套用**：點擊 **"套用並儲存"** 按鈕。
4.  **享受成果**：將手機翻轉至背面！
5.  **AOD 模式**：鎖定你的手機螢幕，跑馬燈將會持續運作（請確認系統設定已開啟 AOD 功能）。

## 🏗 技術架構 (Technical Architecture)

### 1. `MarqueeService.kt` (核心服務)
*   **角色**：負責處理 Glyph Matrix 邏輯的背景 `Service`。
*   **管理器**：使用 `GlyphMatrixManager` 與硬體溝通。
*   **繪圖邏輯**：
    *   使用 Android 標準的 `Canvas` 與 `Paint` 在虛擬 `Bitmap` 上繪製文字。
    *   將 Bitmap 的像素點轉換為 25x25 的 `IntArray` (灰階亮度值 0-255)。
    *   透過 `glyphManager.setMatrixFrame()` 發送畫面數據。
*   **生命週期**：
    *   `onBind`: 當玩具被選中時，啟動動畫迴圈。
    *   `onUnbind`: 當玩具被取消選取時，停止迴圈並熄燈，讓出控制權給其他玩具。
*   **AOD**：透過 `Messenger` 監聽 `GlyphToy.EVENT_AOD` 事件，在螢幕關閉時觸發動畫。

### 2. `MainActivity.kt` (使用者介面)
*   提供友善的設定介面。
*   使用 `SharedPreferences` 持久化儲存使用者的設定（文字、速度、亮度）。
*   發送帶有 `UPDATE_CONFIG` Action 的 Intent 來通知背景 Service 即時更新。

### 3. `AndroidManifest.xml` (註冊資訊)
*   註冊 Service 並設定特定的 Metadata，讓系統能識別這是一個 **Glyph Toy**。
*   **關鍵 Metadata**：
    *   `com.nothing.glyph.toy.name`: 玩具顯示名稱。
    *   `com.nothing.glyph.toy.image`: 圓形預覽圖示。
    *   `com.nothing.glyph.toy.aod_support`: 啟用 AOD 支援。
    *   `NothingKey`: SDK 授權金鑰。

## 🎨 圖示設計 (Icon Design)
本專案包含一個客製化設計的圓形預覽圖示 (`toy_preview.png`)，模擬 Nothing 的點陣風格與硬體排列，確保在系統選單中看起來原汁原味。

## 🤝 貢獻 (Contribution)

歡迎 Fork 此專案並提交 Pull Requests！
未來可能的改進方向：
- [x]   新增捲動方向控制（上下左右）。

## 📄 授權 (License)

本專案為開源專案。
*注意：GlyphMatrix SDK 版權歸 Nothing Technology Limited 所有。*

---
*Built with ❤️ for the Nothing Community.*