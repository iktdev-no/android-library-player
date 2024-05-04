package no.iktdev.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi
import com.google.android.exoplayer2.ui.StyledPlayerView

@RequiresApi(api = Build.VERSION_CODES.O)
class PictureInPicture(val activity: PlayerActivity) {
    enum class IntentFilter(val value: String) {
        FILTER_PIP_PLAYBACK("PIP_PLAYBACK_STATE")
        ;
        companion object {
            fun getEnum(value: String): IntentFilter {
                for (ifs in IntentFilter.values()) {
                    if (ifs.value == value) return ifs
                }
                throw IllegalArgumentException("Enum with '$value' does not exist")
            }
        }
    }


    // This was due to a mal-implementation in Android 11, which caused the PIP to become vertical
    var invertPlayerAspect: Boolean = false
    fun getPictureInPictureParams(): PictureInPictureParams {
        val playerView = this.playerView ?: throw RuntimeException("PlayerView is not set!")

        val builder = PictureInPictureParams.Builder()
        val rational = if (invertPlayerAspect) Rational(playerView.height, playerView.width) else Rational(playerView.width, playerView.height)

        val remoteActionList: MutableList<RemoteAction> = ArrayList()
        remoteActionList.add(getPlayPauseRemoteAction(playerView.player?.isPlaying ?: false))
        builder.setActions(remoteActionList)
        builder.setAspectRatio(rational)
        val built = builder.build()
        return built
    }

    private fun PendingIntentFlag(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags or PendingIntent.FLAG_IMMUTABLE else flags
    }

    private fun getPlayPauseRemoteAction(isPlaying: Boolean): RemoteAction {
        val playPause = Intent(IntentFilter.FILTER_PIP_PLAYBACK.value)
        playPause.action = IntentFilter.FILTER_PIP_PLAYBACK.value
        val playPause_pendingIntent = PendingIntent.getBroadcast(
            activity.applicationContext, 0, playPause, PendingIntentFlag(
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        return RemoteAction(
            Icon.createWithResource(
                activity,
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            ),
            "Playback",
            "Playback buttons",
            playPause_pendingIntent
        )
    }

    interface PipListener {
        fun onPlaybackToggled()
    }

    private var playerView: StyledPlayerView? = null
    private var registeredReceiver: Boolean = false
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerReceiver(playerView: StyledPlayerView) {
        this.playerView = playerView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(pipReceiver, IntentFilter(IntentFilter.FILTER_PIP_PLAYBACK.value), Context.RECEIVER_NOT_EXPORTED)
        } else {
            activity.registerReceiver(pipReceiver, IntentFilter(IntentFilter.FILTER_PIP_PLAYBACK.value))
        }
        registeredReceiver = true
    }

    fun unregisterReceiver() {
        if (registeredReceiver) {
            activity.unregisterReceiver(pipReceiver)
        }
    }

    val pipReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val exoPlayer = playerView?.player ?: return
            if (exoPlayer.isPlaying)
                exoPlayer.pause()
            else
                exoPlayer.play()
            activity.setPictureInPictureParams(getPictureInPictureParams())
        }

    }


}