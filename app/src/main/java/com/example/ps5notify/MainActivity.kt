package com.example.ps5notify

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    data class AppItem(val label: String, val pkg: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        fun field(hint: String, value: String, type: Int = InputType.TYPE_CLASS_TEXT) =
            EditText(this).apply { this.hint = hint; setText(value); inputType = type }

        val host = field("IP de la PS5 (ej. 192.168.1.50)", prefs.host)
        val port = field("Puerto", prefs.port.toString(), InputType.TYPE_CLASS_NUMBER)
        val token = field("Token (igual que en el payload)", prefs.token)

        val enabled = Switch(this).apply { text = "Reenviar notificaciones"; isChecked = prefs.enabled }
        val content = Switch(this).apply {
            text = "Mostrar contenido del mensaje en la TV"; isChecked = prefs.showContent
        }

        val btnAccess = Button(this).apply {
            text = "1) Dar acceso a notificaciones"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        val btnTest = Button(this).apply {
            text = "2) Guardar y enviar prueba"
            setOnClickListener {
                save(prefs, host, port, token, enabled, content)
                Ps5Client.send(prefs, "msg", "PS5 Notify", "Prueba", "Funciona!")
                Toast.makeText(context, "Enviado", Toast.LENGTH_SHORT).show()
            }
        }

        // Lista de apps con checkbox
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }

        val allowed = prefs.allowedApps.toMutableSet()
        val list = ListView(this).apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            adapter = ArrayAdapter(
                context, android.R.layout.simple_list_item_multiple_choice,
                apps.map { it.label }
            )
            apps.forEachIndexed { i, a -> setItemChecked(i, a.pkg in allowed) }
            setOnItemClickListener { _, _, pos, _ ->
                if (isItemChecked(pos)) allowed.add(apps[pos].pkg) else allowed.remove(apps[pos].pkg)
                prefs.allowedApps = allowed.toSet()
            }
        }

        listOf(host, port, token, enabled, content, btnAccess, btnTest,
            TextView(this).apply { text = "Apps permitidas:"; setPadding(0, pad, 0, 0) })
            .forEach { root.addView(it) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun save(p: Prefs, h: EditText, po: EditText, t: EditText, e: Switch, c: Switch) {
        p.host = h.text.toString().trim()
        p.port = po.text.toString().toIntOrNull() ?: 9055
        p.token = t.text.toString().trim()
        p.enabled = e.isChecked
        p.showContent = c.isChecked
    }
}
