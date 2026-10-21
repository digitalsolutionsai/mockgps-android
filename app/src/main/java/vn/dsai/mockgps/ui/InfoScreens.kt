@file:OptIn(ExperimentalMaterial3Api::class)

package vn.dsai.mockgps.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.dsai.mockgps.R

const val PRIVACY_URL = "https://dsai.vn/mockgps/privacy"
const val CONTACT_EMAIL = "loivo@dsai.vn"
const val SOURCE_URL = "https://github.com/digitalsolutionsai/mockgps-android"

@Composable
private fun InfoScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
            Spacer(Modifier.navigationBarsPadding().padding(bottom = 16.dp))
        }
    }
}

@Composable
fun AdbScreen(onBack: () -> Unit) {
    val pkg = LocalContext.current.packageName
    val svc = "adb shell am start-foreground-service -n $pkg/.MockService -a $pkg"
    InfoScaffold(stringResource(R.string.adb_title), onBack) {
        Paragraph(stringResource(R.string.adb_intro))
        SectionTitle(stringResource(R.string.adb_section_point))
        CommandBlock("$svc.POINT --es lat 10.7769 --es lng 106.7009")
        Paragraph(stringResource(R.string.adb_alt_hint))
        SectionTitle(stringResource(R.string.adb_section_query))
        CommandBlock("$svc.POINT --es q \"Chợ Bến Thành\"")
        SectionTitle(stringResource(R.string.adb_section_fav))
        CommandBlock("$svc.POINT --es fav \"Nhà\"")
        SectionTitle(stringResource(R.string.adb_section_route))
        CommandBlock("$svc.ROUTE --es points \"10.7798,106.6990;10.7725,106.6980\" --es by road --es travel motorbike --es speed 35 --es end loop")
        Paragraph(stringResource(R.string.adb_route_params))
        SectionTitle(stringResource(R.string.adb_section_controls))
        CommandBlock("$svc.PAUSE")
        CommandBlock("$svc.RESUME")
        CommandBlock("$svc.SPEED --es speed 60")
        CommandBlock("$svc.JOYSTICK --es state on")
        CommandBlock("$svc.STOP")
        SectionTitle(stringResource(R.string.adb_section_check))
        CommandBlock("adb shell dumpsys location | grep 'last location'")
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    InfoScaffold(stringResource(R.string.about_title), onBack) {
        Text(stringResource(R.string.about_app_version, version), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Paragraph(stringResource(R.string.about_description))
        Row {
            TextButton(onClick = { ctx.openUrl("https://dsai.vn") }) { Text("dsai.vn") }
            TextButton(onClick = { ctx.openUrl("mailto:$CONTACT_EMAIL") }) { Text(CONTACT_EMAIL) }
            TextButton(onClick = { ctx.openUrl(SOURCE_URL) }) { Text("GitHub") }
        }

        SectionTitle(stringResource(R.string.about_section_notes))
        Paragraph(stringResource(R.string.about_notes))

        SectionTitle(stringResource(R.string.about_section_privacy))
        Paragraph(stringResource(R.string.about_privacy))
        TextButton(onClick = { ctx.openUrl(PRIVACY_URL) }) { Text(stringResource(R.string.about_privacy_policy)) }

        SectionTitle(stringResource(R.string.about_section_third_party))
        Paragraph(stringResource(R.string.about_third_party))
        TextButton(onClick = { ctx.openUrl("https://www.openstreetmap.org/copyright") }) { Text(stringResource(R.string.about_osm_copyright)) }
    }
}
