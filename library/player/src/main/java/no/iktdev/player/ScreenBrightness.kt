package no.iktdev.player

import android.view.Window
import android.widget.SeekBar

class ScreenBrightness(var window: Window) {

    fun setBrightness(value: Float) {
        val params = window.attributes ?: return
        params.screenBrightness = value
        window.attributes = params
    }

    fun abandonBrightnessControl() {
        val params = window.attributes ?: return
        params.screenBrightness = -1f
        window.attributes = params
    }

    val lightBar = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
            val level = progress.toFloat() / 100
            setBrightness(level)
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {
        }

        override fun onStopTrackingTouch(p0: SeekBar?) {
        }

    }
}