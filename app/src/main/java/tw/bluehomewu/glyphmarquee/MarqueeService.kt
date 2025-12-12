package tw.bluehomewu.glyphmarquee

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphToy

class MarqueeService : Service() {

    private lateinit var glyphManager: GlyphMatrixManager
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "NothingToy"

    // 設定變數
    private var textToScroll = " HELLO NOTHING (3) "
    private var updateSpeed = 100L
    private var brightness = 255
    private var direction = 0 // 0:Left, 1:Right, 2:Up, 3:Down

    // 使用 scrollOffset 替代 scrollX，因為它現在可能代表 X 也可能代表 Y
    private var scrollOffset = 0
    private lateinit var textBitmap: Bitmap
    private val lock = Any()

    private var isBoundBySystem = false

    // AOD 訊息處理器
    private val serviceHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val msgWhat = try { GlyphToy.MSG_GLYPH_TOY } catch (e: Exception) { 1 }

            if (msg.what == msgWhat) {
                val bundle = msg.data
                val key = try { GlyphToy.MSG_GLYPH_TOY_DATA } catch (e: Exception) { "glyph_toy_data" }
                val event = bundle.getString(key)
                val aodEvent = try { GlyphToy.EVENT_AOD } catch (e: Exception) { "aod" }

                if (aodEvent == event) {
                    Log.d(TAG, "Received AOD Event - Triggering Animation")

                    // 1. 強制從 SharedPreferences 重新讀取最新的方向、文字、亮度
                    loadSettings()

                    // 2. 觸發動畫
                    triggerAodAnimation()
                }
            }
            super.handleMessage(msg)
        }
    }
    private val serviceMessenger = Messenger(serviceHandler)

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            Log.d(TAG, "Glyph Matrix Service Connected")
            Thread {
                try {
                    glyphManager.register("23112")
                    Thread.sleep(100)
                    loadSettings()

                    if (isBoundBySystem) {
                        handler.post { startMarquee() }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Registration Error", e)
                }
            }.start()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d(TAG, "Disconnected")
            stopMarquee()
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            glyphManager = GlyphMatrixManager.getInstance(applicationContext)
            glyphManager.init(callback)
            loadSettings()
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "System Bound (Toy Selected / AOD Active)")
        isBoundBySystem = true
        startMarquee()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "System Unbound")
        isBoundBySystem = false
        stopMarquee()
        turnOffLights()
        return true
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "System Rebound")
        isBoundBySystem = true
        startMarquee()
        super.onRebind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_CONFIG") {
            Log.d(TAG, "Config Updated")
            loadSettings()
            if (!isRunning && isBoundBySystem) {
                startMarquee()
            }
        }
        return START_STICKY
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("MarqueePrefs", Context.MODE_PRIVATE)
        textToScroll = prefs.getString("text", " HELLO NOTHING (3) ") ?: " ERROR "
        updateSpeed = prefs.getInt("speed", 100).toLong()
        brightness = prefs.getInt("brightness", 255)
        // 讀取方向
        direction = prefs.getInt("direction", 0)

        synchronized(lock) {
            scrollOffset = 0
            prepareTextBitmap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        turnOffLights()
        try {
            glyphManager.unInit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopMarquee()
    }

    private fun turnOffLights() {
        try {
            val blackFrame = IntArray(625) { 0 }
            if (::glyphManager.isInitialized) {
                glyphManager.setMatrixFrame(blackFrame)
            }
        } catch (e: Exception) {}
    }

    private fun prepareTextBitmap() {
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        // 取得精確的文字寬度 (包含最後面的空格)
        val textWidth = paint.measureText(textToScroll).toInt()
        val height = 25

        // 防止寬度過小 (至少 1 pixel)
        val finalWidth = if (textWidth > 0) textWidth else 1

        // 1. 先畫出標準的橫向文字圖片 (Base Bitmap)
        val baseBitmap = Bitmap.createBitmap(finalWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(baseBitmap)
        canvas.drawColor(Color.BLACK)

        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)

        // 只畫一次文字
        canvas.drawText(textToScroll, 0f, yPos, paint)

        // 2. 根據方向進行旋轉
        if (direction == 2 || direction == 3) {
            // 如果是 Up/Down 模式，將圖片旋轉 90 度
            // 這樣文字就會變成直排，適合手機直立時閱讀
            val matrix = Matrix()

            // ==========================================
            // ⚠️ 修正點：區分上下方向的旋轉角度
            // ==========================================
            if (direction == 2) {
                // 向上 (Up)：順時針 90 度
                matrix.postRotate(90f)
            } else {
                // 向下 (Down)：逆時針 90 度 (-90)
                matrix.postRotate(-90f)
            }

            // 建立新的旋轉後 Bitmap
            // 注意：原本的 寬x高 變成 高x寬
            try {
                // 建立旋轉後的直向圖片
                textBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)
            } catch (e: Exception) {
                // 如果旋轉失敗，就用原本的
                textBitmap = baseBitmap
            }
        } else {
            // 左右方向不旋轉
            textBitmap = baseBitmap
        }

        Log.d(TAG, "Bitmap Updated for AOD/Normal. Dir=$direction")
    }

    private fun triggerAodAnimation() {
        stopMarquee()
        Log.d(TAG, "Starting AOD Sequence")
        isRunning = true
        handler.post(marqueeRunnable)
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            try {
                synchronized(lock) {
                    if (::textBitmap.isInitialized) {
                        val matrixData = IntArray(625)
                        val bmpWidth = textBitmap.width
                        val bmpHeight = textBitmap.height

                        for (y in 0 until 25) {
                            for (x in 0 until 25) {
                                var fetchX = 0
                                var fetchY = 0

                                // ==========================================
                                // 根據方向讀取像素
                                // ==========================================
                                when (direction) {
                                    // 橫向模式 (圖片寬度很長，高度固定 25)
                                    0 -> { // Left: X 軸遞增
                                        fetchX = (scrollOffset + x) % bmpWidth
                                        fetchY = y
                                    }
                                    1 -> { // Right: X 軸遞減 (反向)
                                        // (bmpWidth - ...) 確保是正數索引
                                        fetchX = (bmpWidth - (scrollOffset % bmpWidth) + x) % bmpWidth
                                        fetchY = y
                                    }

                                    // 直向模式 (圖片已旋轉，寬度固定 25，高度很長)
                                    // 注意：這時我們的 "視窗" 是在 Y 軸上移動
                                    2 -> { // Up (順時針 90 度後，Y 軸遞增)
                                        fetchX = x
                                        fetchY = (scrollOffset + y) % bmpHeight
                                    }
                                    3 -> { // Down (逆時針 90 度後，Y 軸遞減)
                                        fetchX = x
                                        // 反向捲動邏輯
                                        fetchY = (bmpHeight - (scrollOffset % bmpHeight) + y) % bmpHeight
                                    }
                                }

                                // 讀取像素並填入 Matrix
                                // 確保座標在範圍內 (對於 Up/Down，X 應該在 0-24 內；對於 Left/Right，Y 應該在 0-24 內)
                                if (fetchX < bmpWidth && fetchY < bmpHeight) {
                                    val pixel = textBitmap.getPixel(fetchX, fetchY)
                                    val pixelBrightness = if (Color.red(pixel) > 50) brightness else 0
                                    matrixData[y * 25 + x] = pixelBrightness
                                } else {
                                    matrixData[y * 25 + x] = 0
                                }
                            }
                        }

                        val frame = GlyphMatrixFrame.Builder()
                            .addTop(matrixData)
                            .build(applicationContext)

                        if (::glyphManager.isInitialized) {
                            glyphManager.setMatrixFrame(frame)
                        }

                        scrollOffset += 1
                        // 防止整數溢位，雖然很久才會發生
                        if (scrollOffset > 1000000) scrollOffset = 0
                    }
                }

                handler.postDelayed(this, updateSpeed)

            } catch (e: Exception) {
                Log.e(TAG, "Animation Error: ${e.message}")
            }
        }
    }

    private fun startMarquee() {
        if (!isRunning) {
            isRunning = true
            handler.removeCallbacks(marqueeRunnable)
            handler.post(marqueeRunnable)
        }
    }

    private fun stopMarquee() {
        if (isRunning) {
            isRunning = false
            handler.removeCallbacks(marqueeRunnable)
        }
    }
}