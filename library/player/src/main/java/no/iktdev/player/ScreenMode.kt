package no.iktdev.player

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsCompat
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView

class ScreenMode(val activity: Activity, val playerView: StyledPlayerView, val aspectButton: ImageButton? = null, val notchButton: ImageButton? = null, val listener: Listener) {
    enum class Aspect {
        FIT, FILL
    }
    enum class Notch {
        AVOID, USE
    }

    var aspectMode: Aspect = Aspect.FIT
        set(value) {
            field = value
            changeAspectMode()
        }

    var notchMode: Notch = Notch.AVOID
        set(value) {
            field = value
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                changeNotchMode()
            } else {
                Log.e(this::class.java.simpleName, "Attempted to use notch on non supported device!")
            }
        }

    private fun changeAspectMode() {
        listener.onAspectModeChanged(aspectMode)
        if (aspectMode == Aspect.FILL) {
            playerView.subtitleView?.setBottomPaddingFraction(0.125f)
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        } else {
            playerView.subtitleView?.setBottomPaddingFraction(0.05f)
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        val icon = if (aspectMode == Aspect.FILL) R.drawable.ic_aspect_fit_black_24dp else R.drawable.ic_aspect_fill_black_24dp
        aspectButton?.setImageResource(icon)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun changeNotchMode() {
        listener.onNotchModeChanged(notchMode)
        val window = activity.window
        if (notchMode == Notch.USE) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        } else {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        }
        val icon = if (notchMode == Notch.USE) R.drawable.ic_notch_avoid else R.drawable.ic_notch_use
        notchButton?.setImageResource(icon)
    }


    interface Listener {
        fun onAspectModeChanged(aspect: Aspect)
        fun onNotchModeChanged(notch: Notch)
    }
}