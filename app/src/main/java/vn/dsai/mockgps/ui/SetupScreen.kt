@file:OptIn(ExperimentalMaterial3Api::class)

package vn.dsai.mockgps.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import vn.dsai.mockgps.Permissions
import vn.dsai.mockgps.R

@Composable
fun SetupScreen(onDone: () -> Unit) {
    val ctx = LocalContext.current
    var perm by remember { mutableStateOf(PermState.read(ctx)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { perm = PermState.read(ctx) }
    // Also catches changes made over adb while this screen stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            perm = PermState.read(ctx)
        }
    }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        perm = PermState.read(ctx)
    }
    val pkg = ctx.packageName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Paragraph(stringResource(R.string.setup_intro))

            Step(
                n = 1, done = perm.devOptions || perm.mockApp,
                title = stringResource(R.string.setup_step1_title),
                body = stringResource(R.string.setup_step1_body),
                action = stringResource(R.string.setup_step1_action),
                onAction = { ctx.openSafely(Intent(Settings.ACTION_DEVICE_INFO_SETTINGS), Intent(Settings.ACTION_SETTINGS)) },
            )
            Step(
                n = 2, done = perm.mockApp,
                title = stringResource(R.string.setup_step2_title),
                body = stringResource(R.string.setup_step2_body),
                action = stringResource(R.string.setup_step2_action),
                onAction = { ctx.openDeveloperOptions() },
            )
            Step(
                n = 3, done = perm.location && perm.notifications,
                title = stringResource(R.string.setup_step3_title),
                body = stringResource(R.string.setup_step3_body),
                action = stringResource(R.string.setup_step3_action),
                onAction = { request.launch(Permissions.runtimeRequests()) },
                secondary = stringResource(R.string.setup_step3_secondary) to { ctx.openAppDetails() },
            )
            Step(
                n = 4, done = perm.overlay, optional = true,
                title = stringResource(R.string.setup_step4_title),
                body = stringResource(R.string.setup_step4_body),
                action = stringResource(R.string.setup_step4_action),
                onAction = { ctx.openOverlaySettings() },
            )

            Step(
                n = 5, done = perm.batteryUnrestricted, optional = true,
                title = stringResource(R.string.setup_step5_title),
                body = stringResource(R.string.setup_step5_body),
                action = stringResource(R.string.setup_step5_action),
                onAction = { ctx.openSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), Intent(Settings.ACTION_SETTINGS)) },
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.setup_quick_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Paragraph(stringResource(R.string.setup_quick_body))
                    CommandBlock(
                        "adb shell appops set $pkg android:mock_location allow\n" +
                            "adb shell pm grant $pkg android.permission.ACCESS_COARSE_LOCATION\n" +
                            "adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS",
                    )
                }
            }

            Button(onClick = onDone, enabled = perm.ready, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (perm.ready) R.string.setup_start else R.string.setup_finish_steps))
            }
            if (!perm.ready) {
                TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.setup_later)) }
            }
            Spacer(Modifier.navigationBarsPadding().padding(bottom = 8.dp))
        }
    }
}

@Composable
private fun Step(
    n: Int,
    done: Boolean,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    optional: Boolean = false,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(Modifier.padding(16.dp)) {
            Surface(
                shape = CircleShape,
                color = if (done) Color(0xFF34A853) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (done) Icon(Icons.Filled.Check, stringResource(R.string.setup_done), tint = Color.White, modifier = Modifier.size(18.dp))
                    else Text("$n", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (optional) stringResource(R.string.setup_step_optional, title) else title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Paragraph(body)
                if (!done) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        FilledTonalButton(onClick = onAction) { Text(action) }
                        secondary?.let { (label, go) -> TextButton(onClick = go) { Text(label) } }
                    }
                }
            }
        }
    }
}
