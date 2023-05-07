package no.iktdev.player

import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.text.CueGroup
import com.google.android.exoplayer2.ui.StyledPlayerView
import no.iktdev.player.audio.AudioControl
import java.util.logging.Logger

abstract class PlayerActivity: AppCompatActivity(), Player.Listener, AudioControl.AudioState {
    open var playerView: StyledPlayerView? = null
        set(value) {
            field = value
            field?.setControllerVisibilityListener(playerControlVisibilityListener)
        }
    open var exoPlayer: ExoPlayer? = null
        set(value) {
            if (value == null)
                field?.removeListener(this)
            else
                value.addListener(this)
            field = value

        }

    protected var allowPip: Boolean = false
    protected var isCurrentlyPip: Boolean = false
        set(value) {
            field = value
            val pv = playerView ?: return
            pv.controllerAutoShow = !value
            pv.useController = !value
            pv.subtitleView?.visibility = if (field) View.GONE else View.VISIBLE
            if (value)
                pv.hideController()
            else
                pv.showController()
        }
    protected var pip: PictureInPicture? = null
    protected var isBackstackLost = false
        private set

    protected lateinit var audioControl: AudioControl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audioControl = AudioControl(this.applicationContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pip = PictureInPicture(this)
        }
        /*if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                onCloseActivity()
            }
        } else {
        }*/
    }

    protected open fun forward(seconds: Long = 30) {
        val requestedJump = seconds * 1000
        val player_current = exoPlayer?.currentPosition ?: return
        val player_duration = exoPlayer?.duration ?: return

        val targetTime = if (player_current + requestedJump <= player_duration)
            player_current + requestedJump
        else
            player_duration - 1000
        exoPlayer?.seekTo(targetTime)
    }

    protected open fun rewind(seconds: Long = 10) {
        val requestedJump = seconds * 1000
        val player_current = exoPlayer?.currentPosition ?: return
        val targetTime = if (player_current > requestedJump) player_current - requestedJump else 0
        exoPlayer?.seekTo(targetTime)
    }

    override fun onStart() {
        super.onStart()
        audioControl.listener = this
    }

    override fun onResume() {
        super.onResume()
        if (playerView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                pip?.registerReceiver(playerView!!)

    }

    override fun onPause() {
        super.onPause()
        if (!isCurrentlyPip) {
            resumePlaying = false
        }
    }

    override fun onStop() {
        super.onStop()
        audioControl.abandonAudioFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (pip != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pip?.unregisterReceiver()
        }
    }

    val playerControlVisibilityListener = StyledPlayerView.ControllerVisibilityListener { visibility ->
            if (visibility != View.GONE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.setDecorFitsSystemWindows(false)
                    val windowInsetsController = window.insetsController ?: return@ControllerVisibilityListener
                    windowInsetsController.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsets.Type.systemBars())

                } else {
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                }
            }
        }


    protected fun keepScreenOn(isPlaying: Boolean = false) {
        if (isPlaying)
            this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else
            this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && allowPip) {
            val pipParam = pip?.getPictureInPictureParams() ?: return
            enterPictureInPictureMode(pipParam)
        }
    }

    protected open fun forwardOldActivity() {
        val activityManager = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        // iterate app tasks available and navigate to launcher task (browse task)
        val appTasks = activityManager.appTasks
        for (task in appTasks) {
            val baseIntent = task.taskInfo.baseIntent
            val categories = baseIntent.categories
            if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                task.moveToFront()
                return
            }
        }
    }

    protected open fun onCloseActivity() {
        if (isBackstackLost)
            forwardOldActivity()
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onCloseActivity()
    }

    override fun enterPictureInPictureMode(params: PictureInPictureParams): Boolean {
        isBackstackLost = true
        return super.enterPictureInPictureMode(params)
    }


    override fun onCues(cueGroup: CueGroup) {
        super.onCues(cueGroup)
        playerView?.subtitleView?.setCues(cueGroup.cues)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val previousState = exoPlayer?.isPlaying ?: false
        super.onIsPlayingChanged(isPlaying)
        keepScreenOn(isPlaying)
        if (isPlaying) {
            audioControl.requestFocus()
            resumePlaying = true
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Toast.makeText(this@PlayerActivity.baseContext, error.message, Toast.LENGTH_LONG).show()
    }

    protected var resumePlaying: Boolean = false
    override fun OnAudioFocusGained() {
        if (resumePlaying) {
            Log.i(this.localClassName, "OnAudioFocusGained -> Calling exoplayer.play()")
            exoPlayer?.play()
        }
    }

    override fun OnAudioFocusLost() {
        exoPlayer?.pause()
    }

    override fun OnAudioFocusDelayed() {
        exoPlayer?.pause()
    }
}