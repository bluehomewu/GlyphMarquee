package tw.bluehomewu.glyphmarquee

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

data class ReleaseInfo(
    val tagName: String,
    val body: String,
    val apkDownloadUrl: String
)

object UpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/bluehomewu/GlyphMarquee/releases/latest"

    private val client = OkHttpClient()

    fun fetchLatestRelease(
        onSuccess: (ReleaseInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val request = Request.Builder()
            .url(API_URL)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string()
                    if (!response.isSuccessful || body == null) {
                        onError("HTTP ${response.code}")
                        return
                    }
                    val json = JSONObject(body)
                    val tagName = json.getString("tag_name")
                    val releaseBody = json.optString("body", "")

                    val assets = json.optJSONArray("assets")
                    var apkUrl = ""
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                apkUrl = asset.getString("browser_download_url")
                                break
                            }
                        }
                    }

                    onSuccess(ReleaseInfo(tagName, releaseBody, apkUrl))
                } catch (e: Exception) {
                    onError(e.message ?: "Parse error")
                }
            }
        })
    }

    /**
     * Returns positive if [remote] > [local] (remote is newer), 0 if equal, negative otherwise.
     * Handles "-hotfix" (or any suffix): same base + suffix on remote = newer.
     */
    fun compareVersions(local: String, remote: String): Int {
        val (localBase, localSuffix) = splitTag(local)
        val (remoteBase, remoteSuffix) = splitTag(remote)

        val baseCmp = compareSemanticParts(localBase, remoteBase)
        if (baseCmp != 0) return baseCmp

        return when {
            localSuffix.isEmpty() && remoteSuffix.isNotEmpty() -> 1
            localSuffix.isNotEmpty() && remoteSuffix.isEmpty() -> -1
            else -> remoteSuffix.compareTo(localSuffix)
        }
    }

    fun isNewer(local: String, remote: String): Boolean = compareVersions(local, remote) > 0

    private fun splitTag(tag: String): Pair<String, String> {
        val stripped = tag.trimStart('v', 'V')
        val dashIdx = stripped.indexOf('-')
        return if (dashIdx >= 0) stripped.substring(0, dashIdx) to stripped.substring(dashIdx + 1)
        else stripped to ""
    }

    private fun compareSemanticParts(a: String, b: String): Int {
        val aParts = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bParts = b.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(aParts.size, bParts.size)
        for (i in 0 until len) {
            val diff = bParts.getOrElse(i) { 0 } - aParts.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }
}

