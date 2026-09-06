package com.ashish.fpsoverlay

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Bare-minimum controller UI: request the overlay/notification permissions,
 * start or stop the system-wide FPS overlay (see OverlayService), and edit
 * its font size, font, and color. All actual FPS drawing happens in
 * OverlayService so it stays visible while using other apps.
 */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button

    companion object {
        private const val PREFS_NAME = OverlayService.PREFS_NAME
        private const val KEY_SIZE = OverlayService.KEY_SIZE
        private const val KEY_FONT = OverlayService.KEY_FONT
        private const val KEY_COLOR = OverlayService.KEY_COLOR
        private const val DEFAULT_SIZE = OverlayService.DEFAULT_SIZE
        private val DEFAULT_COLOR = OverlayService.DEFAULT_COLOR
        private val FONTS = OverlayService.FONTS

        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_NOTIFICATION_PERMISSION = 1002

        private val FONT_NAMES = arrayOf("Default", "Monospace", "Serif", "Sans-serif")
        private val SWATCHES = intArrayOf(
            DEFAULT_COLOR,
            Color.WHITE,
            Color.RED,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.BLACK
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)

        toggleButton.setOnClickListener { onToggleClicked() }
        settingsButton.setOnClickListener { showSettingsDialog() }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        if (OverlayService.isRunning) {
            statusText.setText(R.string.status_on)
            toggleButton.setText(R.string.stop_overlay)
        } else {
            statusText.setText(R.string.status_off)
            toggleButton.setText(R.string.start_overlay)
        }
    }

    private fun onToggleClicked() {
        if (OverlayService.isRunning) {
            stopService(Intent(this, OverlayService::class.java))
            updateStatus()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            return
        }

        requestNotificationPermissionThenStart()
    }

    private fun requestNotificationPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATION_PERMISSION)
            return
        }
        startOverlayService()
    }

    private fun startOverlayService() {
        if (!Settings.canDrawOverlays(this)) return
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateStatus()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION && Settings.canDrawOverlays(this)) {
            requestNotificationPermissionThenStart()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            // Start regardless of the result: the notification is only a status
            // indicator, the overlay itself doesn't depend on it.
            startOverlayService()
        }
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val sizeSeek: SeekBar = dialogView.findViewById(R.id.sizeSeekBar)
        val sizeLabel: TextView = dialogView.findViewById(R.id.sizeLabel)
        val fontSpinner: Spinner = dialogView.findViewById(R.id.fontSpinner)
        val swatchContainer: LinearLayout = dialogView.findViewById(R.id.swatchContainer)

        val currentSize = prefs.getFloat(KEY_SIZE, DEFAULT_SIZE).roundToInt()
        val currentFont = prefs.getInt(KEY_FONT, 0)
        var selectedColor = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)

        // Size range: 8sp..72sp
        sizeSeek.max = 64
        sizeSeek.progress = (currentSize - 8).coerceIn(0, 64)
        sizeLabel.text = getString(R.string.font_size_label) + ": ${currentSize}sp"
        sizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sizeLabel.text = getString(R.string.font_size_label) + ": ${progress + 8}sp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        fontSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, FONT_NAMES)
        fontSpinner.setSelection(currentFont)

        val swatchViews = mutableListOf<View>()
        val swatchSizePx = (32 * resources.displayMetrics.density).toInt()
        val swatchMarginPx = (8 * resources.displayMetrics.density).toInt()
        for (c in SWATCHES) {
            val swatch = View(this)
            val params = LinearLayout.LayoutParams(swatchSizePx, swatchSizePx)
            params.setMargins(swatchMarginPx, swatchMarginPx, swatchMarginPx, swatchMarginPx)
            swatch.layoutParams = params
            swatch.setBackgroundColor(c)
            swatch.contentDescription = String.format("#%06X", 0xFFFFFF and c)
            swatch.setOnClickListener {
                selectedColor = c
                highlightSelected(swatchViews, c)
            }
            swatchContainer.addView(swatch)
            swatchViews.add(swatch)
        }
        highlightSelected(swatchViews, selectedColor)

        AlertDialog.Builder(this)
            .setTitle(R.string.settings_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                prefs.edit()
                    .putFloat(KEY_SIZE, (sizeSeek.progress + 8).toFloat())
                    .putInt(KEY_FONT, fontSpinner.selectedItemPosition)
                    .putInt(KEY_COLOR, selectedColor)
                    .apply()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun highlightSelected(swatchViews: List<View>, selectedColor: Int) {
        for (v in swatchViews) {
            val isSelected = v.contentDescription == String.format("#%06X", 0xFFFFFF and selectedColor)
            v.alpha = if (isSelected) 1.0f else 0.5f
        }
    }
}
