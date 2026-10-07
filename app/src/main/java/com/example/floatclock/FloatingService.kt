package com.example.floatclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {

    companion object {
        const val ACTION_STOP = "com.example.floatclock.action.STOP"
        const val ACTION_APPLY = "com.example.floatclock.action.APPLY"
        private const val CHANNEL_ID = "float_clock"
        private const val NOTIF_ID = 1

        @Volatile
        var running = false
            private set
    }

    private lateinit var wm: WindowManager
    private var root: LinearLayout? = null
    private lateinit var params: WindowManager.LayoutParams
    private var timeView: TextView? = null
    private var controlRow: LinearLayout? = null

    private var format24 = true
    private var showTenths = true

    private val handler = Handler(Looper.getMainLooper())

    // 模式一：定时刷新，对齐到 100ms 边界，0.1 秒间隔均匀、不漂移
    private val tick = object : Runnable {
        override fun run() {
            renderTime()
            handler.postDelayed(this, 100 - (SystemClock.elapsedRealtime() % 100))
        }
    }

    // 模式二：跟随屏幕垂直同步逐帧刷新（十分位变化时立即上屏，更顺滑）
    private var choreographer: Choreographer? = null
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            renderTime()
            choreographer?.postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        startAsForeground()
        buildOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_APPLY -> applySettings()
            else -> if (root == null && Settings.canDrawOverlays(this)) buildOverlay()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacks(tick)
        choreographer?.removeFrameCallback(frameCallback)
        root?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        root = null
        super.onDestroy()
    }

    private fun buildOverlay() {
        if (root != null) return

        val view = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root = view

        val tv = TextView(this).apply {
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }
        timeView = tv
        view.addView(tv)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        controlRow = row
        row.addView(controlButton(getString(R.string.btn_settings)) {
            row.visibility = View.GONE
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        })
        row.addView(controlButton(getString(R.string.btn_close)) { stopSelf() })
        view.addView(row)

        // 时钟区域：拖动移动位置，单击展开/收起按钮
        tv.setOnTouchListener(dragListener)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = Prefs.posX(this@FloatingService)
            y = Prefs.posY(this@FloatingService)
        }

        applySettings()
        wm.addView(view, params)
    }

    private fun applySettings() {
        format24 = Prefs.use24h(this)
        showTenths = Prefs.showTenths(this)
        applyTickMode()
        val tv = timeView ?: return
        tv.textSize = Prefs.textSize(this).toFloat()
        tv.setTextColor(Prefs.textColor(this))
        // 深色阴影保证在任何背景上可读
        tv.setShadowLayer(6f, 2f, 2f, 0xCC000000.toInt())
        tv.setPadding(dp(10), dp(4), dp(10), dp(4))
        tv.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.argb(Prefs.bgAlpha(this@FloatingService), 0, 0, 0))
        }
        renderTime()
    }

    private fun applyTickMode() {
        handler.removeCallbacks(tick)
        choreographer?.removeFrameCallback(frameCallback)
        if (Prefs.frameSync(this)) {
            if (choreographer == null) choreographer = Choreographer.getInstance()
            choreographer?.postFrameCallback(frameCallback)
        } else {
            handler.post(tick)
        }
    }

    private fun renderTime() {
        val tv = timeView ?: return
        tv.text = ClockFormat.format(format24, showTenths, System.currentTimeMillis())
    }

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    private val dragListener = View.OnTouchListener { _, e ->
        val view = root ?: return@OnTouchListener false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = e.rawX
                downRawY = e.rawY
                startX = params.x
                startY = params.y
                dragging = false
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downRawX
                val dy = e.rawY - downRawY
                if (!dragging && (abs(dx) > 8 || abs(dy) > 8)) dragging = true
                if (dragging) {
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    try {
                        wm.updateViewLayout(view, params)
                    } catch (_: Exception) {
                    }
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                if (dragging) {
                    Prefs.setPos(this@FloatingService, params.x, params.y)
                } else {
                    controlRow?.visibility =
                        if (controlRow?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                }
                true
            }
            else -> false
        }
    }

    private fun controlButton(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(6), dp(16), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(0x99000000.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = dp(10) }
            setOnClickListener { onClick() }
        }

    private fun startAsForeground() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        startForeground(NOTIF_ID, notif)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
