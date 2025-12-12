package tw.bluehomewu.glyphmarquee

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

    private var scrollX = 0
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

        synchronized(lock) {
            scrollX = 0
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

    // ==========================================
    // ⚠️ 修正點 1：移除所有 Padding，只畫一次
    // ==========================================
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

        // 建立剛好等於文字寬度的 Bitmap (不需要乘 2，也不需要加 50)
        textBitmap = Bitmap.createBitmap(finalWidth, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(textBitmap)
        canvas.drawColor(Color.BLACK)

        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)

        // 只畫一次文字
        canvas.drawText(textToScroll, 0f, yPos, paint)

        Log.d(TAG, "Bitmap Updated: Width=$finalWidth")
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

                        for (y in 0 until 25) {
                            for (x in 0 until 25) {
                                // ==========================================
                                // ⚠️ 修正點 2：使用取餘數 (%) 達成無限循環
                                // ==========================================
                                // (當前捲軸位置 + x) 除以 圖片總寬度 的餘數
                                // 這樣當索引超出圖片寬度時，會自動回到 0，達成完美循環
                                val targetX = (scrollX + x) % bmpWidth

                                val pixel = textBitmap.getPixel(targetX, y)
                                val pixelBrightness = if (Color.red(pixel) > 50) brightness else 0
                                matrixData[y * 25 + x] = pixelBrightness
                            }
                        }

                        val frame = GlyphMatrixFrame.Builder()
                            .addTop(matrixData)
                            .build(applicationContext)

                        if (::glyphManager.isInitialized) {
                            glyphManager.setMatrixFrame(frame)
                        }

                        // 移動捲軸
                        scrollX += 1
                        // 當捲軸超過圖片寬度時，重置為 0 (其實不重置也可以，因為上面有 % bmpWidth)
                        // 但為了避免整數溢位，還是重置一下比較好
                        if (scrollX >= bmpWidth) scrollX = 0
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