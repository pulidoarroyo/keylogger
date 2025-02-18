package com.example.keylogger

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class ServerActivity : AppCompatActivity() {
    private var serverSocket: ServerSocket? = null  // Server socket for listening for client connections
    private var clientSocket: Socket? = null  // Socket for handling the client connection
    private val serverPort = 9999  // Port number on which the server listens
    private var printWriter: PrintWriter? = null  // PrintWriter to send data to the client
    private var isClientConnected = false  // Flag to track whether a client is connected
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())  // Coroutine scope for background tasks
    private var isServerRunning = false  // Flag to track the server's running state

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_server)  // Set the layout for the server activity

        val ipAddress = getLocalIpAddress()  // Get the local IP address of the server

        // Find views for UI elements
        val ipText = findViewById<TextView>(R.id.tvIP)
        val statusText = findViewById<TextView>(R.id.tvStatus)
        val inputEditText = findViewById<EditText>(R.id.etInput)
        val stopServerButton = findViewById<Button>(R.id.btnStopServer)

        // Display the server's IP address in a large, easy-to-read format
        ipText.text = "Server IP: $ipAddress"
        statusText.text = "Status: Waiting for client..."  // Initial status message

        // Start the server in a background coroutine
        coroutineScope.launch { startServer() }

        // Continuously check the client's connection status
        coroutineScope.launch {
            while (isActive) {
                delay(1000)  // Check every second
                checkClientStatus()
            }
        }

        // Handle key events from the input text field (e.g., for special characters like BACKSPACE, ENTER, etc.)
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
                specialKey?.let { sendToClient(it) }  // Send special key information to the client
            }
            false
        }

        // Monitor text input and send it to the client when new text is entered
        inputEditText.addTextChangedListener(object : TextWatcher {
            private var previousText = ""  // Keep track of the previous text to detect changes

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousText = s?.toString() ?: ""  // Store the previous text before changes
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s?.toString() ?: ""
                if (currentText.length > previousText.length) {  // If new text was added
                    val newText = currentText.substring(start, start + count)
                    sendToClient(newText)  // Send the new text to the client
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Stop the server when the stop button is clicked
        stopServerButton.setOnClickListener {
            stopServer()  // Stop the server
            navigateToMainActivity()  // Navigate to the main activity
        }
    }

    // Start the server to listen for client connections
    private suspend fun startServer() {
        try {
            serverSocket = ServerSocket(serverPort)  // Create a server socket to listen on the given port
            isServerRunning = true  // Set the server as running
            while (isServerRunning) {
                clientSocket = serverSocket?.accept()  // Wait for a client to connect
                isClientConnected = true  // A client has connected
                printWriter = PrintWriter(clientSocket?.getOutputStream(), true)  // Prepare to send data to the client

                // Update UI to reflect client connection
                runOnUiThread {
                    findViewById<TextView>(R.id.tvStatus).text = "Status: Client connected!"
                }
            }
        } catch (e: Exception) {
            logError("Error starting server: ${e.message}")  // Log error if the server fails to start
        }
    }

    // Stop the server and clean up resources
    private fun stopServer() {
        try {
            if (isClientConnected) {  // If a client is connected, inform them that the server is stopping
                sendToClient("Server is stopping. You have been disconnected.")
            }

            isServerRunning = false  // Mark the server as stopped
            serverSocket?.close()  // Close the server socket
            serverSocket = null

            // Update the UI to reflect that the server has stopped
            runOnUiThread {
                findViewById<TextView>(R.id.tvStatus).text = "Status: Server stopped."
            }

        } catch (e: Exception) {
            logError("Error stopping server: ${e.message}")  // Log error if the server fails to stop
        }
    }

    // Navigate to the main activity
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)  // Create an intent to navigate to the MainActivity
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()  // Close the ServerActivity
    }

    // Check if the client is still connected
    private fun checkClientStatus() {
        if (clientSocket == null || clientSocket?.isClosed == true || printWriter?.checkError() == true) {
            isClientConnected = false  // Mark client as disconnected
            closeClientConnection()  // Close the client connection
            runOnUiThread {
                findViewById<TextView>(R.id.tvStatus).text = "Status: Client disconnected. Waiting for new client..."
            }
            if (isServerRunning) {
                coroutineScope.launch { startServer() }  // Restart the server to wait for a new client
            }
        }
    }

    // Send a message to the client
    private fun sendToClient(message: String) {
        coroutineScope.launch {
            try {
                printWriter?.println(message)  // Send the message to the client
            } catch (e: Exception) {
                logError("Error sending data: ${e.message}")  // Log error if message sending fails
                isClientConnected = false
                closeClientConnection()  // Close the client connection if an error occurs
            }
        }
    }

    // Get the local IP address of the device
    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filter { it is Inet4Address && !it.isLoopbackAddress }
                .map { it.hostAddress }
                .firstOrNull { it.startsWith("192.168") || it.startsWith("172.") || it.startsWith("10.") }
                ?: "IP not found"
        } catch (e: Exception) {
            logError("Error fetching IP: ${e.message}")
            "IP not found"
        }
    }

    // Close the client connection
    private fun closeClientConnection() {
        try {
            printWriter?.close()  // Close the PrintWriter
            clientSocket?.close()  // Close the client socket
            printWriter = null
            clientSocket = null
        } catch (e: Exception) {
            logError("Error closing client connection: ${e.message}")  // Log error if closing the connection fails
        }
    }

    // Clean up resources when the activity is destroyed
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()  // Cancel all coroutines
        closeClientConnection()  // Close the client connection
        serverSocket?.close()  // Close the server socket
    }

    // Log error messages (could be replaced with a proper logging mechanism)
    private fun logError(message: String) {
        println("Error: $message")  // Log the error to the console
    }
}
