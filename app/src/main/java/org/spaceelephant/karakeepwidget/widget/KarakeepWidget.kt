package org.spaceelephant.karakeepwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionParametersOf
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.spaceelephant.karakeepwidget.data.KarakeepApi
import org.spaceelephant.karakeepwidget.data.KarakeepNote
import org.spaceelephant.karakeepwidget.data.KarakeepPrefs

/** Result of a widget data load. */
private sealed interface WidgetState {
    data object NotConfigured : WidgetState
    data class Error(val message: String) : WidgetState
    data class Loaded(val notes: List<KarakeepNote>) : WidgetState
}

class KarakeepWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        provideContent {
            GlanceTheme {
                WidgetContent(state)
            }
        }
    }

    private suspend fun loadState(context: Context): WidgetState {
        if (!KarakeepPrefs.isConfigured(context)) return WidgetState.NotConfigured
        return try {
            WidgetState.Loaded(KarakeepApi.fetchNotes(context))
        } catch (e: Exception) {
            WidgetState.Error(e.message ?: "Failed to load notes")
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
    ) {
        Header()
        Spacer(GlanceModifier.height(8.dp))
        when (state) {
            is WidgetState.NotConfigured -> CenteredMessage(
                "Tap to set up your Karakeep server",
                GlanceModifier.clickable(actionRunCallback<OpenConfigAction>()),
            )

            is WidgetState.Error -> CenteredMessage(
                state.message,
                GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
            )

            is WidgetState.Loaded ->
                if (state.notes.isEmpty()) {
                    CenteredMessage(
                        "No notes found",
                        GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
                    )
                } else {
                    NoteList(state.notes)
                }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Karakeep - Widget",
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = "↻",
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .clickable(actionRunCallback<RefreshAction>())
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = TextStyle(
                color = GlanceTheme.colors.primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun NoteList(notes: List<KarakeepNote>) {
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        items(notes, itemId = { it.id.hashCode().toLong() }) { note ->
            NoteRow(note)
        }
    }
}

@Composable
private fun NoteRow(note: KarakeepNote) {
    // Outer box has NO background, so its padding becomes a transparent gap
    // between cards. (In Glance, background fills the padding area too, unlike
    // regular Compose — so the spacing must live on a backgroundless wrapper.)
    Box(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF1F1F1F))
                .cornerRadius(12.dp)
                .clickable(
                    actionRunCallback<OpenNoteAction>(
                        actionParametersOf(noteIdKey to note.id),
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = note.title,
                maxLines = 2,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (note.snippet.isNotBlank()) {
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = note.snippet,
                    maxLines = 2,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(message: String, modifier: GlanceModifier) {
    Box(
        modifier = GlanceModifier.fillMaxSize().then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 14.sp,
            ),
        )
    }
}
