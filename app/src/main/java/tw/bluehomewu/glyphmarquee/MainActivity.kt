package tw.bluehomewu.glyphmarquee

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nothing.ketchum.Common

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tilText = findViewById<TextInputLayout>(R.id.tilMarqueeText)
        val etText = findViewById<TextInputEditText>(R.id.etMarqueeText)
        val sliderSpeed = findViewById<Slider>(R.id.sliderSpeed)
        val tvSpeedValue = findViewById<TextView>(R.id.tvSpeedValue)
        val sliderBrightness = findViewById<Slider>(R.id.sliderBrightness)
        val tvBrightnessValue = findViewById<TextView>(R.id.tvBrightnessValue)
        val actvDirection = findViewById<AutoCompleteTextView>(R.id.actvDirection)
        val actvAodTimeout = findViewById<AutoCompleteTextView>(R.id.actvAodTimeout)
        val btnApply = findViewById<Button>(R.id.btnApply)

        val isPhone4aPro = try { Common.getDeviceMatrixLength() == 13 } catch (_: Exception) { false }

        if (isPhone4aPro) {
            tilText.helperText = getString(R.string.input_helper_phone_4a_pro)
        }

        val directions = resources.getStringArray(R.array.directions)
        val aodOptions = resources.getStringArray(R.array.aod_timeout_options)

        actvDirection.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, directions))
        actvAodTimeout.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, aodOptions))

        val prefs = getSharedPreferences("MarqueePrefs", Context.MODE_PRIVATE)

        val defaultText = if (isPhone4aPro) getString(R.string.default_marquee_text_4a_pro)
                          else getString(R.string.default_marquee_text)
        etText.setText(prefs.getString("text", defaultText))

        val savedSpeed = prefs.getInt("speed", 100).coerceIn(10, 200)
        sliderSpeed.value = savedSpeed.toFloat()
        tvSpeedValue.text = "$savedSpeed ms"

        val savedBrightness = prefs.getInt("brightness", 255).coerceIn(1, 255)
        sliderBrightness.value = savedBrightness.toFloat()
        tvBrightnessValue.text = "$savedBrightness"

        val savedDirection = prefs.getInt("direction", 0).coerceIn(0, directions.lastIndex)
        actvDirection.setText(directions[savedDirection], false)

        val savedAodTimeoutIndex = prefs.getInt("aod_timeout_index", 5).coerceIn(0, aodOptions.lastIndex)
        actvAodTimeout.setText(aodOptions[savedAodTimeoutIndex], false)

        sliderSpeed.addOnChangeListener { _, value, _ ->
            tvSpeedValue.text = "${value.toInt()} ms"
        }

        sliderBrightness.addOnChangeListener { _, value, _ ->
            tvBrightnessValue.text = "${value.toInt()}"
        }

        btnApply.setOnClickListener {
            val newText = etText.text.toString()
            val newSpeed = sliderSpeed.value.toInt()
            val newBrightness = sliderBrightness.value.toInt()
            val newDirection = directions.indexOf(actvDirection.text.toString()).coerceAtLeast(0)
            val newAodTimeoutIndex = aodOptions.indexOf(actvAodTimeout.text.toString()).coerceAtLeast(0)

            prefs.edit().apply {
                putString("text", newText)
                putInt("speed", newSpeed)
                putInt("brightness", newBrightness)
                putInt("direction", newDirection)
                putInt("aod_timeout_index", newAodTimeoutIndex)
                apply()
            }

            val intent = Intent(this, MarqueeService::class.java)
            intent.action = "UPDATE_CONFIG"
            startService(intent)

            Toast.makeText(this, getString(R.string.toast_settings_updated), Toast.LENGTH_SHORT).show()
        }

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

        val localTag = "v$currentVersionName"
        UpdateChecker.checkForUpdate(localTag) { info ->
            mainHandler.post { showReleaseDialog(isUpdate = true, info = info) }
        }

        tvVersion.setOnClickListener {
            Toast.makeText(this, getString(R.string.updater_checking), Toast.LENGTH_SHORT).show()
            UpdateChecker.fetchLatestRelease(
                onSuccess = { info ->
                    val isNewer = UpdateChecker.isNewer(localTag, info.tagName)
                    mainHandler.post { showReleaseDialog(isUpdate = isNewer, info = info) }
                },
                onError = { err ->
                    mainHandler.post {
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.updater_error_title))
                            .setMessage(getString(R.string.updater_error_message, err))
                            .setPositiveButton(getString(R.string.updater_close), null)
                            .show()
                    }
                }
            )
        }
    }

    // ── Updater ───────────────────────────────────────────────────────────

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun showReleaseDialog(isUpdate: Boolean, info: ReleaseInfo) {
        val markwon = io.noties.markwon.Markwon.builder(this)
            .usePlugin(io.noties.markwon.ext.strikethrough.StrikethroughPlugin.create())
            .usePlugin(io.noties.markwon.ext.tables.TablePlugin.create(this))
            .usePlugin(io.noties.markwon.ext.tasklist.TaskListPlugin.create(this))
            .build()

        val dialogView = layoutInflater.inflate(R.layout.dialog_release, null)
        val chipVersion = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.chipVersion)
        val tvNotes = dialogView.findViewById<TextView>(R.id.tvReleaseNotes)
        val layoutProgress = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutDownloadProgress)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvDownloadStatus)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressDownload)

        chipVersion.text = info.tagName
        markwon.setMarkdown(tvNotes, info.body.ifBlank { "_No release notes provided._" })

        val title = if (isUpdate)
            getString(R.string.updater_new_version_title, info.tagName)
        else
            getString(R.string.updater_up_to_date_title)

        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setNeutralButton(getString(R.string.updater_close), null)

        if (isUpdate) {
            builder.setPositiveButton(getString(R.string.updater_update), null)
            builder.setNegativeButton(getString(R.string.updater_skip), null)
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                if (info.apkDownloadUrl.isBlank()) {
                    Toast.makeText(this, getString(R.string.updater_no_apk), Toast.LENGTH_LONG).show()
                } else {
                    ApkDownloader.downloadAndInstall(
                        context = this,
                        url = info.apkDownloadUrl,
                        tagName = info.tagName,
                        dialog = dialog,
                        updateButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE),
                        layoutProgress = layoutProgress,
                        tvStatus = tvStatus,
                        progressBar = progressBar
                    )
                }
            }
        }

        dialog.show()
    }
}