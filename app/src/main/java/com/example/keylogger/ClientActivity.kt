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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client)

        val connectButton = findViewById<Button>(R.id.btnConnect)
        val ipInput = findViewById<EditText>(R.id.etServerIP)
        val outputText = findViewById<TextView>(R.id.tvOutput)

        connectButton.setOnClickListener {
            val serverIP = ipInput.text.toString()
            if (serverIP.isNotEmpty()) {
                connectButton.isEnabled = false
                connectButton.text = "Connecting..."

                // Start connection in background
                CoroutineScope(Dispatchers.IO).launch {
                    connectToServer(serverIP)
                }
            }
        }
    }

    private fun connectToServer(serverIP: String) {
        try {
            // Connect to server
            clientSocket = Socket(serverIP, serverPort)

            // Update UI to show connected status
            runOnUiThread {
                findViewById<Button>(R.id.btnConnect).text = "Connected"
                findViewById<EditText>(R.id.etServerIP).isEnabled = false
            }

            // Start receiving messages
            val reader = BufferedReader(InputStreamReader(clientSocket?.getInputStream()))

            while (true) {
                val receivedText = reader.readLine() ?: break
                runOnUiThread {
                    val outputText = findViewById<TextView>(R.id.tvOutput)
                    outputText.append("$receivedText\n")

                    // Find ScrollView directly and scroll to bottom
                    val scrollView = findViewById<ScrollView>(R.id.scrollView)
                    scrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                val button = findViewById<Button>(R.id.btnConnect)
                button.text = "Connect"
                button.isEnabled = true
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        clientSocket?.close()
    }
}