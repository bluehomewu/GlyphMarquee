package tw.bluehomewu.glyphmarquee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etText = findViewById<TextInputEditText>(R.id.etMarqueeText)
        val sbSpeed = findViewById<SeekBar>(R.id.sbSpeed)
        val tvSpeedValue = findViewById<TextView>(R.id.tvSpeedValue)

        // 新增亮度控制項
        val sbBrightness = findViewById<SeekBar>(R.id.sbBrightness)
        val tvBrightnessValue = findViewById<TextView>(R.id.tvBrightnessValue)

        val btnApply = findViewById<Button>(R.id.btnApply)

        // 1. 讀取儲存的設定
        val prefs = getSharedPreferences("MarqueePrefs", Context.MODE_PRIVATE)

        etText.setText(prefs.getString("text", getString(R.string.default_marquee_text)))

        // 讀取速度
        val savedSpeed = prefs.getInt("speed", 100)
        sbSpeed.progress = savedSpeed
        tvSpeedValue.text = "$savedSpeed ms"

        // 讀取亮度 (預設 255)
        val savedBrightness = prefs.getInt("brightness", 255)
        sbBrightness.progress = savedBrightness
        tvBrightnessValue.text = "$savedBrightness"

        // 2. 拉桿監聽器
        sbSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = if (progress < 10) 10 else progress
                tvSpeedValue.text = "$value ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 顯示目前的亮度數值
                tvBrightnessValue.text = "$progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 3. 儲存與套用
        btnApply.setOnClickListener {
            val newText = etText.text.toString()
            var newSpeed = sbSpeed.progress
            if (newSpeed < 10) newSpeed = 10

            val newBrightness = sbBrightness.progress

            // 儲存設定
            prefs.edit().apply {
                putString("text", newText)
                putInt("speed", newSpeed)
                putInt("brightness", newBrightness)
                apply()
            }

            // 通知 Service
            val intent = Intent(this, MarqueeService::class.java)
            intent.action = "UPDATE_CONFIG"
            startService(intent)

            Toast.makeText(this, getString(R.string.toast_settings_updated), Toast.LENGTH_SHORT).show()
        }
    }
}