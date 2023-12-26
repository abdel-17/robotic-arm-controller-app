package com.example.controller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.controller.ui.theme.RobotControllerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.IOException
import kotlin.math.roundToInt

private const val TAG = "MainActivity"

private val roboticArmParts = listOf("Rotator", "Lower Joint", "Upper Joint", "Claw")

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RobotControllerTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    App(viewModel)
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun App(viewModel: MainViewModel) {
    val bluetoothPermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_CONNECT
        else
            Manifest.permission.BLUETOOTH
    )

    LaunchedEffect(bluetoothPermissionState) {
        val permissionStatus = bluetoothPermissionState.status
        if (permissionStatus.isGranted) {
            Log.d(TAG, "Bluetooth permission is already granted")
            return@LaunchedEffect
        }

        Log.d(TAG, "Requesting bluetooth permission")
        bluetoothPermissionState.launchPermissionRequest()
    }

    if (!bluetoothPermissionState.status.isGranted) {
        Text(
            text = "Bluetooth permission is required",
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
        )
        return
    }

    fun sendBluetooth(value: String) {
        try {
            Log.d(TAG, "Sending $value through Bluetooth")
            viewModel.bluetoothSocket?.outputStream?.write(value.toByteArray())
        } catch (e: IOException) {
            Log.d(TAG, "Failed to send through Bluetooth: ${e.message}")
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        ConnectButton(viewModel)

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 32.dp),
        ) {
            roboticArmParts.forEachIndexed { i, part ->
                LabeledSlider(
                    label = part,
                    enabled = viewModel.bluetoothSocket?.isConnected == true,
                    onValueChangeFinished = { value ->
                        sendBluetooth(value = "s${i + 1}$value")
                    }
                )
            }
        }
    }
}

@RequiresPermission("android.permission.BLUETOOTH_CONNECT")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectButton(viewModel: MainViewModel) {
    val context = LocalContext.current
    var showBluetoothDevices by remember { mutableStateOf(false) }

    fun connect(device: BluetoothDevice) {
        viewModel.connect(device) { success ->
            if (success) {
                showBluetoothDevices = false
            } else {
                Toast.makeText(context, "Failed to connect to device", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    TextButton(
        onClick = { showBluetoothDevices = true }
    ) {
        Text("Connect")
    }

    if (showBluetoothDevices) {
        ModalBottomSheet(
            onDismissRequest = { showBluetoothDevices = false }
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                items(viewModel.pairedDevices.toList()) { device ->
                    BluetoothDeviceCard(
                        device = device.name,
                        enabled = !viewModel.connecting,
                        onClick = { connect(device) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceCard(
    device: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            text = device,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BluetoothDeviceCardPreview() {
    RobotControllerTheme {
        BluetoothDeviceCard(
            device = "HC-05",
            enabled = true,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
fun LabeledSlider(
    label: String,
    enabled: Boolean,
    onValueChangeFinished: (value: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var value by remember { mutableIntStateOf(0) }
    Column(modifier = modifier) {
        Text(
            text = "$label: $value",
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { value = it.roundToInt() },
            enabled = enabled,
            onValueChangeFinished = { onValueChangeFinished(value) },
            valueRange = 0f..100f,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LabeledSliderPreview() {
    RobotControllerTheme {
        LabeledSlider(
            label = roboticArmParts[0],
            enabled = true,
            onValueChangeFinished = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
