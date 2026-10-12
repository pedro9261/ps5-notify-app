package com.example.ps5notify

import android.app.Notification
import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifyListener : NotificationListenerService() {

    private val recent = HashMap<String, Long>()
    private val handler = Handler(Looper.getMainLooper())
    private var listenerConnected = false
    private lateinit var preferences: SharedPreferences

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "pollIntervalMinutes" && listenerConnected) schedulePolling()
    }

    private val pollNotifications = object : Runnable {
        override fun run() {
            activeNotifications?.forEach(::handleNotification)
            schedulePolling()
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = getSharedPreferences("ps5notify", MODE_PRIVATE)
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        listenerConnected = true
        schedulePolling()
    }

    override fun onListenerDisconnected() {
        listenerConnected = false
        handler.removeCallbacks(pollNotifications)
        requestRebind(ComponentName(this, NotifyListener::class.java))
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        listenerConnected = false
        handler.removeCallbacks(pollNotifications)
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handleNotification(sbn)
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!prefs.enabled) return
        if (sbn.packageName == packageName) return
        if (sbn.packageName !in prefs.allowedApps) return

        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        // Anti-duplicados (apps que actualizan la misma notificacion varias veces)
        val key = sbn.key + title + text
        val now = System.currentTimeMillis()
        if (now - (recent[key] ?: 0L) < 3000) return
        recent[key] = now
        if (recent.size > 100) recent.entries.removeAll { now - it.value > 10_000 }

        val app = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: PackageManager.NameNotFoundException) { sbn.packageName }

        val isCall = n.category == Notification.CATEGORY_CALL
        when {
            isCall -> Ps5Client.send(prefs, "call", app, title.ifBlank { "Llamada entrante" }, "")
            prefs.showContent -> Ps5Client.send(prefs, "msg", app, title, text)
            else -> Ps5Client.send(prefs, "msg", app, "Tienes un mensaje", "")
        }
    }

    private fun schedulePolling() {
        handler.removeCallbacks(pollNotifications)
        val delay = Prefs(this).pollIntervalMinutes * 60_000L
        handler.postDelayed(pollNotifications, delay)
    }
}
