package tw.bluehomewu.glyphmarquee

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException

object ApkDownloader {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Download APK and show progress inline within [dialog].
     * Disables [updateButton] while downloading.
     * Shows progress in [layoutProgress], [tvStatus], [progressBar].
     * On complete, triggers Package Manager installer.
     */
    fun downloadAndInstall(
        context: Context,
        url: String,
        tagName: String,
        dialog: AlertDialog,
        updateButton: Button,
        layoutProgress: LinearLayout,
        tvStatus: TextView,
        progressBar: ProgressBar
    ) {
        // Show inline progress, disable button
        mainHandler.post {
            updateButton.isEnabled = false
            layoutProgress.visibility = View.VISIBLE
            tvStatus.text = context.getString(R.string.updater_downloading_message)
            progressBar.progress = 0
            progressBar.isIndeterminate = true
        }

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    resetProgress(updateButton, layoutProgress, progressBar)
                    Toast.makeText(
                        context,
                        context.getString(R.string.updater_download_failed, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post {
                        resetProgress(updateButton, layoutProgress, progressBar)
                        Toast.makeText(
                            context,
                            context.getString(R.string.updater_download_failed, "HTTP ${response.code}"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return
                }

                try {
                    val apkDir = File(context.cacheDir, "apk").also { it.mkdirs() }
                    val apkFile = File(apkDir, "GlyphMarquee-$tagName.apk")
                    val body = response.body ?: throw IOException("Empty body")
                    val contentLength = body.contentLength()

                    mainHandler.post { progressBar.isIndeterminate = contentLength <= 0 }

                    body.byteStream().use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(8 * 1024)
                            var downloaded = 0L
                            var bytes: Int
                            while (input.read(buffer).also { bytes = it } != -1) {
                                output.write(buffer, 0, bytes)
                                downloaded += bytes
                                if (contentLength > 0) {
                                    val pct = (downloaded * 100 / contentLength).toInt()
                                    mainHandler.post {
                                        progressBar.progress = pct
                                        tvStatus.text = context.getString(
                                            R.string.updater_download_progress, pct
                                        )
                                    }
                                }
                            }
                        }
                    }

                    mainHandler.post {
                        dialog.dismiss()
                        installApk(context, apkFile)
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        resetProgress(updateButton, layoutProgress, progressBar)
                        Toast.makeText(
                            context,
                            context.getString(R.string.updater_download_failed, e.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private fun resetProgress(
        updateButton: Button,
        layoutProgress: LinearLayout,
        progressBar: ProgressBar
    ) {
        updateButton.isEnabled = true
        layoutProgress.visibility = View.GONE
        progressBar.isIndeterminate = false
        progressBar.progress = 0
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
