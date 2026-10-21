@file:OptIn(ExperimentalMaterial3Api::class)

package vn.dsai.mockgps.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import vn.dsai.mockgps.MockState
import vn.dsai.mockgps.Prefs
import vn.dsai.mockgps.R
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit, onMapThemeChanged: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var accuracy by remember { mutableFloatStateOf(prefs.accuracyM) }
    var altitude by remember { mutableFloatStateOf(prefs.altitudeM) }
    var jitter by remember { mutableStateOf(prefs.jitter) }
    var interval by remember { mutableIntStateOf(prefs.intervalMs) }
    var gps by remember { mutableStateOf(prefs.mockGps) }
    var network by remember { mutableStateOf(prefs.mockNetwork) }
    var fused by remember { mutableStateOf(prefs.mockFused) }
    var joystick by remember { mutableFloatStateOf(prefs.joystickKmh) }
    var theme by remember { mutableStateOf(prefs.mapTheme) }
    var routing by remember { mutableStateOf(prefs.routingServer) }
    val running = MockState.status.collectAsStateWithLifecycle().value.running
    val locale = LocalConfiguration.current.locales[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (running) Paragraph(stringResource(R.string.settings_running_note))

            SectionTitle(stringResource(R.string.settings_section_signal))
            LabeledSlider(
                stringResource(R.string.settings_accuracy, accuracy.roundToInt()),
                accuracy, 1f..50f,
                onChange = { accuracy = it }, onDone = { prefs.accuracyM = accuracy.roundToInt().toFloat() },
            )
            LabeledSlider(
                stringResource(R.string.settings_altitude, altitude.roundToInt()),
                altitude, -50f..3000f,
                onChange = { altitude = it }, onDone = { prefs.altitudeM = altitude.roundToInt().toFloat() },
            )
            SwitchRow(stringResource(R.string.settings_jitter_title), stringResource(R.string.settings_jitter_desc), jitter) {
                jitter = it; prefs.jitter = it
            }
            Text(stringResource(R.string.settings_interval), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    500 to stringResource(R.string.settings_interval_500ms),
                    1000 to stringResource(R.string.settings_interval_1s),
                    2000 to stringResource(R.string.settings_interval_2s),
                    5000 to stringResource(R.string.settings_interval_5s),
                ).forEach { (ms, label) ->
                    FilterChip(selected = interval == ms, onClick = { interval = ms; prefs.intervalMs = ms }, label = { Text(label) })
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_section_sources))
            SwitchRow(stringResource(R.string.settings_source_gps), stringResource(R.string.settings_source_gps_desc), gps) { gps = it; prefs.mockGps = it }
            SwitchRow(stringResource(R.string.settings_source_network), stringResource(R.string.settings_source_network_desc), network) { network = it; prefs.mockNetwork = it }
            SwitchRow(
                stringResource(R.string.settings_source_fused),
                stringResource(R.string.settings_source_fused_desc),
                fused,
            ) { fused = it; prefs.mockFused = it }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_section_joystick))
            Text(stringResource(R.string.settings_joystick_speed), style = MaterialTheme.typography.bodyLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5f, 10f, 18f, 40f, 60f).forEach { kmh ->
                    FilterChip(
                        selected = joystick == kmh,
                        onClick = { joystick = kmh; prefs.joystickKmh = kmh },
                        label = { Text(stringResource(R.string.settings_speed_kmh, kmh.roundToInt())) },
                    )
                }
            }

            if (Build.VERSION.SDK_INT >= 33) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionTitle(stringResource(R.string.settings_section_language))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable {
                        ctx.openSafely(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${ctx.packageName}")),
                            Intent(Settings.ACTION_LOCALE_SETTINGS),
                        )
                    }.padding(vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_app_language), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_section_map))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "auto" to stringResource(R.string.settings_theme_auto),
                    "light" to stringResource(R.string.settings_theme_light),
                    "dark" to stringResource(R.string.settings_theme_dark),
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = theme == key,
                        onClick = {
                            theme = key
                            prefs.mapTheme = key
                            onMapThemeChanged()
                        },
                        label = { Text(label) },
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_section_routing))
            Paragraph(stringResource(R.string.settings_routing_desc))
            OutlinedTextField(
                value = routing,
                onValueChange = { routing = it; prefs.routingServer = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
            if (routing != Prefs.DEFAULT_ROUTING) {
                TextButton(onClick = { routing = Prefs.DEFAULT_ROUTING; prefs.routingServer = "" }) { Text(stringResource(R.string.settings_routing_reset)) }
            }
            Spacer(Modifier.navigationBarsPadding().padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, onDone: () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(value = value, onValueChange = onChange, onValueChangeFinished = onDone, valueRange = range)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
