package org.spaceelephant.karakeepwidget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch
import org.spaceelephant.karakeepwidget.data.KarakeepApi
import org.spaceelephant.karakeepwidget.data.KarakeepPrefs
import org.spaceelephant.karakeepwidget.ui.theme.KarakeepWidgetTheme
import org.spaceelephant.karakeepwidget.widget.KarakeepWidget

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KarakeepWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
@Preview
@Composable
private fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(KarakeepPrefs.serverUrl(context)) }
    var apiKey by remember { mutableStateOf(KarakeepPrefs.apiKey(context)) }
    var appPackage by remember { mutableStateOf(KarakeepPrefs.appPackage(context)) }
    var noteLimit by remember { mutableStateOf(KarakeepPrefs.noteLimit(context).toString()) }
    var status by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Karakeep Widget",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Connect to your Karakeep instance, then add the \"Karakeep Notes\" widget to your home screen.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            placeholder = { Text("https://karakeep.example.com") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API key") },
            placeholder = { Text("ak1_...") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = noteLimit,
            onValueChange = { new -> noteLimit = new.filter { it.isDigit() }.take(3) },
            label = { Text("Number of notes") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = appPackage,
            onValueChange = { appPackage = it },
            label = { Text("Karakeep app package") },
            supportingText = { Text("Leave as default unless you use a custom build") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        CreateButton("Save & refresh widget", onClick = {
            val limit = noteLimit.toIntOrNull()?.coerceIn(1, 100)?: KarakeepPrefs.DEFAULT_NOTE_LIMIT
            noteLimit = limit.toString()
            KarakeepPrefs.save(context, serverUrl, apiKey, appPackage, limit)
            scope.launch {
                KarakeepWidget().updateAll(context)
            }
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
        })

        CreateButton("Test connection", onClick = {
            val limit = noteLimit.toIntOrNull()?.coerceIn(1, 100)
                ?: KarakeepPrefs.DEFAULT_NOTE_LIMIT
            KarakeepPrefs.save(context, serverUrl, apiKey, appPackage, limit)
            status = "Testing…"
            scope.launch {
                status = try {
                    val notes = KarakeepApi.fetchNotes(context)
                    "Connected — fetched ${notes.size} note(s)."
                } catch (e: Exception) {
                    "Failed: ${e.message}"
                }
            }
        })

        if (status.isNotBlank()) {
            Text(text = status, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CreateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF008ECF),
            contentColor = Color(0xFFFFFFFF)
        )
    ) {
        Text(text)
    }
}