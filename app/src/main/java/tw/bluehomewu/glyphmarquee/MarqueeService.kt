package tw.bluehomewu.glyphmarquee

import android.app.Service
import android.content.ComponentName
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

    // 你可以隨意修改這裡的文字
    private val textToScroll = " HELLO NOTHING (3) "
    private var scrollX = 0
    private lateinit var textBitmap: Bitmap

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            Log.d(TAG, "Glyph Matrix Service Connected")
            Thread {
                try {
                    // 1. 註冊 (Phone 3 Device ID)
                    glyphManager.register("23112")

                    // 2. 緩衝一下，確保註冊狀態同步
                    Thread.sleep(100)

                    // 3. 開始跑馬燈
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
            prepareTextBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Init failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // 退出時，送出全黑畫面來熄燈
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
            textSize = 20f // 字體大小
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        val textWidth = paint.measureText(textToScroll).toInt()
        val height = 25

        // 建立兩倍寬度的圖片以便循環
        textBitmap = Bitmap.createBitmap(textWidth * 2 + 50, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(textBitmap)
        canvas.drawColor(Color.BLACK)

        // 垂直置中計算
        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(textToScroll, 0f, yPos, paint)
        canvas.drawText(textToScroll, textWidth.toFloat(), yPos, paint)
    }

    private val marqueeRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            try {
                val matrixData = IntArray(625)

                for (y in 0 until 25) {
                    for (x in 0 until 25) {
                        val targetX = (scrollX + x) % (textBitmap.width / 2)

                        if (targetX < textBitmap.width) {
                            val pixel = textBitmap.getPixel(targetX, y)

                            // === 亮度設定 ===
                            // 嚴格設定為 SDK 規範的 255 (最大亮度)
                            // 只要有些微像素存在，就全亮，確保字體清晰
                            val brightness = if (Color.red(pixel) > 50) 255 else 0

                            matrixData[y * 25 + x] = brightness
                        }
                    }
                }

                // 建構並送出畫面
                val frame = GlyphMatrixFrame.Builder()
                    .addTop(matrixData)
                    .build(applicationContext)

                glyphManager.setMatrixFrame(frame)

                // 移動位置
                scrollX += 1
                if (scrollX >= textBitmap.width / 2) scrollX = 0

                // 設定 FPS (目前 100ms = 10fps，如果要跑快一點可以改成 50)
                handler.postDelayed(this, 100)

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