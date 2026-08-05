package com.dfn.fireballpoc

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.dfn.fireballpoc.model.FireballEvent
import com.dfn.fireballpoc.model.ObserverState
import com.dfn.fireballpoc.notify.NotificationHelper
import com.dfn.fireballpoc.sensors.LocationTracker
import com.dfn.fireballpoc.ui.AutoFeedScreen
import com.dfn.fireballpoc.ui.CountdownScreen
import com.dfn.fireballpoc.ui.FormScreen
import com.dfn.fireballpoc.ui.HudScreen
import com.dfn.fireballpoc.ui.MapPickerScreen
import org.osmdroid.config.Configuration

private sealed interface Screen {
    data object Form : Screen
    data class Countdown(val event: FireballEvent) : Screen
    data class Hud(val event: FireballEvent) : Screen
    data object AutoFeed : Screen
    data object MapPicker : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // osmdroid requires a distinct user agent (else tile requests can be blocked by OSM's
        // usage policy) and a writable cache dir; using the app's own cache dir avoids needing
        // any storage permission.
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = cacheDir
        Configuration.getInstance().osmdroidTileCache = cacheDir

        setContent {
            MaterialTheme {
                Scaffold { padding ->
                    AppRoot(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun AppRoot(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<Screen>(Screen.Form) }
    var spoofedLocation by remember { mutableStateOf<ObserverState?>(null) }
    val context = LocalContext.current

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    // A single combined launcher: firing two separate RequestPermission() launchers back-to-back
    // (one for location, one for notifications) silently drops the second request while the
    // first system dialog is still pending - RequestMultiplePermissions() is the correct way to
    // ask for more than one permission and was the actual reason notifications never appeared.
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results[Manifest.permission.ACCESS_FINE_LOCATION]?.let { locationPermissionGranted = it }
    }

    // Request up front so permissions are already resolved by the time the user reaches the HUD
    // or starts the auto-alert feed.
    LaunchedEffect(Unit) {
        NotificationHelper.ensureChannel(context)

        val toRequest = buildList {
            if (!locationPermissionGranted) add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (toRequest.isNotEmpty()) {
            permissionsLauncher.launch(toRequest.toTypedArray())
        }
    }

    val locationTracker = remember { LocationTracker(context) }
    val realObserverState by locationTracker.observerState.collectAsState()

    // A spoofed location (picked on the map) always wins over the real GPS fix, so the observer
    // can simulate being somewhere they aren't - the whole point of the "move for a better view"
    // suggestion is easier to test when you can actually go stand at the suggested spot virtually.
    val effectiveObserverState = spoofedLocation ?: realObserverState
    val effectiveLocationAvailable = locationPermissionGranted || spoofedLocation != null

    // Only track real GPS while the HUD/auto-feed is visible AND no spoofed location is set, to
    // avoid keeping GPS on for the whole app or while a spoofed location is already in use.
    DisposableEffect(screen, locationPermissionGranted, spoofedLocation) {
        if ((screen is Screen.Hud || screen is Screen.AutoFeed) && locationPermissionGranted && spoofedLocation == null) {
            locationTracker.start()
        }
        onDispose { locationTracker.stop() }
    }

    Box(modifier = modifier) {
        when (val current = screen) {
            is Screen.Form -> FormScreen(
                spoofedLocation = spoofedLocation,
                onPickLocation = { screen = Screen.MapPicker },
                onClearSpoofedLocation = { spoofedLocation = null },
                onSimulate = { event -> screen = Screen.Countdown(event) },
                onStartAutoFeed = { screen = Screen.AutoFeed }
            )

            is Screen.Countdown -> CountdownScreen(
                event = current.event,
                onShowHud = { screen = Screen.Hud(current.event) }
            )

            is Screen.Hud -> HudScreen(
                event = current.event,
                observerState = effectiveObserverState,
                locationPermissionGranted = effectiveLocationAvailable,
                onBackToForm = { screen = Screen.Form }
            )

            is Screen.AutoFeed -> AutoFeedScreen(
                observerState = effectiveObserverState,
                locationPermissionGranted = effectiveLocationAvailable,
                onStop = { screen = Screen.Form }
            )

            is Screen.MapPicker -> MapPickerScreen(
                initialLocation = effectiveObserverState,
                onConfirm = { picked ->
                    spoofedLocation = picked
                    screen = Screen.Form
                },
                onCancel = { screen = Screen.Form }
            )
        }
    }
}
