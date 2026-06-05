package org.spaceelephant.karakeepwidget.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import org.spaceelephant.karakeepwidget.MainActivity
import org.spaceelephant.karakeepwidget.data.KarakeepApi
import org.spaceelephant.karakeepwidget.data.KarakeepPrefs

/** Parameter carrying the tapped note id to [OpenNoteAction]. */
val noteIdKey = ActionParameters.Key<String>("note_id")

/** Re-runs the widget's data load when the refresh button is tapped. */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        KarakeepWidget().update(context, glanceId)
    }
}

/** Opens the configuration screen (MainActivity). */
class OpenConfigAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/**
 * Opens the Karakeep app. Falls back to opening the note in a browser
 * if the app is not installed.
 */
class OpenNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val pkg = KarakeepPrefs.appPackage(context)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)

        val intent = if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            val noteId = parameters[noteIdKey].orEmpty()
            val serverUrl = KarakeepPrefs.serverUrl(context)
            if (serverUrl.isEmpty()) {
                Toast.makeText(context, "Karakeep app not installed", Toast.LENGTH_SHORT).show()
                return
            }
            Intent(Intent.ACTION_VIEW, Uri.parse(KarakeepApi.webUrlFor(serverUrl, noteId)))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Karakeep", Toast.LENGTH_SHORT).show()
        }
    }
}
