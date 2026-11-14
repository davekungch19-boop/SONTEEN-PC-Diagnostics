package com.sonteen.pcdiagnostics

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val purple = Color.rgb(119, 72, 220)
    private val bg = Color.rgb(16, 12, 28)
    private val panel = Color.rgb(34, 26, 51)
    private lateinit var content: LinearLayout
    private lateinit var rootStatus: TextView

    private val properties = listOf(
        "ro.product.cpu.abi",
        "ro.product.cpu.abilist",
        "ro.product.cpu.abilist32",
        "ro.product.cpu.abilist64",
        "ro.boot.hypervisor",
        "ro.boot.qemu",
        "ro.kernel.qemu",
        "ro.dalvik.vm.isa.arm",
        "ro.hardware",
        "ro.boot.hardware",
        "ro.product.model",
        "ro.product.brand",
        "ro.product.name",
        "ro.product.device"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        refreshData()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 28)
        }
        scroll.addView(content)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "SONTEEN"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }
        content.addView(title)
        content.addView(label("PC DIAGNOSTICS  •  READ ONLY", 13f, purple, true))
        content.addView(label("ตรวจสอบข้อมูล Android และสถานะ Root โดยไม่แก้ไขค่าระบบ", 14f, Color.LTGRAY, false))

        rootStatus = label("กำลังตรวจสอบ Root…", 16f, Color.WHITE, true)
        addPanel(rootStatus)

        val refresh = button("รีเฟรชข้อมูล") { refreshData() }
        content.addView(refresh)
        val export = button("บันทึกรายงาน") { exportReport() }
        content.addView(export)

        content.addView(label("SYSTEM PROPERTIES", 16f, purple, true))
        content.addView(label("ค่าด้านล่างอ่านจากเครื่องจริงเท่านั้น ไม่มีการปลอมตัวเป็น PC", 12f, Color.LTGRAY, false))
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 8)
        }

    private fun addPanel(view: TextView) {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(panel)
        }
        wrap.addView(view)
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 18; bottomMargin = 12 }
        content.addView(wrap, lp)
    }

    private fun button(text: String, action: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(purple)
        isAllCaps = false
        setOnClickListener { action() }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8; bottomMargin = 6 }
        layoutParams = lp
    }

    private fun runCommand(vararg args: String): String {
        return try {
            val p = ProcessBuilder(*args).redirectErrorStream(true).start()
            val output = p.inputStream.bufferedReader().use { it.readText() }
            if (!p.waitFor(4, java.util.concurrent.TimeUnit.SECONDS)) {
                p.destroyForcibly()
                "TIMEOUT"
            } else output.trim()
        } catch (e: Exception) {
            "UNAVAILABLE"
        }
    }

    private fun refreshData() {
        val uid = runCommand("su", "-c", "id")
        val rooted = uid.contains("uid=0")
        rootStatus.text = if (rooted) "ROOT: GRANTED\n$uid" else "ROOT: NOT GRANTED"
        rootStatus.setTextColor(if (rooted) Color.rgb(100, 220, 140) else Color.rgb(255, 190, 90))

        while (content.childCount > 5) content.removeViewAt(5)
        properties.forEach { key ->
            val value = runCommand("getprop", key).ifBlank { "(ว่าง)" }
            val row = TextView(this).apply {
                text = "$key\n$value"
                textSize = 13f
                setTextColor(Color.WHITE)
                setPadding(12, 10, 12, 10)
                setBackgroundColor(panel)
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 5 }
            content.addView(row, lp)
        }
    }

    private fun exportReport() {
        val lines = mutableListOf<String>()
        lines += "SONTEEN PC Diagnostics"
        lines += "Generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        lines += "Root check: " + runCommand("su", "-c", "id")
        properties.forEach { key -> lines += "$key=${runCommand("getprop", key)}" }
        try {
            val out = File(filesDir, "sonteen_device_report.txt")
            out.writeText(lines.joinToString("\n"))
            Toast.makeText(this, "บันทึกแล้ว: ${out.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "บันทึกไม่สำเร็จ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
