package com.example.smartdevice

import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale
import java.util.StringJoiner

class OverlayComponent(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT
    )

    private lateinit var overlayView: View
    private lateinit var nightUIBlack: View
    private lateinit var nightUIOrange: View
    lateinit var decibelTextView: TextView

    private var isAdded = false

    fun createOverlay() {
        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_layout, null)

        nightUIBlack = overlayView.findViewById(R.id.nightUIBlack)
        nightUIOrange = overlayView.findViewById(R.id.nightUIOrange)
        decibelTextView = overlayView.findViewById(R.id.decibelTextView)

        nightUIBlack.visibility = View.GONE
        nightUIOrange.visibility = View.GONE
        decibelTextView.visibility = View.GONE
    }

    fun showOverlay(type: String) {
        if (!isAdded) {
            windowManager.addView(overlayView, params)
            isAdded = true
        }

        when (type) {
            "OverlayService" -> nightUIBlack.visibility = View.VISIBLE
            "AudioMonitorService" -> decibelTextView.visibility = View.VISIBLE
        }
    }

    fun hideOverlay(type: String) {
        when (type) {
            "OverlayService" -> nightUIBlack.visibility = View.GONE
            "AudioMonitorService" -> decibelTextView.visibility = View.GONE
        }
    }

    fun destroyOverlay() {
        if (isAdded) {
            windowManager.removeView(overlayView)
            isAdded = false
        }
    }

    fun decibelText(decibel: Double): String {
        var integerDb = decibel.toInt()
        val joiner = StringJoiner("")

        if (decibel < 0) {
            integerDb = 0
        }
        val viewDb = String.format(Locale.getDefault(), "%3d db ", integerDb)

        var i = 0
        while (i < integerDb) {
            if (i >= 50) {
                joiner.add("X")
            } else {
                joiner.add("|")
            }
            i += 2
        }
        return "音量$viewDb$joiner"
    }

    fun syncAlpha(alpha: Float) {
        nightUIBlack.alpha = alpha
        nightUIOrange.alpha = alpha
    }

    fun switchNightUI(target: String) {
        nightUIBlack.visibility = View.GONE
        nightUIOrange.visibility = View.GONE

        when (target) {
            "normal" -> nightUIBlack.visibility = View.VISIBLE
            "orange" -> nightUIOrange.visibility = View.VISIBLE
        }
    }
}
