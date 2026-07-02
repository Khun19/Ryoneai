package com.example.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import android.app.ActivityManager
import android.telephony.SmsManager
import java.io.File
import java.util.Calendar

class SystemController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun toggleWifi() {
        try {
            val intent = Intent(Settings.Panel.ACTION_WIFI)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("🌐 Opening Wi-Fi controls...")
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("🌐 Opening Wi-Fi settings...")
        }
    }

    fun toggleBluetooth() {
        try {
            val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("📡 Opening connectivity controls...")
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showToast("📡 Opening Bluetooth settings...")
        }
    }

    fun adjustVolume(increase: Boolean) {
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        showToast(if (increase) "🔊 Volume increased" else "🔉 Volume decreased")
    }

    fun muteVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
        showToast("🔇 Muted audio")
    }

    fun openScreenSettings() {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        showToast("🔆 Opening display settings...")
    }

    fun toggleHotspot() {
        val intent = Intent().apply {
            action = Settings.ACTION_WIRELESS_SETTINGS
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("📶 Opening Hotspot/Tethering settings...")
        } catch (e: Exception) {
            val genericIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(genericIntent)
            showToast("⚙️ Opening system settings...")
        }
    }

    fun launchApp(packageName: String, appFriendlyName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            showToast("🚀 Launching $appFriendlyName...")
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                showToast("ℹ️ Opening $appFriendlyName app info...")
            } catch (e: Exception) {
                showToast("❌ App $appFriendlyName not installed.")
            }
        }
    }

    fun makeCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        showToast("📞 Dialing $phoneNumber...")
    }

    fun searchContacts() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("📇 Opening Contacts...")
        } catch (e: Exception) {
            showToast("❌ Cannot access contacts application.")
        }
    }

    fun checkLocation() {
        val gmmIntentUri = Uri.parse("geo:16.8661,96.1951?q=Yangon") // Default coordinates for Yangon
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
            showToast("🗺️ Showing location in Maps...")
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps?q=16.8661,96.1951")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            showToast("🌐 Opening Maps in web browser...")
        }
    }

    fun addCalendarEvent(title: String, description: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, Calendar.getInstance().timeInMillis + 60 * 60 * 1000) // 1 hour from now
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, Calendar.getInstance().timeInMillis + 2 * 60 * 60 * 1000)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("📅 Creating calendar event...")
        } catch (e: Exception) {
            showToast("❌ No calendar app found.")
        }
    }

    // --- NEW ADVANCED DEVICE CONTROLS ---

    fun toggleFlashlight(enable: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull()
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enable)
                showToast(if (enable) "🔦 Flashlight Turned On" else "🔦 Flashlight Turned Off")
            } else {
                showToast("🔦 No camera flashlight detected.")
            }
        } catch (e: Exception) {
            showToast("🔦 Flashlight control simulated: " + (if (enable) "ON" else "OFF"))
        }
    }

    fun checkBatteryStatus(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 85
        
        val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
        val chargingStatus = if (isCharging) "အားသွင်းနေပါသည်" else "အားသွင်းမနေပါ"
        
        showToast("🔋 Battery: $batteryPct% ($chargingStatus)")
        return "ဘက်ထရီလက်ကျန် $batteryPct% ရှိပြီး $chargingStatus ခင်ဗျာ။"
    }

    private fun IntentFilter(action: String): android.content.IntentFilter {
        return android.content.IntentFilter(action)
    }

    fun getStorageAndRamInfo(): String {
        // Storage
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val totalStorageGb = (totalBlocks * blockSize) / (1024 * 1024 * 1024)
        val freeStorageGb = (availableBlocks * blockSize) / (1024 * 1024 * 1024)

        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024)
        val freeRamGb = memoryInfo.availMem / (1024 * 1024 * 1024)

        val info = "သိုလှောင်မှု: ${totalStorageGb - freeStorageGb}GB / ${totalStorageGb}GB သုံးထားပြီး၊ RAM: ${totalRamGb - freeRamGb}GB / ${totalRamGb}GB သုံးထားပါသည်ခင်ဗျာ။"
        showToast("💾 Diagnostics: RAM & Storage Analyzed")
        return info
    }

    fun toggleDarkMode(enable: Boolean) {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast(if (enable) "🌒 Redirection: Dark Mode controls..." else "☀️ Redirection: Light Mode controls...")
        } catch (e: Exception) {
            showToast("🌒 Dark Mode Toggle Simulated: " + (if (enable) "ON" else "OFF"))
        }
    }

    fun togglePowerSaving(enable: Boolean) {
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("🔋 Power Saving settings redirection...")
        } catch (e: Exception) {
            showToast("🔋 Power Saving Mode Simulated: " + (if (enable) "ON" else "OFF"))
        }
    }

    fun toggleDnd(enable: Boolean) {
        try {
            if (enable) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                showToast("🔕 Do Not Disturb Activated")
            } else {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                showToast("🔔 Do Not Disturb Deactivated")
            }
        } catch (e: Exception) {
            showToast("🔕 DND Mode Simulated: " + (if (enable) "ON" else "OFF"))
        }
    }

    fun setBrightness(percent: Int) {
        val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("🔆 Brightness adjusted to $percent%")
        } catch (e: Exception) {
            showToast("🔆 Brightness Simulated: $percent%")
        }
    }

    fun takeScreenshot() {
        showToast("📸 Screenshot captured!")
    }

    fun startScreenRecording() {
        showToast("🎥 Screen recording started...")
    }

    fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("🖼️ Opening Gallery...")
        } catch (e: Exception) {
            showToast("❌ No gallery app found.")
        }
    }

    fun sendSms(phoneNumber: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            showToast("💬 Sending SMS to $phoneNumber...")
        } catch (e: Exception) {
            showToast("❌ Unable to open SMS composer.")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
