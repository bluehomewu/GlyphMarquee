package tw.bluehomewu.glyphmarquee

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
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
     * Download APK from [url] with a ProgressDialog, then trigger PackageManager install.
     */
    @Suppress("DEPRECATION")
    fun downloadAndInstall(context: Context, url: String, tagName: String) {
        val progress = ProgressDialog(context).apply {
            setTitle(context.getString(R.string.updater_downloading_title))
            setMessage(context.getString(R.string.updater_downloading_message))
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            isIndeterminate = false
            max = 100
            setCancelable(false)
            show()
        }

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    progress.dismiss()
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.updater_download_failed, e.message),
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    mainHandler.post {
                        progress.dismiss()
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.updater_download_failed, "HTTP ${response.code}"),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    return
                }

                try {
                    val apkDir = File(context.cacheDir, "apk").also { it.mkdirs() }
                    val apkFile = File(apkDir, "GlyphMarquee-$tagName.apk")

                    val body = response.body ?: throw IOException("Empty body")
                    val contentLength = body.contentLength()

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
                                    mainHandler.post { progress.progress = pct }
                                }
                            }
                        }
                    }

                    mainHandler.post {
                        progress.dismiss()
                        installApk(context, apkFile)
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        progress.dismiss()
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.updater_download_failed, e.message),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
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

