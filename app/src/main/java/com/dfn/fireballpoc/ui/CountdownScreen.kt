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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dfn.fireballpoc.model.FireballEvent
import kotlinx.coroutines.delay

@Composable
fun CountdownScreen(event: FireballEvent, onShowHud: () -> Unit) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(event) {
        while (true) {
            nowMs = System.currentTimeMillis()
            if (nowMs >= event.startTimeEpochMs) {
                onShowHud()
                break
            }
            delay(1000)
        }
    }

    val remainingMs = (event.startTimeEpochMs - nowMs).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(event.identifier, style = MaterialTheme.typography.titleMedium)
        Text("Time until event", style = MaterialTheme.typography.titleMedium)
        Text(
            formatHms(remainingMs),
            style = MaterialTheme.typography.displayLarge
        )
        Button(onClick = onShowHud, modifier = Modifier.padding(top = 24.dp)) {
            Text("Show HUD now")
        }
    }
}
