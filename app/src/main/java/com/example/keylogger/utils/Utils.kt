package com.example.keylogger.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.location.LocationManager
import android.view.accessibility.AccessibilityManager


fun Context.isAccessibilityServiceEnabled(service: Class<out AccessibilityService>): Boolean {
    val am = this.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
    for (enabledService in enabledServices) {
        val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
        if (enabledServiceInfo.packageName == this.packageName &&
            enabledServiceInfo.name == service.name
        ) {
            return true
        }
    }
    return false
}

fun Context.isGPSEnabled(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}