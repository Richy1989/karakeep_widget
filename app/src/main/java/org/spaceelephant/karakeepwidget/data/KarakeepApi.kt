package org.spaceelephant.karakeepwidget.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A single Karakeep bookmark/note as rendered in the widget. */
data class KarakeepNote(
    val id: String,
    val title: String,
    val snippet: String,
)

/** Raised when the Karakeep API cannot be reached or returns an error. */
class KarakeepApiException(message: String) : IOException(message)

object KarakeepApi {

    /**
     * Fetches the most recent bookmarks from the Karakeep instance.
     * Runs on the IO dispatcher; safe to call from a widget coroutine.
     */
    suspend fun fetchNotes(context: Context): List<KarakeepNote> = withContext(Dispatchers.IO) {
        val base = KarakeepPrefs.serverUrl(context)
        val token = KarakeepPrefs.apiKey(context)
        val limit = KarakeepPrefs.noteLimit(context)

        if (base.isEmpty() || token.isEmpty()) {
            throw KarakeepApiException("Karakeep is not configured")
        }

        val endpoint = "$base/api/v1/bookmarks?limit=$limit&sortOrder=desc"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw KarakeepApiException(
                    when (code) {
                        401, 403 -> "Unauthorized — check your API key"
                        404 -> "Not found — check the server URL"
                        else -> "Server error $code${if (detail.isNotBlank()) ": ${detail.take(120)}" else ""}"
                    }
                )
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseBookmarks(body)
        } catch (e: KarakeepApiException) {
            throw e
        } catch (e: Exception) {
            throw KarakeepApiException(e.message ?: "Could not reach Karakeep")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseBookmarks(body: String): List<KarakeepNote> {
        val root = JSONObject(body)
        val array = root.optJSONArray("bookmarks") ?: return emptyList()
        val notes = ArrayList<KarakeepNote>(array.length())
        for (i in 0 until array.length()) {
            val bm = array.optJSONObject(i) ?: continue
            val id = bm.optString("id").ifBlank { continue }
            val content = bm.optJSONObject("content")
            val type = content?.optString("type").orEmpty()

            val explicitTitle = bm.optStringOrNull("title")
                ?: content?.optStringOrNull("title")

            val text = when (type) {
                "text" -> content?.optStringOrNull("text").orEmpty()
                "link" -> content?.optStringOrNull("description")
                    ?: content?.optStringOrNull("url").orEmpty()
                else -> content?.optStringOrNull("url").orEmpty()
            }

            val note = bm.optStringOrNull("note").orEmpty()
            val combined = listOf(text, note).firstOrNull { it.isNotBlank() }.orEmpty()

            val title = (explicitTitle ?: combined.firstLine()).ifBlank { "(untitled)" }
            val snippet = combined
                .replace('\n', ' ')
                .trim()
                .let { if (it == title) "" else it }

            notes += KarakeepNote(
                id = id,
                title = title.take(120),
                snippet = snippet.take(180),
            )
        }
        return notes
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun String.firstLine(): String =
        trim().lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()

    /** Web URL for a single bookmark, used as a fallback when the app is not installed. */
    fun webUrlFor(serverUrl: String, noteId: String): String =
        "${serverUrl.trimEnd('/')}/dashboard/preview/${URLEncoder.encode(noteId, "UTF-8")}"
}
