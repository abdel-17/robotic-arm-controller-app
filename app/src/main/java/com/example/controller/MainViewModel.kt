package com.example.controller

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager: BluetoothManager =
        application.getSystemService(BluetoothManager::class.java)

    @get:RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    val pairedDevices: Set<BluetoothDevice>
        get() = bluetoothManager.adapter.bondedDevices

    private var _bluetoothSocket = mutableStateOf<BluetoothSocket?>(null)
    val bluetoothSocket: BluetoothSocket?
        get() = _bluetoothSocket.value

    private var _connecting = mutableStateOf(false)
    val connecting: Boolean
        get() = _connecting.value

    fun connect(device: BluetoothDevice, onComplete: (success: Boolean) -> Unit) {
        _bluetoothSocket.value?.close()
        _bluetoothSocket.value = null
        _connecting.value = true

        val thread = BluetoothConnectionThread(device) { socket ->
            viewModelScope.launch {
                _connecting.value = false
                _bluetoothSocket.value = socket
                onComplete(socket != null)
            }
        }
        thread.start()
    }

    override fun onCleared() {
        _bluetoothSocket.value?.close()
    }
}
