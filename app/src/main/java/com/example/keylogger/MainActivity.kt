package com.example.keylogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.keylogger.ui.theme.KeyloggerTheme
import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find and set up the Server button
        findViewById<Button>(R.id.btnServer).setOnClickListener {
            val intent = Intent(this, ServerActivity::class.java)
            startActivity(intent)
        }

        // Find and set up the Client button
        findViewById<Button>(R.id.btnClient).setOnClickListener {
            val intent = Intent(this, ClientActivity::class.java)
            startActivity(intent)
        }
    }
}