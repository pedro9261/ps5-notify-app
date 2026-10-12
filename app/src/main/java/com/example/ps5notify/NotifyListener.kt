package com.example.ps5notify

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifyListener : NotificationListenerService() {

    private val recent = HashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
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
}
