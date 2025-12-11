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
import android.util.Log
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixFrame

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

    // 增加一個標記，確認是否被系統選中 (Bound)
    private var isBoundBySystem = false

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            Log.d(TAG, "Glyph Matrix Service Connected")
            Thread {
                try {
                    glyphManager.register("23112")
                    Thread.sleep(100)

                    // 連線成功，載入設定
                    loadSettings()

                    // 只有當「系統選中我們」或是「APP正在設定」時才開始跑
                    // 但為了避免 Bug，這裡我們先不強制 Start，
                    // 而是交給 onBind/onRebind 來觸發，或者如果已經 Bound 就開始
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

    // =========================================================
    // ⚠️ 關鍵修正：生命週期管理 (Lifecycle Management)
    // =========================================================

    // 1. 當使用者透過按鈕選中這個 Toy 時，系統會呼叫 onBind
    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "System Bound to Service (Toy Selected)")
        isBoundBySystem = true
        startMarquee()
        return null
    }

    // 2. 當使用者切換到別的 Toy 時，系統會呼叫 onUnbind
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "System Unbound (Toy Deselected)")
        isBoundBySystem = false

        // 立刻停止迴圈
        stopMarquee()

        // 熄燈 (避免殘留影像)
        turnOffLights()

        // 回傳 true，這樣下次切換回來時才會呼叫 onRebind
        return true
    }

    // 3. 當使用者再次切換回來時
    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "System Rebound (Toy Selected Again)")
        isBoundBySystem = true
        startMarquee()
        super.onRebind(intent)
    }

    // =========================================================

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_CONFIG") {
            Log.d(TAG, "Received Update Command")
            loadSettings()

            // 如果使用者正在 APP 裡按「套用」，即使沒選中 Toy，我們也讓他預覽一下
            // 但如果不想干擾其他 Toy，可以把下面這行拿掉
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

    private fun prepareTextBitmap() {
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        val textWidth = paint.measureText(textToScroll).toInt()
        val height = 25
        val finalWidth = if (textWidth > 0) textWidth else 50

        textBitmap = Bitmap.createBitmap(finalWidth * 2 + 50, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(textBitmap)
        canvas.drawColor(Color.BLACK)

        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(textToScroll, 0f, yPos, paint)
        canvas.drawText(textToScroll, finalWidth.toFloat(), yPos, paint)
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            // 雙重保險：如果沒被選中，就不要跑 (除非你在 Debug)
            if (!isRunning) return
            // if (!isBoundBySystem) return // 加上這行可以更嚴格防止干擾，但會導致 APP 內預覽失效

            try {
                synchronized(lock) {
                    if (::textBitmap.isInitialized) {
                        val matrixData = IntArray(625)

                        for (y in 0 until 25) {
                            for (x in 0 until 25) {
                                val targetX = (scrollX + x) % (textBitmap.width / 2)

                                if (targetX < textBitmap.width) {
                                    val pixel = textBitmap.getPixel(targetX, y)
                                    val pixelBrightness = if (Color.red(pixel) > 50) brightness else 0
                                    matrixData[y * 25 + x] = pixelBrightness
                                }
                            }
                        }

                        val frame = GlyphMatrixFrame.Builder()
                            .addTop(matrixData)
                            .build(applicationContext)

                        if (::glyphManager.isInitialized) {
                            glyphManager.setMatrixFrame(frame)
                        }

                        scrollX += 1
                        if (scrollX >= textBitmap.width / 2) scrollX = 0
                    }
                }

                handler.postDelayed(this, updateSpeed)

            } catch (e: Exception) {
                Log.e(TAG, "Animation Error: ${e.message}")
            }
        }
    }

    private fun startMarquee() {
        // 只有在還沒跑的時候才啟動，避免重複 Runnable
        if (!isRunning) {
            isRunning = true
            handler.removeCallbacks(marqueeRunnable) // 先清掉舊的
            handler.post(marqueeRunnable)
            Log.d(TAG, "Marquee Started")
        }
    }

    private fun stopMarquee() {
        if (isRunning) {
            isRunning = false
            handler.removeCallbacks(marqueeRunnable)
            Log.d(TAG, "Marquee Stopped")
        }
    }
}