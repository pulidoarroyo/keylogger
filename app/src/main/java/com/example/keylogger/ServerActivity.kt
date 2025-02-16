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

class ServerActivity : AppCompatActivity() {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val serverPort = 9999
    private var printWriter: PrintWriter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)

        val statusText = findViewById<TextView>(R.id.tvStatus)
        val inputEditText = findViewById<EditText>(R.id.etInput)

        // Display server IP address
        val ipAddress = getLocalIpAddress()
        statusText.text = "Server IP: $ipAddress\nWaiting for client..."

        // Start server in background
        CoroutineScope(Dispatchers.IO).launch {
            startServer()
        }

        // Monitor text input
        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.toString()?.let { text ->
                    CoroutineScope(Dispatchers.IO).launch {
                        sendToClient(text)
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun startServer() {
        try {
            serverSocket = ServerSocket(serverPort)

            // Wait for client connection
            clientSocket = serverSocket?.accept()

            // Set up writer for sending data to client
            printWriter = PrintWriter(clientSocket?.getOutputStream(), true)

            runOnUiThread {
                findViewById<TextView>(R.id.tvStatus).append("\nClient connected!")
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
        }
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "Unknown"
    }

    override fun onDestroy() {
        super.onDestroy()
        printWriter?.close()
        clientSocket?.close()
        serverSocket?.close()
    }
}