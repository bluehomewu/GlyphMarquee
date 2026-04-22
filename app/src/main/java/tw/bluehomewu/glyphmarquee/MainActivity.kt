package tw.bluehomewu.glyphmarquee

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nothing.ketchum.Common
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tilText = findViewById<TextInputLayout>(R.id.tilMarqueeText)
        val etText = findViewById<TextInputEditText>(R.id.etMarqueeText)
        val sbSpeed = findViewById<SeekBar>(R.id.sbSpeed)

        // 偵測裝置矩陣尺寸，Phone (4a) Pro = 13，Phone (3) = 25
        val isPhone4aPro = try { Common.getDeviceMatrixLength() == 13 } catch (_: Exception) { false }

        // Phone (4a) Pro (13×13) 不適合顯示文字，提示用戶改用符號或 Emoji
        if (isPhone4aPro) {
            tilText.helperText = getString(R.string.input_helper_phone_4a_pro)
        }
        val tvSpeedValue = findViewById<TextView>(R.id.tvSpeedValue)

        // 新增亮度控制項
        val sbBrightness = findViewById<SeekBar>(R.id.sbBrightness)
        val tvBrightnessValue = findViewById<TextView>(R.id.tvBrightnessValue)

        // 新增 Spinner 參考
        val spDirection = findViewById<Spinner>(R.id.spDirection)
        val spAodTimeout = findViewById<Spinner>(R.id.spAodTimeout)

        val btnApply = findViewById<Button>(R.id.btnApply)

        // 設定方向 Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.directions,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spDirection.adapter = adapter
        }

        // 設定 AOD 計時關閉 Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.aod_timeout_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spAodTimeout.adapter = adapter
        }

        // 1. 讀取儲存的設定
        val prefs = getSharedPreferences("MarqueePrefs", Context.MODE_PRIVATE)

        val defaultText = if (isPhone4aPro) getString(R.string.default_marquee_text_4a_pro)
                          else getString(R.string.default_marquee_text)
        etText.setText(prefs.getString("text", defaultText))

        // 讀取速度
        val savedSpeed = prefs.getInt("speed", 100)
        sbSpeed.progress = savedSpeed
        tvSpeedValue.text = "$savedSpeed ms"

        // 讀取亮度 (預設 255)
        val savedBrightness = prefs.getInt("brightness", 255)
        sbBrightness.progress = savedBrightness
        tvBrightnessValue.text = "$savedBrightness"

        // 讀取方向 (預設 0 = Left)
        val savedDirection = prefs.getInt("direction", 0)
        spDirection.setSelection(savedDirection)

        // 讀取 AOD 計時關閉 (預設 5 = 永遠顯示)
        val savedAodTimeoutIndex = prefs.getInt("aod_timeout_index", 5)
        spAodTimeout.setSelection(savedAodTimeoutIndex)

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

            // 取得選中的方向 (Index)
            val newDirection = spDirection.selectedItemPosition

            // 取得 AOD 計時關閉 Index
            val newAodTimeoutIndex = spAodTimeout.selectedItemPosition

            // 儲存設定
            prefs.edit().apply {
                putString("text", newText)
                putInt("speed", newSpeed)
                putInt("brightness", newBrightness)
                putInt("direction", newDirection)
                putInt("aod_timeout_index", newAodTimeoutIndex)
                apply()
            }

            // 通知 Service
            val intent = Intent(this, MarqueeService::class.java)
            intent.action = "UPDATE_CONFIG"
            startService(intent)

            Toast.makeText(this, getString(R.string.toast_settings_updated), Toast.LENGTH_SHORT).show()
        }

        // Mismatch 4: guide users to Glyph Toys manager (README recommendation)
        // Only show on Phone (3) — Phone (4a) Pro only supports AOD, not the full Glyph Toys UI
        val btnOpenGlyphToys = findViewById<Button>(R.id.btnOpenGlyphToys)
        if (!isPhone4aPro) {
            btnOpenGlyphToys.setOnClickListener {
                try {
                    val intent = Intent()
                    intent.component = ComponentName(
                        "com.nothing.thirdparty",
                        "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Glyph Toys Manager not available on this device", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btnOpenGlyphToys.visibility = android.view.View.GONE
        }

        val btnGithub = findViewById<Button>(R.id.btnGithub)
        btnGithub.setOnClickListener {
            try {
                val uri = android.net.Uri.parse("https://github.com/bluehomewu/GlyphMarquee")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            } catch (e: Exception) {
                // 防止使用者沒安裝瀏覽器導致閃退 (雖然機率很低)
                Toast.makeText(this, "無法開啟連結", Toast.LENGTH_SHORT).show()
            }
        }

        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        var currentVersionName = "unknown"
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            currentVersionName = packageInfo.versionName ?: "unknown"
            tvVersion.text = "v$currentVersionName"
        } catch (e: Exception) {
            tvVersion.text = "Version Unknown"
        }

        // ── Background startup check ──────────────────────────────────────
        checkForUpdate(currentVersionName, showToastIfUpToDate = false)

        // ── Tap version label to manually check / view release notes ─────
        tvVersion.setOnClickListener {
            checkForUpdate(currentVersionName, showToastIfUpToDate = true)
        }
    }

    // ── Updater ───────────────────────────────────────────────────────────

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun checkForUpdate(localVersion: String, showToastIfUpToDate: Boolean) {
        // Show "checking" toast only on manual tap
        if (showToastIfUpToDate) {
            Toast.makeText(this, getString(R.string.updater_checking), Toast.LENGTH_SHORT).show()
        }

        UpdateChecker.fetchLatestRelease(
            onSuccess = { info ->
                mainHandler.post { showReleaseDialog(localVersion, info) }
            },
            onError = { err ->
                if (showToastIfUpToDate) {
                    mainHandler.post {
                        AlertDialog.Builder(this)
                            .setTitle(getString(R.string.updater_error_title))
                            .setMessage(getString(R.string.updater_error_message, err))
                            .setPositiveButton(getString(R.string.updater_close), null)
                            .show()
                    }
                }
            }
        )
    }

    private fun showReleaseDialog(localVersion: String, info: ReleaseInfo) {
        val isNewer = UpdateChecker.isNewer("v$localVersion", info.tagName)

        val markwon = Markwon.builder(this)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(this))
            .usePlugin(TaskListPlugin.create(this))
            .build()

        // Build dialog content view with scrollable Markwon TextView
        val scrollView = android.widget.ScrollView(this)
        val contentTv = TextView(this).apply {
            setPadding(48, 32, 48, 32)
            setTextIsSelectable(true)
        }
        markwon.setMarkdown(contentTv, info.body.ifBlank { "_No release notes provided._" })
        scrollView.addView(contentTv)

        val title = if (isNewer)
            getString(R.string.updater_new_version_title, info.tagName)
        else
            getString(R.string.updater_up_to_date_title)

        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setNeutralButton(getString(R.string.updater_close), null)

        if (isNewer) {
            builder.setPositiveButton(getString(R.string.updater_update)) { _, _ ->
                if (info.apkDownloadUrl.isBlank()) {
                    Toast.makeText(this, getString(R.string.updater_no_apk), Toast.LENGTH_LONG).show()
                } else {
                    ApkDownloader.downloadAndInstall(this, info.apkDownloadUrl, info.tagName)
                }
            }
            builder.setNegativeButton(getString(R.string.updater_skip), null)
        }

        builder.show()
    }
}
