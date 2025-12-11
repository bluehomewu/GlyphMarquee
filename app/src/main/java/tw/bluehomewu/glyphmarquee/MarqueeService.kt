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
    private var brightness = 255 // 新增亮度變數，預設最亮

    private var scrollX = 0
    private lateinit var textBitmap: Bitmap
    private val lock = Any()

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            Log.d(TAG, "Glyph Matrix Service Connected")
            Thread {
                try {
                    glyphManager.register("23112")
                    Thread.sleep(100)

                    // 連線後載入設定
                    loadSettings()

                    handler.post { startMarquee() }
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "UPDATE_CONFIG") {
            Log.d(TAG, "Received Update Command")
            loadSettings()
        }
        return START_STICKY
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("MarqueePrefs", Context.MODE_PRIVATE)
        textToScroll = prefs.getString("text", " HELLO NOTHING (3) ") ?: " ERROR "
        updateSpeed = prefs.getInt("speed", 100).toLong()
        // 讀取亮度
        brightness = prefs.getInt("brightness", 255)

        synchronized(lock) {
            scrollX = 0
            prepareTextBitmap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            val blackFrame = IntArray(625) { 0 }
            glyphManager.setMatrixFrame(blackFrame)
            glyphManager.unInit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopMarquee()
    }

    override fun onBind(intent: Intent?): IBinder? = null

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

        Log.d(TAG, "Updated: Text='$textToScroll', Speed=$updateSpeed, Brightness=$brightness")
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            try {
                synchronized(lock) {
                    if (::textBitmap.isInitialized) {
                        val matrixData = IntArray(625)

                        for (y in 0 until 25) {
                            for (x in 0 until 25) {
                                val targetX = (scrollX + x) % (textBitmap.width / 2)

                                if (targetX < textBitmap.width) {
                                    val pixel = textBitmap.getPixel(targetX, y)

                                    // === 亮度套用 ===
                                    // 判斷是否有點，如果有，就使用使用者設定的亮度變數
                                    val pixelBrightness = if (Color.red(pixel) > 50) brightness else 0

                                    matrixData[y * 25 + x] = pixelBrightness
                                }
                            }
                        }

                        val frame = GlyphMatrixFrame.Builder()
                            .addTop(matrixData)
                            .build(applicationContext)

                        glyphManager.setMatrixFrame(frame)

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
        if (!isRunning) {
            isRunning = true
            handler.post(marqueeRunnable)
        }
    }

    private fun stopMarquee() {
        isRunning = false
        handler.removeCallbacks(marqueeRunnable)
    }
}