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
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
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
    // aodTimeoutMinutes == 0 表示永遠顯示
    private var aodTimeoutMinutes = 0

    // 裝置矩陣尺寸（Phone (3) = 25, Phone (4a) Pro = 13）
    private var matrixLength = 25

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
                    matrixLength = Common.getDeviceMatrixLength()
                    val deviceId = if (matrixLength == 13) Glyph.DEVICE_25111p else Glyph.DEVICE_23112
                    Log.d(TAG, "Device: $deviceId, Matrix: ${matrixLength}x${matrixLength}")
                    glyphManager.register(deviceId)
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

    // Mismatch 3: return false so system calls onBind() again on rebind (matches README)
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "System Unbound")
        isBoundBySystem = false
        // 延遲關閉，避免系統在 AOD 過渡期短暫重連時造成閃爍
        handler.postDelayed({
            if (!isBoundBySystem) {
                stopMarquee()
                turnOffLights()
            }
        }, 500)
        return false
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
        val defaultText = if (matrixLength == 13) " HELLO NOTHING (4a) Pro " else " HELLO NOTHING (3) "
        textToScroll = prefs.getString("text", defaultText) ?: " ERROR "
        updateSpeed = prefs.getInt("speed", 100).toLong()
        brightness = prefs.getInt("brightness", 255)
        direction = prefs.getInt("direction", 0)

        // aod_timeout_index: 0→1min, 1→2min, 2→5min, 3→10min, 4→30min, 5→0(forever)
        val timeoutIndex = prefs.getInt("aod_timeout_index", 5)
        val timeoutOptions = intArrayOf(1, 2, 5, 10, 30, 0)
        aodTimeoutMinutes = timeoutOptions.getOrElse(timeoutIndex) { 0 }

        synchronized(lock) {
            scrollOffset = 0
            prepareTextBitmap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        turnOffLights()
        try {
            glyphManager.unInit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isRunning = false
    }

    // Mismatch 1: use GlyphMatrixObject instead of addTop(IntArray)
    private fun turnOffLights() {
        try {
            if (::glyphManager.isInitialized) {
                val blackBitmap = Bitmap.createBitmap(matrixLength, matrixLength, Bitmap.Config.ARGB_8888)
                val obj = GlyphMatrixObject.Builder()
                    .setImageSource(blackBitmap)
                    .build()
                val frame = GlyphMatrixFrame.Builder()
                    .addTop(obj)
                    .build(applicationContext)
                glyphManager.setMatrixFrame(frame)
            }
        } catch (e: Exception) {}
    }

    private fun prepareTextBitmap() {
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = matrixLength * 0.8f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        // 取得精確的文字寬度 (包含最後面的空格)
        val textWidth = paint.measureText(textToScroll).toInt()
        val height = matrixLength

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

            if (direction == 2) {
                // 向上 (Up)：順時針 90 度
                matrix.postRotate(90f)
            } else {
                // 向下 (Down)：逆時針 90 度 (-90)
                matrix.postRotate(-90f)
            }

            try {
                textBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)
            } catch (e: Exception) {
                textBitmap = baseBitmap
            }
        } else {
            textBitmap = baseBitmap
        }

        Log.d(TAG, "Bitmap Updated for AOD/Normal. Dir=$direction, MatrixLength=$matrixLength")
    }

    // AOD 計時關閉：時間到停止動畫並熄燈
    private val aodTimeoutRunnable = Runnable {
        Log.d(TAG, "AOD timeout reached, stopping animation")
        stopMarquee()
        turnOffLights()
    }

    // 延遲啟動，讓系統在 AOD 過渡期穩定後再開始動畫
    private val aodStartRunnable = Runnable {
        Log.d(TAG, "Starting AOD Sequence")
        isRunning = true
        handler.post(marqueeRunnable)
        if (aodTimeoutMinutes > 0) {
            handler.postDelayed(aodTimeoutRunnable, aodTimeoutMinutes * 60 * 1000L)
        }
    }

    private fun triggerAodAnimation() {
        isRunning = false
        handler.removeCallbacks(marqueeRunnable)
        handler.removeCallbacks(aodStartRunnable)
        handler.removeCallbacks(aodTimeoutRunnable)
        Log.d(TAG, "AOD Event: queuing animation start (200ms delay)")
        handler.postDelayed(aodStartRunnable, 200)
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            try {
                synchronized(lock) {
                    if (::textBitmap.isInitialized) {
                        val size = matrixLength
                        val bmpWidth = textBitmap.width
                        val bmpHeight = textBitmap.height

                        // 計算每個 LED 的亮度值（含捲動位移與方向）
                        val matrixData = IntArray(size * size)
                        for (y in 0 until size) {
                            for (x in 0 until size) {
                                val fetchX: Int
                                val fetchY: Int
                                when (direction) {
                                    0 -> { fetchX = (scrollOffset + x) % bmpWidth; fetchY = y }
                                    1 -> { fetchX = (bmpWidth - (scrollOffset % bmpWidth) + x) % bmpWidth; fetchY = y }
                                    2 -> { fetchX = x; fetchY = (scrollOffset + y) % bmpHeight }
                                    3 -> { fetchX = x; fetchY = (bmpHeight - (scrollOffset % bmpHeight) + y) % bmpHeight }
                                    else -> { fetchX = x; fetchY = y }
                                }
                                if (fetchX < bmpWidth && fetchY < bmpHeight) {
                                    val pixel = textBitmap.getPixel(fetchX, fetchY)
                                    matrixData[y * size + x] = if (Color.red(pixel) > 50) brightness else 0
                                }
                            }
                        }

                        // Mismatch 1: convert to Bitmap and use GlyphMatrixObject (documented API)
                        val pixels = IntArray(size * size) { i ->
                            val v = matrixData[i].coerceIn(0, 255)
                            Color.argb(255, v, v, v)
                        }
                        val frameBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                        frameBitmap.setPixels(pixels, 0, size, 0, 0, size, size)

                        val obj = GlyphMatrixObject.Builder()
                            .setImageSource(frameBitmap)
                            .build()

                        val frame = GlyphMatrixFrame.Builder()
                            .addTop(obj)
                            .build(applicationContext)

                        if (::glyphManager.isInitialized) {
                            glyphManager.setMatrixFrame(frame)
                        }

                        scrollOffset += 1
                        if (scrollOffset > 1000000) scrollOffset = 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Animation Error: ${e.message}")
            } finally {
                // 無論是否發生例外，只要 isRunning 仍為 true 就繼續排程
                // 避免例外發生後動畫靜止但 GlyphMatrix 仍亮著的情況
                if (isRunning) {
                    handler.postDelayed(this, updateSpeed)
                }
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
        isRunning = false
        handler.removeCallbacks(marqueeRunnable)
        handler.removeCallbacks(aodStartRunnable)
        handler.removeCallbacks(aodTimeoutRunnable)
    }
}
