package org.spaceelephant.karakeepwidget.data

import android.content.Context

/**
 * Lightweight wrapper around SharedPreferences for the Karakeep connection settings.
 * Read both from the configuration UI and from the widget background work.
 */
object KarakeepPrefs {

    private const val PREFS = "karakeep_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_APP_PACKAGE = "app_package"
    private const val KEY_NOTE_LIMIT = "note_limit"

    /** Package id of the official Karakeep (formerly Hoarder) Android app. */
    const val DEFAULT_APP_PACKAGE = "app.hoarder.hoardermobile"
    const val DEFAULT_NOTE_LIMIT = 25

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Server base URL, e.g. https://karakeep.example.com (no trailing slash). */
    fun serverUrl(context: Context): String =
        prefs(context).getString(KEY_SERVER_URL, "").orEmpty().trim().trimEnd('/')

    fun apiKey(context: Context): String =
        prefs(context).getString(KEY_API_KEY, "").orEmpty().trim()

    fun appPackage(context: Context): String =
        prefs(context).getString(KEY_APP_PACKAGE, DEFAULT_APP_PACKAGE)
            ?.trim()
            ?.ifEmpty { DEFAULT_APP_PACKAGE }
            ?: DEFAULT_APP_PACKAGE

    fun noteLimit(context: Context): Int =
        prefs(context).getInt(KEY_NOTE_LIMIT, DEFAULT_NOTE_LIMIT)

    fun isConfigured(context: Context): Boolean =
        serverUrl(context).isNotEmpty() && apiKey(context).isNotEmpty()

    fun save(
        context: Context,
        serverUrl: String,
        apiKey: String,
        appPackage: String = DEFAULT_APP_PACKAGE,
        noteLimit: Int = DEFAULT_NOTE_LIMIT,
    ) {
        prefs(context).edit().apply {
            putString(KEY_SERVER_URL, serverUrl.trim().trimEnd('/'))
            putString(KEY_API_KEY, apiKey.trim())
            putString(KEY_APP_PACKAGE, appPackage.trim().ifEmpty { DEFAULT_APP_PACKAGE })
            putInt(KEY_NOTE_LIMIT, noteLimit)
            apply()
        }
    }
}
