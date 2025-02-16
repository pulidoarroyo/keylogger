package com.example.keylogger

import android.widget.ScrollView
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class ClientActivity : AppCompatActivity() {
    private var clientSocket: Socket? = null
    private val serverPort = 9999
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        val connectButton = findViewById<Button>(R.id.btnConnect)
        val disconnectButton = findViewById<Button>(R.id.btnDisconnect)
        val ipInput = findViewById<EditText>(R.id.etServerIP)
        val outputText = findViewById<TextView>(R.id.tvOutput)

        connectButton.setOnClickListener {
            val serverIP = ipInput.text.toString()
            if (serverIP.isNotEmpty()) {
                connectButton.isEnabled = false
                connectButton.text = "Connecting..."

                CoroutineScope(Dispatchers.IO).launch {
                    connectToServer(serverIP)
                }
            }
        }

        disconnectButton.setOnClickListener {
            disconnectFromServer()
        }
    }

    private fun connectToServer(serverIP: String) {
        try {
            clientSocket = Socket(serverIP, serverPort)
            isConnected = true

            runOnUiThread {
                findViewById<Button>(R.id.btnConnect).text = "Connected"
                findViewById<EditText>(R.id.etServerIP).isEnabled = false
                findViewById<Button>(R.id.btnDisconnect).isEnabled = true

                val outputText = findViewById<TextView>(R.id.tvOutput)
                outputText.append("Connected to server at $serverIP\n")
            }

            val reader = BufferedReader(InputStreamReader(clientSocket?.getInputStream()))

            while (isConnected) {
                val receivedText = reader.readLine() ?: break
                runOnUiThread {
                    val outputText = findViewById<TextView>(R.id.tvOutput)
                    outputText.append("$receivedText\n")

                    val scrollView = findViewById<ScrollView>(R.id.scrollView)
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            resetConnection("Connection failed: ${e.message}")
        }
    }

    private fun disconnectFromServer() {
        isConnected = false
        clientSocket?.close()
        resetConnection("Disconnected from server")
    }

    private fun resetConnection(message: String) {
        runOnUiThread {
            val button = findViewById<Button>(R.id.btnConnect)
            button.text = "Connect"
            button.isEnabled = true

            findViewById<Button>(R.id.btnDisconnect).isEnabled = false
            findViewById<EditText>(R.id.etServerIP).isEnabled = true

            val outputText = findViewById<TextView>(R.id.tvOutput)
            outputText.append("$message\n")

            val scrollView = findViewById<ScrollView>(R.id.scrollView)
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromServer()
    }
}