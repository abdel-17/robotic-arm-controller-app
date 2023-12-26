package com.example.controller

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.IOException
import java.util.UUID

class BluetoothConnectionThread(
    private val device: BluetoothDevice,
    private inline val onComplete: (socket: BluetoothSocket?) -> Unit,
) : Thread() {

    private companion object {
        const val TAG = "BluetoothConnectionThread"
        val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    override fun run() {
        var socket: BluetoothSocket? = null
        try {
            Log.d(TAG, "Connecting to ${device.name}")
            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            Log.d(TAG, "Connected to ${device.name}")
        } catch (e: IOException) {
            socket?.close()
            Log.d(TAG, "Failed to connect to ${device.name}: ${e.message}")
        } finally {
            onComplete(socket)
        }
    }
}
