package com.ashish.fpsoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Foreground service that draws the FPS counter in a system overlay window,
 * visible on top of other apps (requires SYSTEM_ALERT_WINDOW, granted by the
 * user via Settings before this service is started).
 */
class OverlayService : Service() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        private const val CHANNEL_ID = "fps_overlay_channel"
        private const val NOTIFICATION_ID = 1

        const val PREFS_NAME = "fps_overlay_prefs"
        const val KEY_SIZE = "font_size"
        const val KEY_FONT = "font_index"
        const val KEY_COLOR = "text_color"
        const val DEFAULT_SIZE = 18f
        val DEFAULT_COLOR: Int = Color.parseColor("#00FF00")
        val FONTS = arrayOf(Typeface.DEFAULT, Typeface.MONOSPACE, Typeface.SERIF, Typeface.SANS_SERIF)
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: TextView
    private lateinit var layoutParams: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences

    private var frameCount = 0
    private var lastTimeNs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastTimeNs == 0L) lastTimeNs = frameTimeNanos
            frameCount++
            val elapsedNs = frameTimeNanos - lastTimeNs
            if (elapsedNs >= 1_000_000_000L) {
                val fps = (frameCount * 1_000_000_000.0 / elapsedNs).roundToInt()
                overlayView.text = "$fps FPS"
                frameCount = 0
                lastTimeNs = frameTimeNanos
            }
            if (isRunning) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> applySettings() }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createOverlayView()
        applySettings()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        startForegroundWithNotification()

        isRunning = true
        frameCount = 0
        lastTimeNs = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        runCatching { windowManager.removeView(overlayView) }
        super.onDestroy()
    }

    private fun createOverlayView() {
        overlayView = TextView(this).apply {
            text = "0 FPS"
            setPadding(12, 12, 12, 12)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 16
            y = 16
        }

        // Lets the user drag the overlay anywhere on screen.
        var startX = 0
        var startY = 0
        var startTouchX = 0f
        var startTouchY = 0f
        overlayView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = startX + (event.rawX - startTouchX).roundToInt()
                    layoutParams.y = startY + (event.rawY - startTouchY).roundToInt()
                    runCatching { windowManager.updateViewLayout(overlayView, layoutParams) }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, layoutParams)
    }

    private fun applySettings() {
        val size = prefs.getFloat(KEY_SIZE, DEFAULT_SIZE)
        val fontIndex = prefs.getInt(KEY_FONT, 0).coerceIn(0, FONTS.size - 1)
        val color = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        overlayView.textSize = size
        overlayView.setTypeface(FONTS[fontIndex], Typeface.BOLD)
        overlayView.setTextColor(color)
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "FPS Overlay", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the FPS overlay visible while you use other apps"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }
}
