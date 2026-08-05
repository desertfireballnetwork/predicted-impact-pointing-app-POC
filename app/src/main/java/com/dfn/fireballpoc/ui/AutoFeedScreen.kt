package com.dfn.fireballpoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dfn.fireballpoc.model.EventGenerator
import com.dfn.fireballpoc.model.EventType
import com.dfn.fireballpoc.model.FireballEvent
import com.dfn.fireballpoc.model.ObserverState
import com.dfn.fireballpoc.notify.NotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Auto-generates a new fake event every [EventGenerator.CYCLE_MS], alternating randomly between
 * fireball and re-entry geometry, and fires a "look up soon" notification
 * [EventGenerator.NOTIFY_LEAD_MS] before each one starts. A new event replaces whatever's
 * currently showing even if the previous one hasn't finished - matches how a real alert feed
 * would behave (a new alert can arrive before you've reacted to the last one).
 */
@Composable
fun AutoFeedScreen(
    observerState: ObserverState?,
    locationPermissionGranted: Boolean,
    onStop: () -> Unit
) {
    val context = LocalContext.current

    var currentEvent by remember { mutableStateOf<FireballEvent?>(null) }
    var showHud by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            val type = if (kotlin.random.Random.nextBoolean()) EventType.FIREBALL else EventType.REENTRY
            val event = EventGenerator.generate(observerState, type)

            currentEvent = event
            showHud = false

            launch {
                val notifyDelayMs = (event.startTimeEpochMs - EventGenerator.NOTIFY_LEAD_MS) - System.currentTimeMillis()
                if (notifyDelayMs > 0) {
                    delay(notifyDelayMs)
                    NotificationHelper.notifyUpcoming(context, type)
                }
            }

            delay(EventGenerator.CYCLE_MS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Auto alert feed", style = MaterialTheme.typography.titleLarge)

        val event = currentEvent
        if (event == null) {
            Text("Generating first event...")
        } else {
            if (showHud) {
                HudScreen(
                    event = event,
                    observerState = observerState,
                    locationPermissionGranted = locationPermissionGranted,
                    onBackToForm = onStop
                )
            } else {
                CountdownScreen(event = event, onShowHud = { showHud = true })
                Button(onClick = onStop, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Stop auto alerts")
                }
            }
        }
    }
}
