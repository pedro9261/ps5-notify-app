package com.example.ps5notify

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    data class AppItem(val label: String, val pkg: String)

    private val ink = Color.rgb(25, 35, 52)
    private val muted = Color.rgb(100, 113, 132)
    private val blue = Color.rgb(39, 103, 238)
    private val page = Color.rgb(244, 247, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.statusBarColor = page
        window.navigationBarColor = page
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val prefs = Prefs(this)
        val pad = dp(20)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(page)
            isFillViewport = true
            addView(content)
        }

        content.addView(TextView(this).apply {
            text = "PS5 Notify"
            setTextColor(ink)
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "Notificaciones del móvil, en tu pantalla."
            setTextColor(muted)
            textSize = 15f
            setPadding(0, dp(4), 0, dp(12))
        })
        content.addView(TextView(this).apply {
            text = "BETA"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(blue)
            background = rounded(Color.rgb(226, 237, 255), dp(20))
            setPadding(dp(12), dp(6), dp(12), dp(6))
        }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(16) })
        content.addView(TextView(this).apply {
            text = "La recepción en tiempo real sigue activa. También puedes volver a revisar las notificaciones activas cada cierto tiempo."
            setTextColor(ink)
            textSize = 14f
            background = rounded(Color.rgb(232, 241, 255), dp(16))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })

        val host = input("IP de la PS5", prefs.host, "Ej. 192.168.1.50")
        val port = input("Puerto", prefs.port.toString(), "9055", InputType.TYPE_CLASS_NUMBER)
        val token = input("Token (solo lectura)", prefs.token, "Fijo para coincidir con el bridge").apply {
            isEnabled = false
        }
        val interval = input(
            "Verificar también cada (minutos)",
            prefs.pollIntervalMinutes.toString(),
            "Mínimo: 1",
            InputType.TYPE_CLASS_NUMBER
        )

        val enabled = Switch(this).apply {
            text = "Reenviar notificaciones"
            textSize = 15f
            setTextColor(ink)
            isChecked = prefs.enabled
        }
        val showContent = Switch(this).apply {
            text = "Mostrar el contenido del mensaje en la TV"
            textSize = 15f
            setTextColor(ink)
            isChecked = prefs.showContent
        }

        val configCard = card()
        sectionHeading(configCard, "Conexión")
        addField(configCard, host)
        addField(configCard, port)
        addField(configCard, token)
        sectionHeading(configCard, "Comprobación")
        addField(configCard, interval)
        configCard.addView(TextView(this).apply {
            text = "Por defecto: cada 2 minutos. La recepción en tiempo real no se desactiva."
            setTextColor(muted)
            textSize = 13f
            setPadding(0, dp(2), 0, dp(12))
        })
        configCard.addView(enabled, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(4)
            bottomMargin = dp(8)
        })
        configCard.addView(showContent, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(8)
        })
        content.addView(configCard, bottomSpacing())

        val actionsCard = card()
        sectionHeading(actionsCard, "Preparar y probar")
        actionsCard.addView(TextView(this).apply {
            text = "Concede el acceso a notificaciones y guarda la configuración antes de probar."
            setTextColor(muted)
            textSize = 13f
            setPadding(0, 0, 0, dp(12))
        })
        val accessButton = actionButton("Conceder acceso a notificaciones", secondary = true)
        accessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        actionsCard.addView(accessButton, buttonSpacing())

        val saveButton = actionButton("Guardar configuración")
        saveButton.setOnClickListener {
            if (save(prefs, host, port, interval, enabled, showContent)) {
                Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            }
        }
        actionsCard.addView(saveButton, buttonSpacing())

        val testButton = actionButton("Guardar y enviar notificación de prueba")
        testButton.setOnClickListener {
            if (save(prefs, host, port, interval, enabled, showContent)) {
                Ps5Client.send(prefs, "msg", "PS5 Notify", "Prueba", "La conexión funciona")
                Toast.makeText(this, "Prueba enviada", Toast.LENGTH_SHORT).show()
            }
        }
        actionsCard.addView(testButton)
        content.addView(actionsCard, bottomSpacing())

        val appsCard = card()
        sectionHeading(appsCard, "Aplicaciones permitidas")
        appsCard.addView(TextView(this).apply {
            text = "Selecciona de qué aplicaciones se reenviarán notificaciones."
            setTextColor(muted)
            textSize = 13f
            setPadding(0, 0, 0, dp(8))
        })
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
        val allowed = prefs.allowedApps.toMutableSet()
        apps.forEach { app ->
            appsCard.addView(CheckBox(this).apply {
                text = app.label
                textSize = 14f
                setTextColor(ink)
                isChecked = app.pkg in allowed
                setOnCheckedChangeListener { _, checked ->
                    if (checked) allowed.add(app.pkg) else allowed.remove(app.pkg)
                    prefs.allowedApps = allowed.toSet()
                }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        content.addView(appsCard)
        setContentView(scroll)
    }

    private fun input(
        label: String,
        value: String,
        hint: String,
        type: Int = InputType.TYPE_CLASS_TEXT
    ): EditText {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        wrap.addView(TextView(this).apply {
            text = label
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ink)
        })
        val edit = EditText(this).apply {
            setText(value)
            this.hint = hint
            inputType = type
            textSize = 15f
            setTextColor(ink)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.rgb(247, 249, 252), dp(12), Color.rgb(223, 229, 238))
        }
        wrap.addView(edit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
        edit.tag = wrap
        return edit
    }

    private fun addField(card: LinearLayout, field: EditText) {
        val wrapper = field.tag as LinearLayout
        card.addView(wrapper, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(12)
        })
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(Color.WHITE, dp(18))
    }

    private fun sectionHeading(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ink)
            setPadding(0, 0, 0, dp(12))
        })
    }

    private fun actionButton(label: String, secondary: Boolean = false) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(if (secondary) blue else Color.WHITE)
        background = rounded(
            if (secondary) Color.rgb(232, 241, 255) else blue,
            dp(12)
        )
        minHeight = dp(48)
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            stroke?.let { setStroke(dp(1), it) }
        }

    private fun bottomSpacing() = LinearLayout.LayoutParams(-1, -2).apply {
        bottomMargin = dp(14)
    }

    private fun buttonSpacing() = LinearLayout.LayoutParams(-1, -2).apply {
        bottomMargin = dp(8)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun save(
        prefs: Prefs,
        host: EditText,
        port: EditText,
        interval: EditText,
        enabled: Switch,
        showContent: Switch
    ): Boolean {
        val minutes = interval.text.toString().toIntOrNull()
        if (minutes == null || minutes < 1) {
            interval.error = "El intervalo debe ser de al menos 1 minuto"
            interval.requestFocus()
            return false
        }

        prefs.host = host.text.toString().trim()
        prefs.port = port.text.toString().toIntOrNull() ?: 9055
        prefs.pollIntervalMinutes = minutes
        prefs.enabled = enabled.isChecked
        prefs.showContent = showContent.isChecked
        return true
    }
}
