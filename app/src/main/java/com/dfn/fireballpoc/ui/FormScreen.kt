package com.dfn.fireballpoc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dfn.fireballpoc.model.EventGenerator
import com.dfn.fireballpoc.model.EventType
import com.dfn.fireballpoc.model.FireballEvent
import com.dfn.fireballpoc.model.ObserverState

private data class FormFields(
    val startLat: String = "-29.0",
    val startLon: String = "122.0",
    val startHeightKm: String = "90",
    val endLat: String = "-30.5",
    val endLon: String = "121.0",
    val endHeightKm: String = "20",
    val startInSeconds: String = "60",
    val durationSeconds: String = "5"
)

/**
 * "Start in" is used instead of a full date/time picker: the underlying model still stores an
 * absolute epoch timestamp (computed once, on Simulate), but entering an offset from now is far
 * easier to test with outdoors than dialing in a wall-clock date/time.
 */
@Composable
fun FormScreen(
    spoofedLocation: ObserverState?,
    onPickLocation: () -> Unit,
    onClearSpoofedLocation: () -> Unit,
    onSimulate: (FireballEvent) -> Unit,
    onStartAutoFeed: () -> Unit
) {
    var fields by remember { mutableStateOf(FormFields()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Simulated event", style = MaterialTheme.typography.titleLarge)

        LocationStatusRow(spoofedLocation, onPickLocation, onClearSpoofedLocation)

        CoordinateRow("Start", fields.startLat, fields.startLon, fields.startHeightKm) { lat, lon, height ->
            fields = fields.copy(startLat = lat, startLon = lon, startHeightKm = height)
        }
        CoordinateRow("End", fields.endLat, fields.endLon, fields.endHeightKm) { lat, lon, height ->
            fields = fields.copy(endLat = lat, endLon = lon, endHeightKm = height)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField("Start in (s)", fields.startInSeconds, Modifier.weight(1f)) { fields = fields.copy(startInSeconds = it) }
            NumberField("Duration (s)", fields.durationSeconds, Modifier.weight(1f)) { fields = fields.copy(durationSeconds = it) }
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                val event = buildEvent(fields)
                if (event == null) {
                    errorMessage = "All fields must be valid numbers."
                } else {
                    errorMessage = null
                    onSimulate(event)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Simulate")
        }

        Button(
            onClick = onStartAutoFeed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start auto alerts (new event every 2 min)")
        }
    }
}

@Composable
private fun LocationStatusRow(
    spoofedLocation: ObserverState?,
    onPickLocation: () -> Unit,
    onClearSpoofedLocation: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (spoofedLocation == null) {
            Text(
                "Using real GPS",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onPickLocation) {
                Text("Simulate location")
            }
        } else {
            Text(
                "Simulating from %.3f, %.3f".format(spoofedLocation.latDeg, spoofedLocation.lonDeg),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = onPickLocation) {
                Text("Change")
            }
            OutlinedButton(onClick = onClearSpoofedLocation) {
                Text("Use real GPS")
            }
        }
    }
}

@Composable
private fun CoordinateRow(
    label: String,
    lat: String,
    lon: String,
    heightKm: String,
    onChange: (lat: String, lon: String, heightKm: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            NumberField("Lat", lat, Modifier.weight(1f)) { onChange(it, lon, heightKm) }
            NumberField("Lon", lon, Modifier.weight(1f)) { onChange(lat, it, heightKm) }
            NumberField("Alt (km)", heightKm, Modifier.weight(1f)) { onChange(lat, lon, it) }
        }
    }
}

private fun buildEvent(fields: FormFields): FireballEvent? {
    val startLat = fields.startLat.toDoubleOrNull() ?: return null
    val startLon = fields.startLon.toDoubleOrNull() ?: return null
    val startHeightKm = fields.startHeightKm.toDoubleOrNull() ?: return null
    val endLat = fields.endLat.toDoubleOrNull() ?: return null
    val endLon = fields.endLon.toDoubleOrNull() ?: return null
    val endHeightKm = fields.endHeightKm.toDoubleOrNull() ?: return null
    val startInSeconds = fields.startInSeconds.toDoubleOrNull() ?: return null
    val durationSeconds = fields.durationSeconds.toDoubleOrNull() ?: return null

    val startTimeEpochMs = System.currentTimeMillis() + (startInSeconds * 1000).toLong()

    return FireballEvent(
        identifier = EventGenerator.generateIdentifier(EventType.FIREBALL),
        type = EventType.FIREBALL,
        startLatDeg = startLat,
        startLonDeg = startLon,
        startHeightM = startHeightKm * 1000.0,
        endLatDeg = endLat,
        endLonDeg = endLon,
        endHeightM = endHeightKm * 1000.0,
        startTimeEpochMs = startTimeEpochMs,
        durationSeconds = durationSeconds
    )
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        // Text, not Number/Decimal: several IMEs hide the minus sign on numeric keypads, which
        // would make it impossible to type negative latitudes/longitudes (e.g. -29.0).
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        modifier = modifier
    )
}
