package com.example.keylogger

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.delay
import android.view.KeyEvent

class ServerActivity : AppCompatActivity() {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val serverPort = 9999
    private var printWriter: PrintWriter? = null
    private var isClientConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        val ipAddress = getLocalIpAddress()

        // Find views
        val ipText = findViewById<TextView>(R.id.tvIP)
        val statusText = findViewById<TextView>(R.id.tvStatus)
        val inputEditText = findViewById<EditText>(R.id.etInput)

        // Display IP in large, easy to read format
        ipText.text = "Server IP: $ipAddress"
        statusText.text = "Status: Waiting for client..."

        // Start server in background
        CoroutineScope(Dispatchers.IO).launch {
            startServer()
        }

        // Start a coroutine to check client connection status
        // In your while loop that checks connection:
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                delay(1000) // Check every second
                if (isClientConnected) {
                    try {
                        // Try to write to the socket to check if it's still connected
                        if (clientSocket?.isClosed == true || !clientSocket?.isConnected!! ||
                            printWriter?.checkError() == true) {
                            isClientConnected = false
                            clientSocket?.close()
                            clientSocket = null
                            printWriter = null
                            runOnUiThread {
                                findViewById<TextView>(R.id.tvStatus).text = "Status: Client disconnected. Waiting for new client..."
                            }
                            startServer() // Restart server to accept new connection
                        }
                    } catch (e: Exception) {
                        isClientConnected = false
                        clientSocket?.close()
                        clientSocket = null
                        printWriter = null
                        runOnUiThread {
                            findViewById<TextView>(R.id.tvStatus).text = "Status: Client disconnected. Waiting for new client..."
                        }
                        startServer()
                    }
                }
                delay(1000)
            }
        }

        // Enhanced key monitoring
        inputEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val specialKey = when (keyCode) {
                    KeyEvent.KEYCODE_DEL -> "[BACKSPACE]"
                    KeyEvent.KEYCODE_ENTER -> "[ENTER]\n"
                    KeyEvent.KEYCODE_SPACE -> "[SPACE]"
                    KeyEvent.KEYCODE_TAB -> "[TAB]"
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> "[SHIFT]"
                    KeyEvent.KEYCODE_CAPS_LOCK -> "[CAPS_LOCK]"
                    KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.KEYCODE_ALT_RIGHT -> "[ALT]"
                    KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_CTRL_RIGHT -> "[CTRL]"
                    else -> null
                }

                specialKey?.let {
                    if (isClientConnected) {
                        CoroutineScope(Dispatchers.IO).launch {
                            sendToClient(it)
                        }
                    }
                }
            }
            false
        }

        // Monitor text input
        // Regular text monitoring
        inputEditText.addTextChangedListener(object : TextWatcher {
            private var previousText = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s?.toString() ?: ""
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isClientConnected) {
                    val currentText = s?.toString() ?: ""
                    // Only send the new character(s) that were added
                    if (currentText.length > previousText.length) {
                        val newText = currentText.substring(start, start + count)
                        CoroutineScope(Dispatchers.IO).launch {
                            sendToClient(newText)
                        }
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun startServer() {
        try {
            if (serverSocket == null || serverSocket?.isClosed == true) {
                serverSocket = ServerSocket(serverPort)
            }

            // Wait for client connection
            clientSocket = serverSocket?.accept()
            isClientConnected = true

            // Set up writer for sending data to client
            printWriter = PrintWriter(clientSocket?.getOutputStream(), true)

            runOnUiThread {
                findViewById<TextView>(R.id.tvStatus).text = "Status: Client connected!"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendToClient(message: String) {
        try {
            printWriter?.println(message)
        } catch (e: Exception) {
            e.printStackTrace()
            isClientConnected = false
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                // Skip interfaces that are down, loopback or virtual
                if (!networkInterface.isUp || networkInterface.isLoopback) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // We only want IPv4 addresses
                    if (address is java.net.Inet4Address) {
                        val ip = address.hostAddress
                        // Usually hotspot IPs start with 192.168 or 172. or 10.
                        if (ip.startsWith("192.168") || ip.startsWith("172.") || ip.startsWith("10.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "IP not found"
    }

    override fun onDestroy() {
        super.onDestroy()
        isClientConnected = false
        printWriter?.close()
        clientSocket?.close()
        serverSocket?.close()
    }
}