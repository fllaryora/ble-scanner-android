package com.santansarah.scan.presentation.scan.device

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.santansarah.scan.R
import com.santansarah.scan.local.entities.ScannedDevice
import com.santansarah.scan.domain.bleparsables.ELKBLEDOM
import com.santansarah.scan.domain.models.DeviceService

@Composable
fun DeviceButtons(
    connectEnabled: Boolean,
    onConnect: (String) -> Unit,
    device: ScannedDevice,
    disconnectEnabled: Boolean,
    onDisconnect: () -> Unit,
    services: List<DeviceService>,
    onControlClick: (String) -> Unit,
) {
    Row() {
        ConnectButtons(connectEnabled, onConnect, device, disconnectEnabled, onDisconnect)
        services.flatMap { service -> service.characteristics }.find { characteristics ->
            characteristics.uuid == ELKBLEDOM.uuid
        }?.also {
            FilledIconButton(
                /*colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFF0087ff),
                    contentColor = Color(0xFFe2e2e9)
                ),*/
                onClick = { onControlClick(device.address) },
                content = {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(id = R.drawable.control),
                        contentDescription = "Disconnect"
                    )
                })
        }
    }
}

@Composable
fun ConnectButtons(
    connectEnabled: Boolean,
    onConnect: (String) -> Unit,
    device: ScannedDevice,
    disconnectEnabled: Boolean,
    onDisconnect: () -> Unit
) {
    FilledIconButton(
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(.3f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        enabled = connectEnabled,
        onClick = { onConnect(device.address) },
        content = {
            Icon(
                painter = painterResource(id = R.drawable.connect),
                contentDescription = "Connect"
            )
        })
    FilledIconButton(
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.tertiary.copy(.3f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        enabled = disconnectEnabled,
        onClick = { onDisconnect() },
        content = {
            Icon(
                painter = painterResource(id = R.drawable.disconnect),
                contentDescription = "Disconnect"
            )
        })
}
