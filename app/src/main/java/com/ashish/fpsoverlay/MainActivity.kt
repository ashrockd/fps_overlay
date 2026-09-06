package com.ashish.fpsoverlay

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import kotlin.math.roundToInt

/**
 * Bare-minimum FPS overlay. Draws the device's current composited frame
 * rate (measured via Choreographer vsync callbacks) as a draggable,
 * customizable label inside the app's own window.
 */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var fpsText: TextView

    private var frameCount = 0
    private var lastTimeNs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (lastTimeNs == 0L) lastTimeNs = frameTimeNanos
            frameCount++
            val elapsedNs = frameTimeNanos - lastTimeNs
            if (elapsedNs >= 1_000_000_000L) {
                val fps = (frameCount * 1_000_000_000.0 / elapsedNs).roundToInt()
                fpsText.text = "$fps FPS"
                frameCount = 0
                lastTimeNs = frameTimeNanos
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    companion object {
        private const val PREFS_NAME = "fps_overlay_prefs"
        private const val KEY_SIZE = "font_size"
        private const val KEY_FONT = "font_index"
        private const val KEY_COLOR = "text_color"
        private const val DEFAULT_SIZE = 18f
        private val DEFAULT_COLOR = Color.parseColor("#00FF00")

        private val FONTS = arrayOf(Typeface.DEFAULT, Typeface.MONOSPACE, Typeface.SERIF, Typeface.SANS_SERIF)
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
        fpsText = findViewById(R.id.fpsText)
        val settingsButton: ImageButton = findViewById(R.id.settingsButton)

        applySettings()
        setupDrag()

        settingsButton.setOnClickListener { showSettingsDialog() }
    }

    override fun onResume() {
        super.onResume()
        frameCount = 0
        lastTimeNs = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    private fun applySettings() {
        val size = prefs.getFloat(KEY_SIZE, DEFAULT_SIZE)
        val fontIndex = prefs.getInt(KEY_FONT, 0).coerceIn(0, FONTS.size - 1)
        val color = prefs.getInt(KEY_COLOR, DEFAULT_COLOR)
        fpsText.textSize = size
        fpsText.typeface = FONTS[fontIndex]
        fpsText.setTextColor(color)
    }

    /** Lets the user drag the FPS label anywhere within the app's own window. */
    private fun setupDrag() {
        var dX = 0f
        var dY = 0f
        fpsText.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.x = event.rawX + dX
                    view.y = event.rawY + dY
                    true
                }
                else -> false
            }
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
                applySettings()
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
