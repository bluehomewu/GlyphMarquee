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

    /** Fetch latest release unconditionally (for manual changelog tap). */
    fun fetchLatestRelease(
        onSuccess: (ReleaseInfo) -> Unit,
        onError: (String) -> Unit
    ) = fetch(onSuccess, onError)

    /**
     * Fetch and only call [onSuccess] when remote is NEWER than [localTag].
     * Used for silent startup check — no callbacks on error or same/older version.
     */
    fun checkForUpdate(localTag: String, onNewVersion: (ReleaseInfo) -> Unit) {
        fetch(
            onSuccess = { info ->
                if (isNewer(localTag, info.tagName)) onNewVersion(info)
            },
            onError = { /* silent on startup */ }
        )
    }

    fun isNewer(local: String, remote: String): Boolean = compareVersions(local, remote) > 0

    // ── Internal ─────────────────────────────────────────────────────────

    private fun fetch(onSuccess: (ReleaseInfo) -> Unit, onError: (String) -> Unit) {
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
                            if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
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
     * Positive = remote is newer, 0 = equal, negative = local is newer.
     * Same base version with a suffix (e.g. "-hotfix") on remote = newer.
     */
    private fun compareVersions(local: String, remote: String): Int {
        val (lb, ls) = splitTag(local)
        val (rb, rs) = splitTag(remote)
        val baseCmp = compareSemanticParts(lb, rb)
        if (baseCmp != 0) return baseCmp
        return when {
            ls.isEmpty() && rs.isNotEmpty() -> 1
            ls.isNotEmpty() && rs.isEmpty() -> -1
            else -> rs.compareTo(ls)
        }
    }

    private fun splitTag(tag: String): Pair<String, String> {
        val s = tag.trimStart('v', 'V')
        val i = s.indexOf('-')
        return if (i >= 0) s.substring(0, i) to s.substring(i + 1) else s to ""
    }

    private fun compareSemanticParts(a: String, b: String): Int {
        val ap = a.split(".").map { it.toIntOrNull() ?: 0 }
        val bp = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(ap.size, bp.size)) {
            val diff = bp.getOrElse(i) { 0 } - ap.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }
}
