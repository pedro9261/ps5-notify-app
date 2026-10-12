package com.example.ps5notify

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("ps5notify", Context.MODE_PRIVATE)

    var host: String
        get() = sp.getString("host", "") ?: ""
        set(v) = sp.edit().putString("host", v).apply()

    var port: Int
        get() = sp.getInt("port", 9055)
        set(v) = sp.edit().putInt("port", v).apply()

    var token: String
        get() = sp.getString("token", "ps5notify") ?: ""
        set(v) = sp.edit().putString("token", v).apply()

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    /** false = solo "Nuevo mensaje de X" (privacidad: la TV la ve cualquiera) */
    var showContent: Boolean
        get() = sp.getBoolean("showContent", false)
        set(v) = sp.edit().putBoolean("showContent", v).apply()

    var allowedApps: Set<String>
        get() = sp.getStringSet("allowed", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("allowed", v).apply()
}
