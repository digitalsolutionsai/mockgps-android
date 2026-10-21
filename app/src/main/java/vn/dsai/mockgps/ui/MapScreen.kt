@file:OptIn(ExperimentalMaterial3Api::class)

package vn.dsai.mockgps.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.dsai.mockgps.Favorite
import vn.dsai.mockgps.Geo
import vn.dsai.mockgps.Gpx
import vn.dsai.mockgps.LatLng
import vn.dsai.mockgps.MockControl
import vn.dsai.mockgps.MockService
import vn.dsai.mockgps.MockState
import vn.dsai.mockgps.MockStatus
import vn.dsai.mockgps.Mode
import vn.dsai.mockgps.Permissions
import vn.dsai.mockgps.Place
import vn.dsai.mockgps.Places
import vn.dsai.mockgps.Prefs
import vn.dsai.mockgps.R
import vn.dsai.mockgps.Route
import vn.dsai.mockgps.RouteEnd
import vn.dsai.mockgps.Travel
import kotlin.math.roundToInt

enum class Screen { MAP, SETUP, SETTINGS, ADB, ABOUT }

private enum class Pane(@StringRes val label: Int) {
    POINT(R.string.sheet_pane_point),
    ROUTE(R.string.sheet_pane_route),
    FAV(R.string.sheet_pane_fav),
}

/** The map stays composed underneath the secondary screens so it keeps its camera and tiles. */
@Composable
fun AppRoot(incoming: MutableStateFlow<String?>) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var screen by rememberSaveable {
        mutableStateOf(if (!prefs.onboarded && !PermState.read(ctx).ready) Screen.SETUP else Screen.MAP)
    }
    var themeVersion by remember { mutableIntStateOf(0) }
    val systemDark = isSystemInDarkTheme()
    val darkMap = remember(themeVersion, systemDark) {
        when (prefs.mapTheme) {
            "dark" -> true
            "light" -> false
            else -> systemDark
        }
    }

    MapScreen(incoming, darkMap, active = screen == Screen.MAP, onOpen = { screen = it })

    if (screen != Screen.MAP) {
        val close = {
            if (screen == Screen.SETUP) prefs.onboarded = true
            screen = Screen.MAP
        }
        BackHandler(onBack = close)
        when (screen) {
            Screen.SETUP -> SetupScreen(onDone = close)
            Screen.SETTINGS -> SettingsScreen(onBack = close, onMapThemeChanged = { themeVersion++ })
            Screen.ADB -> AdbScreen(onBack = close)
            Screen.ABOUT -> AboutScreen(onBack = close)
            Screen.MAP -> Unit
        }
    }
}

@Composable
private fun MapScreen(incoming: MutableStateFlow<String?>, darkMap: Boolean, active: Boolean, onOpen: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val status by MockState.status.collectAsStateWithLifecycle()
    val map = remember(darkMap) { MapHolder(ctx, darkMap) }
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffold = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val snackbar = scaffold.snackbarHostState

    var pane by rememberSaveable { mutableStateOf(Pane.POINT) }
    var selection by remember { mutableStateOf(prefs.lastPoint) }
    var selectionName by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    val waypoints = remember { mutableStateListOf<LatLng>() }
    var plannedLine by remember { mutableStateOf<List<LatLng>?>(null) }
    var fromGpx by remember { mutableStateOf(false) }
    var routing by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var travel by remember { mutableStateOf(prefs.travel) }
    var byRoad by remember { mutableStateOf(prefs.routeByRoad) }
    var speed by remember { mutableFloatStateOf(prefs.routeSpeedKmh) }
    var routeEnd by remember { mutableStateOf(prefs.routeEnd) }
    var favorites by remember { mutableStateOf(prefs.favorites) }
    var joystickPref by remember { mutableStateOf(prefs.joystickEnabled) }
    var follow by remember { mutableStateOf(true) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var perm by remember { mutableStateOf(PermState.read(ctx)) }
    var saving by remember { mutableStateOf(false) }
    var topInset by remember { mutableIntStateOf(0) }
    var summaryPx by remember { mutableIntStateOf(0) }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { perm = PermState.read(ctx) }
    LaunchedEffect(active) { if (active) perm = PermState.read(ctx) }

    // MapView lifecycle; a theme change swaps in a new MapHolder.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, map) {
        val observer = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> map.view.onStart()
                Lifecycle.Event.ON_RESUME -> map.view.onResume()
                Lifecycle.Event.ON_PAUSE -> {
                    map.camera?.let { prefs.camera = it }
                    map.view.onPause()
                }
                Lifecycle.Event.ON_STOP -> map.view.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) map.view.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) map.view.onStop()
            map.view.onDestroy()
        }
    }
    LaunchedEffect(map) {
        val running = MockState.status.value.position
        val saved = prefs.camera
        when {
            running != null -> map.jumpTo(running, 15.0)
            saved != null -> map.jumpTo(LatLng(saved.first, saved.second), saved.third)
            else -> (selection ?: lastKnown(ctx))?.let { map.jumpTo(it, 15.0) } ?: map.jumpTo(LatLng(16.0, 106.0), 4.5)
        }
    }
    // Keep the camera's focus inside the part of the map not covered by the search bar and the sheet.
    LaunchedEffect(map) {
        snapshotFlow { topInset to runCatching { sheetState.requireOffset() }.getOrNull() }
            .collect { (top, sheetTop) ->
                delay(120)
                map.setInsets(top, sheetTop?.roundToInt() ?: 0)
            }
    }

    val gpxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val pts = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)!!.use { Gpx.parse(it) } }
            }
            pts.onSuccess {
                if (it.size < 2) toast(res.getString(R.string.route_gpx_too_few_points))
                else {
                    waypoints.clear()
                    plannedLine = it
                    fromGpx = true
                    pane = Pane.ROUTE
                    map.fit(it)
                }
            }.onFailure { toast(res.getString(R.string.route_gpx_read_error, it.message)) }
        }
    }

    fun ensureReady(): Boolean {
        if (perm.ready) return true
        onOpen(Screen.SETUP)
        return false
    }

    fun startPoint(p: LatLng) {
        if (!ensureReady()) return
        MockControl.point(ctx, p)
        follow = true
    }

    fun choose(p: LatLng, name: String?, addr: String?) {
        results = emptyList()
        searchError = null
        focus.clearFocus()
        follow = false
        if (pane == Pane.ROUTE) {
            if (fromGpx) { fromGpx = false; plannedLine = null }
            waypoints.add(p)
        } else {
            selection = p
            selectionName = name?.takeIf { it.isNotBlank() }
            address = addr?.takeIf { it.isNotBlank() }
            pane = Pane.POINT
        }
    }

    fun runSearch(text: String) {
        if (text.isBlank()) return
        focus.clearFocus()
        scope.launch {
            searching = true
            searchError = null
            results = try {
                Places.search(ctx, text, map.center)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                searchError = res.getString(R.string.map_search_error, e.message)
                emptyList()
            }
            searching = false
            if (results.isEmpty() && searchError == null) searchError = res.getString(R.string.map_no_results)
            if (results.size == 1) results[0].let { choose(it.point, it.name, it.address); map.moveTo(it.point, 15.0) }
        }
    }

    val shared by incoming.collectAsStateWithLifecycle()
    LaunchedEffect(shared) {
        val s = shared ?: return@LaunchedEffect
        incoming.value = null
        query = s.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        runSearch(s)
    }

    SideEffect {
        map.onTap = {
            results = emptyList()
            focus.clearFocus()
        }
        map.onLongPress = { p ->
            map.view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            choose(p, null, null)
        }
        map.onUserGesture = { follow = false }
    }

    LaunchedEffect(selection) {
        val s = selection ?: return@LaunchedEffect
        if (address != null) return@LaunchedEffect
        delay(400)
        address = Places.reverse(ctx, s)
    }

    val wpKey = waypoints.toList()
    LaunchedEffect(wpKey, byRoad, travel) {
        if (fromGpx) return@LaunchedEffect
        routeError = null
        if (wpKey.size < 2) { plannedLine = null; return@LaunchedEffect }
        if (!byRoad) { plannedLine = wpKey; return@LaunchedEffect }
        delay(350)
        routing = true
        try {
            plannedLine = Places.route(ctx, wpKey, travel, prefs.routingServer)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            routeError = e.message ?: res.getString(R.string.error_detail_network)
            plannedLine = wpKey
        } finally {
            routing = false
        }
    }

    val activeRoute = status.route
    val shownLine = activeRoute?.points ?: plannedLine
    LaunchedEffect(map, selection, wpKey, shownLine, pane) {
        map.render(if (pane == Pane.ROUTE) null else selection, if (activeRoute == null) wpKey else emptyList(), shownLine)
    }
    LaunchedEffect(map, status.position, status.running) {
        val p = status.position.takeIf { status.running }
        map.showMock(p, status.bearing.takeIf { status.speedKmh > 1 })
        if (p != null && follow) map.moveTo(p)
    }
    LaunchedEffect(status.running, status.joystick) { joystickPref = prefs.joystickEnabled }

    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val peek = if (summaryPx == 0) 220.dp else with(density) { (summaryPx + navBottom).toDp() } + 28.dp
    val maxDetails = (LocalConfiguration.current.screenHeightDp * 0.45f).dp

    BottomSheetScaffold(
        scaffoldState = scaffold,
        sheetPeekHeight = peek,
        sheetShadowElevation = 12.dp,
        sheetDragHandle = {
            Box(Modifier.padding(vertical = 12.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.size(width = 32.dp, height = 4.dp),
                ) {}
            }
        },
        sheetContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.onSizeChanged { summaryPx = it.height }) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Pane.entries.forEachIndexed { i, p ->
                            SegmentedButton(
                                selected = pane == p,
                                onClick = { pane = p },
                                shape = SegmentedButtonDefaults.itemShape(i, Pane.entries.size),
                            ) { Text(stringResource(p.label)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    when (pane) {
                        Pane.POINT -> PointSummary(
                            selection, selectionName, address, status.running,
                            onStart = { selection?.let { startPoint(it) } },
                            onCopy = { selection?.let { ctx.copyText("coordinates", it.format()) } },
                            onSave = { saving = true },
                        )
                        Pane.ROUTE -> RouteSummary(
                            status = status,
                            waypointCount = wpKey.size,
                            line = shownLine,
                            fromGpx = fromGpx,
                            routing = routing,
                            routeError = routeError,
                            speed = speed,
                            onUndo = { if (waypoints.isNotEmpty()) waypoints.removeAt(waypoints.lastIndex) },
                            onClear = { waypoints.clear(); plannedLine = null; fromGpx = false },
                            onGpx = { gpxPicker.launch(arrayOf("*/*")) },
                            onStart = {
                                val r = plannedLine?.let { Route.of(it) }
                                when {
                                    !ensureReady() -> Unit
                                    r == null -> toast(res.getString(R.string.error_no_route))
                                    else -> {
                                        MockControl.route(ctx, r, speed, routeEnd)
                                        follow = true
                                    }
                                }
                            },
                            onPause = {
                                MockControl.send(ctx, if (status.paused || status.finished) MockService.ACTION_RESUME else MockService.ACTION_PAUSE)
                            },
                            onStop = { MockControl.send(ctx, MockService.ACTION_STOP) },
                        )
                        Pane.FAV -> FavSummary(favorites.size)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                // Keeps the details out of the peek area that sits under the navigation bar.
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Column(
                    Modifier.heightIn(max = maxDetails).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    when (pane) {
                        Pane.POINT -> PointDetails(
                            address = address,
                            joystickOn = if (status.running) status.joystick else joystickPref,
                            onJoystick = { on ->
                                if (on && !perm.overlay) {
                                    toast(res.getString(R.string.sheet_joystick_permission))
                                    ctx.openOverlaySettings()
                                } else {
                                    joystickPref = on
                                    prefs.joystickEnabled = on
                                    MockControl.send(ctx, MockService.ACTION_JOYSTICK) { putExtra("state", if (on) "on" else "off") }
                                }
                            },
                        )
                        Pane.ROUTE -> RouteDetails(
                            locked = status.running && status.mode == Mode.ROUTE,
                            fromGpx = fromGpx,
                            travel = travel,
                            byRoad = byRoad,
                            speed = speed,
                            routeEnd = routeEnd,
                            onTravel = {
                                travel = it; prefs.travel = it
                                speed = it.kmh; prefs.routeSpeedKmh = it.kmh
                                MockControl.send(ctx, MockService.ACTION_SPEED) { putExtra("speed", it.kmh.toDouble()) }
                            },
                            onByRoad = { byRoad = it; prefs.routeByRoad = it },
                            onSpeed = { speed = it },
                            onSpeedDone = {
                                prefs.routeSpeedKmh = speed
                                MockControl.send(ctx, MockService.ACTION_SPEED) { putExtra("speed", speed.toDouble()) }
                            },
                            onRouteEnd = { routeEnd = it; prefs.routeEnd = it },
                        )
                        Pane.FAV -> FavList(
                            favorites = favorites,
                            onSelect = {
                                choose(it.point, it.name, null)
                                map.moveTo(it.point, 15.0)
                            },
                            onStart = {
                                choose(it.point, it.name, null)
                                startPoint(it.point)
                            },
                            onDelete = { f ->
                                val before = favorites
                                favorites = favorites - f
                                prefs.favorites = favorites
                                scope.launch {
                                    val r = snackbar.showSnackbar(res.getString(R.string.fav_deleted, f.name), res.getString(R.string.fav_undo), duration = SnackbarDuration.Short)
                                    if (r == SnackbarResult.ActionPerformed) {
                                        favorites = before
                                        prefs.favorites = before
                                    }
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(8.dp))
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            key(map) { AndroidView(factory = { map.view }, modifier = Modifier.fillMaxSize()) }

            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    Modifier.onGloballyPositioned { topInset = it.boundsInRoot().bottom.roundToInt() },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchBar(
                        query = query,
                        onChange = { query = it },
                        searching = searching,
                        onSearch = { runSearch(query) },
                        onClear = { query = ""; results = emptyList(); searchError = null },
                        onMenu = onOpen,
                    )
                    when {
                        status.running -> RunningCard(status, perm.location, onStop = { MockControl.send(ctx, MockService.ACTION_STOP) })
                        !perm.ready -> SetupBanner(onClick = { onOpen(Screen.SETUP) })
                    }
                }
                if (results.isNotEmpty()) {
                    Card(elevation = CardDefaults.cardElevation(6.dp)) {
                        Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                            results.forEachIndexed { i, r ->
                                if (i > 0) HorizontalDivider()
                                ListItem(
                                    headlineContent = { Text(r.name.ifBlank { r.point.format() }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    supportingContent = { Text(r.address.ifBlank { stringResource(R.string.map_result_coordinates) }, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                                    leadingContent = { Icon(Icons.Filled.Place, null) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        choose(r.point, r.name, r.address)
                                        map.moveTo(r.point, 15.0)
                                    },
                                )
                            }
                        }
                    }
                }
                searchError?.let {
                    Card { Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium) }
                }
            }

            SmallFloatingActionButton(
                onClick = {
                    val target = status.position.takeIf { status.running } ?: selection ?: lastKnown(ctx)
                    if (target != null) {
                        map.moveTo(target, 14.0)
                        follow = status.running
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).offset {
                    val sheetTop = runCatching { sheetState.requireOffset() }.getOrNull()?.roundToInt() ?: 0
                    IntOffset(-16.dp.roundToPx(), sheetTop - 56.dp.roundToPx())
                },
            ) { Icon(Icons.Filled.MyLocation, stringResource(R.string.map_my_location)) }
        }
    }

    val sel = selection
    if (saving && sel != null) {
        var name by remember { mutableStateOf(selectionName ?: address?.substringBefore(',').orEmpty()) }
        AlertDialog(
            onDismissRequest = { saving = false },
            title = { Text(stringResource(R.string.fav_save_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sel.format(), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.fav_name_label)) }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val n = name.trim()
                    favorites = favorites.filterNot { it.name.equals(n, true) } + Favorite(n, sel)
                    prefs.favorites = favorites
                    saving = false
                    toast(res.getString(R.string.fav_saved, n))
                }) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = { TextButton(onClick = { saving = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@SuppressLint("MissingPermission")
private fun lastKnown(ctx: Context): LatLng? {
    if (!Permissions.hasLocation(ctx)) return null
    val lm = ctx.getSystemService(LocationManager::class.java)
    return lm.getProviders(true)
        .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { LatLng(it.latitude, it.longitude) }
}

// ---- top of the map ---------------------------------------------------------------------------

@Composable
private fun SearchBar(
    query: String,
    onChange: (String) -> Unit,
    searching: Boolean,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onMenu: (Screen) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(28.dp), shadowElevation = 6.dp, tonalElevation = 2.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.map_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = {
                    if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Search, null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = onClear) { Icon(Icons.Filled.Close, stringResource(R.string.map_search_clear)) }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.map_menu)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    MenuItem(stringResource(R.string.setup_title), Icons.Filled.Checklist) { menu = false; onMenu(Screen.SETUP) }
                    MenuItem(stringResource(R.string.settings_title), Icons.Filled.Settings) { menu = false; onMenu(Screen.SETTINGS) }
                    MenuItem(stringResource(R.string.adb_title), Icons.Filled.Terminal) { menu = false; onMenu(Screen.ADB) }
                    MenuItem(stringResource(R.string.map_menu_about), Icons.Filled.Info) { menu = false; onMenu(Screen.ABOUT) }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

@Composable
private fun RunningCard(status: MockStatus, hasLocation: Boolean, onStop: () -> Unit) {
    val ctx = LocalContext.current
    val r = status.route
    val title = when {
        status.mode == Mode.ROUTE && status.finished -> stringResource(R.string.common_arrived)
        status.mode == Mode.ROUTE && status.paused -> stringResource(R.string.common_route_paused)
        status.mode == Mode.ROUTE -> stringResource(R.string.common_following_route, status.speedKmh.roundToInt())
        status.speedKmh > 1 -> stringResource(R.string.map_running_moving, status.speedKmh.roundToInt())
        else -> stringResource(R.string.common_mocking)
    }
    val subtitle = when {
        status.mode == Mode.ROUTE && r != null -> {
            val left = (r.length - status.progressM).coerceAtLeast(0.0)
            val done = Geo.formatDistance(status.progressM)
            val total = Geo.formatDistance(r.length)
            if (status.routeSpeedKmh > 0 && !status.finished) {
                stringResource(R.string.route_progress_eta, done, total, Geo.formatDuration(ctx, left / (status.routeSpeedKmh / 3.6)))
            } else {
                stringResource(R.string.route_progress, done, total)
            }
        }
        else -> status.position?.format().orEmpty()
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(Color(0xFF34A853), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                if (!hasLocation) {
                    Text(stringResource(R.string.map_no_location_permission), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.Filled.Stop, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.common_stop))
            }
        }
    }
}

@Composable
private fun SetupBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.map_setup_incomplete_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.map_setup_incomplete_body), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---- sheet: point -----------------------------------------------------------------------------

@Composable
private fun Hint(text: String, icon: ImageVector = Icons.Filled.TouchApp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PointSummary(
    selection: LatLng?,
    name: String?,
    address: String?,
    running: Boolean,
    onStart: () -> Unit,
    onCopy: () -> Unit,
    onSave: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (selection == null) {
            Hint(stringResource(R.string.sheet_hint_select_point))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, null, tint = Color(0xFFEA4335))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        name ?: address?.substringBefore(',') ?: stringResource(R.string.sheet_selected_point),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(selection.format(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.sheet_copy_coordinates)) }
                IconButton(onClick = onSave) { Icon(Icons.Filled.StarBorder, stringResource(R.string.fav_save)) }
            }
        }
        Button(onClick = onStart, enabled = selection != null, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(if (running) R.string.sheet_teleport_here else R.string.sheet_start_mock))
        }
    }
}

@Composable
private fun PointDetails(address: String?, joystickOn: Boolean, onJoystick: (Boolean) -> Unit) {
    address?.let {
        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Gamepad, null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.sheet_joystick_title), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.sheet_joystick_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = joystickOn, onCheckedChange = onJoystick)
    }
    Hint(stringResource(R.string.sheet_search_hint), Icons.Filled.Info)
}

// ---- sheet: route -----------------------------------------------------------------------------

@Composable
private fun RouteSummary(
    status: MockStatus,
    waypointCount: Int,
    line: List<LatLng>?,
    fromGpx: Boolean,
    routing: Boolean,
    routeError: String?,
    speed: Float,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onGpx: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val ctx = LocalContext.current
    val running = status.running && status.mode == Mode.ROUTE
    val route = remember(line) { line?.let { Route.of(it) } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = when {
                    running -> stringResource(R.string.route_running)
                    routing -> stringResource(R.string.route_finding)
                    fromGpx -> (line?.size ?: 0).let { pluralStringResource(R.plurals.route_gpx_points, it, it) }
                    waypointCount == 0 -> stringResource(R.string.route_hint_add_points)
                    waypointCount == 1 -> stringResource(R.string.route_need_one_more)
                    else -> pluralStringResource(R.plurals.route_point_count, waypointCount, waypointCount)
                }
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = when {
                    running -> status.route?.let { stringResource(R.string.route_progress, Geo.formatDistance(status.progressM), Geo.formatDistance(it.length)) }
                    routeError != null -> stringResource(R.string.route_error_straight)
                    route != null -> stringResource(
                        R.string.route_estimate,
                        Geo.formatDistance(route.length),
                        Geo.formatDuration(ctx, route.length / (speed / 3.6)),
                        speed.roundToInt(),
                    )
                    else -> stringResource(R.string.route_or_open_gpx)
                }
                sub?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (routeError != null && !running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onUndo, enabled = waypointCount > 0 && !running) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.route_undo)) }
            IconButton(onClick = onClear, enabled = (waypointCount > 0 || fromGpx) && !running) { Icon(Icons.Filled.Delete, stringResource(R.string.route_clear)) }
            IconButton(onClick = onGpx, enabled = !running) { Icon(Icons.Filled.FolderOpen, stringResource(R.string.route_open_gpx)) }
        }
        if (running) {
            val total = status.route?.length ?: 1.0
            LinearProgressIndicator(
                progress = { (status.progressM / total).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPause, modifier = Modifier.weight(1f)) {
                    val resume = status.paused || status.finished
                    Icon(if (resume) Icons.Filled.PlayArrow else Icons.Filled.Pause, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(if (status.finished) R.string.route_restart else if (status.paused) R.string.common_resume else R.string.common_pause))
                }
                OutlinedButton(onClick = onStop) { Text(stringResource(R.string.common_stop)) }
            }
        } else {
            Button(onClick = onStart, enabled = route != null && !routing, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Navigation, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.route_start))
            }
        }
    }
}

@Composable
private fun RouteDetails(
    locked: Boolean,
    fromGpx: Boolean,
    travel: Travel,
    byRoad: Boolean,
    speed: Float,
    routeEnd: RouteEnd,
    onTravel: (Travel) -> Unit,
    onByRoad: (Boolean) -> Unit,
    onSpeed: (Float) -> Unit,
    onSpeedDone: () -> Unit,
    onRouteEnd: (RouteEnd) -> Unit,
) {
    Text(stringResource(R.string.route_travel_title), style = MaterialTheme.typography.labelLarge)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Travel.entries.forEach { t ->
            FilterChip(selected = travel == t, onClick = { onTravel(t) }, label = { Text(stringResource(R.string.route_travel_chip, stringResource(t.label), t.kmh.roundToInt())) })
        }
    }
    Text(stringResource(R.string.route_connect_title), style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = byRoad, onClick = { onByRoad(true) }, label = { Text(stringResource(R.string.route_connect_road)) }, enabled = !fromGpx && !locked)
        FilterChip(selected = !byRoad, onClick = { onByRoad(false) }, label = { Text(stringResource(R.string.route_connect_straight)) }, enabled = !fromGpx && !locked)
    }
    Column {
        Text(stringResource(R.string.route_speed, speed.roundToInt()), style = MaterialTheme.typography.labelLarge)
        Slider(value = speed, onValueChange = onSpeed, onValueChangeFinished = onSpeedDone, valueRange = 1f..150f)
    }
    Text(stringResource(R.string.route_end_title), style = MaterialTheme.typography.labelLarge)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        RouteEnd.entries.forEachIndexed { i, e ->
            SegmentedButton(
                selected = routeEnd == e,
                onClick = { onRouteEnd(e) },
                shape = SegmentedButtonDefaults.itemShape(i, RouteEnd.entries.size),
                enabled = !locked,
            ) { Text(stringResource(e.label), maxLines = 1) }
        }
    }
}

// ---- sheet: favorites -------------------------------------------------------------------------

@Composable
private fun FavSummary(count: Int) {
    if (count == 0) Hint(stringResource(R.string.fav_hint_empty), Icons.Filled.StarBorder)
    else Text(pluralStringResource(R.plurals.fav_saved_count, count, count), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FavList(favorites: List<Favorite>, onSelect: (Favorite) -> Unit, onStart: (Favorite) -> Unit, onDelete: (Favorite) -> Unit) {
    favorites.forEach { f ->
        Row(
            Modifier.fillMaxWidth().clickable { onSelect(f) }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(f.point.format(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onStart(f) }) { Icon(Icons.Filled.PlayArrow, stringResource(R.string.fav_start_now)) }
            IconButton(onClick = { onDelete(f) }) { Icon(Icons.Filled.Delete, stringResource(R.string.fav_delete)) }
        }
    }
}
