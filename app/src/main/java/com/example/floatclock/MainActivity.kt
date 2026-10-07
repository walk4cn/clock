package com.example.floatclock

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private var pendingStart = false

    private lateinit var statusView: TextView
    private lateinit var preview: TextView
    private lateinit var sizeValue: TextView
    private lateinit var alphaValue: TextView

    private val colorOptions = intArrayOf(
        0xFFFFFFFF.toInt(), // 白
        0xFFFFEB3B.toInt(), // 黄
        0xFF69F0AE.toInt(), // 绿
        0xFF40C4FF.toInt(), // 蓝
        0xFFFF5252.toInt(), // 红
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        statusView = findViewById(R.id.status)
        preview = findViewById(R.id.preview)
        sizeValue = findViewById(R.id.size_value)
        alphaValue = findViewById(R.id.alpha_value)

        val sizeBar = findViewById<SeekBar>(R.id.size_bar)
        val alphaBar = findViewById<SeekBar>(R.id.alpha_bar)
        val sw24 = findViewById<Switch>(R.id.sw_24h)
        val swTenths = findViewById<Switch>(R.id.sw_tenths)

        sizeBar.max = 60
        sizeBar.progress = Prefs.textSize(this) - 12
        sizeBar.setOnSeekBarChangeListener(seekListener { p ->
            Prefs.setTextSize(this, p + 12)
            refreshPreview()
        })

        alphaBar.max = 255
        alphaBar.progress = Prefs.bgAlpha(this)
        alphaBar.setOnSeekBarChangeListener(seekListener { p ->
            Prefs.setBgAlpha(this, p)
            refreshPreview()
        })

        sw24.isChecked = Prefs.use24h(this)
        sw24.setOnCheckedChangeListener { _, checked ->
            Prefs.setUse24h(this, checked)
            refreshPreview()
        }

        swTenths.isChecked = Prefs.showTenths(this)
        swTenths.setOnCheckedChangeListener { _, checked ->
            Prefs.setShowTenths(this, checked)
            refreshPreview()
        }

        val swFrame = findViewById<Switch>(R.id.sw_frame)
        swFrame.isChecked = Prefs.frameSync(this)
        swFrame.setOnCheckedChangeListener { _, checked ->
            Prefs.setFrameSync(this, checked)
        }

        val colorRow = findViewById<LinearLayout>(R.id.color_row)
        for (i in 0 until colorRow.childCount) {
            val dot = colorRow.getChildAt(i) as TextView
            dot.setTextColor(colorOptions[i])
            dot.setOnClickListener {
                Prefs.setTextColor(this, colorOptions[i])
                refreshColorDots(colorRow)
                refreshPreview()
            }
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener { startFloating() }
        findViewById<Button>(R.id.btn_stop).setOnClickListener {
            if (FloatingService.running) {
                startService(
                    Intent(this, FloatingService::class.java)
                        .setAction(FloatingService.ACTION_STOP)
                )
            }
        }

        refreshColorDots(colorRow)
        refreshPreview()
    }

    override fun onResume() {
        super.onResume()
        val granted = Settings.canDrawOverlays(this)
        statusView.text = getString(if (granted) R.string.status_granted else R.string.status_denied)
        statusView.setTextColor(if (granted) 0xFF2E7D32.toInt() else 0xFFC62828.toInt())

        refreshPreview()

        if (pendingStart) {
            if (granted) {
                pendingStart = false
                launchService(Intent(this, FloatingService::class.java))
            }
        } else if (FloatingService.running) {
            // 从设置页返回，把最新样式同步到悬浮窗
            launchService(
                Intent(this, FloatingService::class.java)
                    .setAction(FloatingService.ACTION_APPLY)
            )
        }
    }

    private fun startFloating() {
        if (Settings.canDrawOverlays(this)) {
            launchService(Intent(this, FloatingService::class.java))
        } else {
            pendingStart = true
            Toast.makeText(this, R.string.need_overlay_permission, Toast.LENGTH_LONG).show()
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun launchService(intent: Intent) {
        try {
            startService(intent)
            if (intent.action == null) {
                Toast.makeText(this, R.string.started, Toast.LENGTH_SHORT).show()
                // 启动成功后自动收起设置页
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "error", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshPreview() {
        preview.text = ClockFormat.format(
            Prefs.use24h(this), Prefs.showTenths(this), System.currentTimeMillis()
        )
        preview.textSize = Prefs.textSize(this).toFloat()
        preview.setTextColor(Prefs.textColor(this))
        (preview.background as? GradientDrawable)?.setColor(
            Color.argb(Prefs.bgAlpha(this), 0, 0, 0)
        )
        sizeValue.text = getString(R.string.text_size_value, Prefs.textSize(this))
        alphaValue.text = getString(
            R.string.bg_alpha_value, (Prefs.bgAlpha(this) * 100f / 255).roundToInt()
        )
    }

    private fun refreshColorDots(row: LinearLayout) {
        val current = Prefs.textColor(this)
        for (i in 0 until row.childCount) {
            row.getChildAt(i).alpha = if (colorOptions[i] == current) 1f else 0.35f
        }
    }

    private fun seekListener(onChange: (Int) -> Unit): SeekBar.OnSeekBarChangeListener =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChange(progress)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) = Unit

            override fun onStopTrackingTouch(sb: SeekBar?) = Unit
        }
}
