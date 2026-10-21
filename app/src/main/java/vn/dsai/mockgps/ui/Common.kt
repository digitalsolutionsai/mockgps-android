package vn.dsai.mockgps.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.dsai.mockgps.Permissions
import vn.dsai.mockgps.R

/** Everything the app needs before it can mock, re-read whenever the user comes back from Settings. */
data class PermState(
    val devOptions: Boolean,
    val mockApp: Boolean,
    val location: Boolean,
    val notifications: Boolean,
    val overlay: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val ready get() = mockApp && location

    companion object {
        fun read(ctx: Context) = PermState(
            devOptions = Settings.Global.getInt(ctx.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1,
            mockApp = Permissions.isMockApp(ctx),
            location = Permissions.hasLocation(ctx),
            notifications = Permissions.hasNotifications(ctx),
            overlay = Permissions.canOverlay(ctx),
            batteryUnrestricted = ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName),
        )
    }
}

fun Context.copyText(label: String, text: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, text))
    // Android 13+ shows its own confirmation.
    if (android.os.Build.VERSION.SDK_INT < 33) Toast.makeText(this, getString(R.string.common_copied), Toast.LENGTH_SHORT).show()
}

fun Context.openSafely(intent: Intent, fallback: Intent? = null) {
    runCatching { startActivity(intent) }.onFailure { fallback?.let { f -> runCatching { startActivity(f) } } }
}

fun Context.openDeveloperOptions() = openSafely(
    Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
    Intent(Settings.ACTION_SETTINGS),
)

fun Context.openAppDetails() = openSafely(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
)

fun Context.openOverlaySettings() = openSafely(
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
)

fun Context.openUrl(url: String) = openSafely(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

/** A monospace command with a copy button, for adb snippets. */
@Composable
fun CommandBlock(command: String, label: String = "adb") {
    val ctx = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            SelectionContainer(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(command, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { ctx.copyText(label, command) }) {
                Icon(Icons.Filled.ContentCopy, stringResource(R.string.common_copy_command))
            }
        }
    }
}

@Composable
fun Paragraph(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun Section(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}
