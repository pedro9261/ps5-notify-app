package com.example.ps5notify

import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

object Ps5Client {
    private val executor = Executors.newSingleThreadExecutor()

    private fun clean(s: String, max: Int) =
        s.replace(Regex("[\\t\\r\\n]+"), " ").take(max)

    fun send(prefs: Prefs, kind: String, app: String, title: String, text: String) {
        val host = prefs.host
        if (host.isBlank()) return
        val line = listOf(
            prefs.token, kind, clean(app, 40), clean(title, 80), clean(text, 160)
        ).joinToString("\t") + "\n"

        executor.execute {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, prefs.port), 1500)
                    s.getOutputStream().apply {
                        write(line.toByteArray(Charsets.UTF_8)); flush()
                    }
                }
            } catch (e: Exception) {
                // PS5 apagada / sin red / payload no cargado: se ignora
            }
        }
    }
}
