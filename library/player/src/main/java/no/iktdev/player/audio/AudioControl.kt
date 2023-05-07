package no.iktdev.player.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioControl(private val applicationContext: Context) {
    private val audioManager: AudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var playbackNowAuthorized = false
    private var playbackDelayed = false
    private var resumeOnFocusGain = false

    val focusLock = Any()

    private val audioAttr: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

    /**
     * Changing listener will notify focus lost on current listener
     */
    var listener: AudioState? = null
        set(value) {
            field?.OnAudioFocusLost()
            field = value
            if (value != null) {
                requestFocus()
            }
        }

    private var focusRequest: AudioFocusRequest? = null
    fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttr)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            this.focusRequest = focusRequest
            val result = audioManager.requestAudioFocus(focusRequest)
            synchronized(focusLock) {
                when (result) {
                    AudioManager.AUDIOFOCUS_REQUEST_FAILED -> playbackNowAuthorized =
                        false
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                        playbackNowAuthorized = true
                        listener?.OnAudioFocusGained()
                    }
                    AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                        playbackNowAuthorized = false
                        playbackDelayed = true
                        listener?.OnAudioFocusDelayed()
                    }
                    else -> { /* Do nothing */}
                }
            }
        } else {
            val result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                AudioManager.AUDIOFOCUS_GAIN
            )
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) listener?.OnAudioFocusGained()
        }

    }

    fun abandonAudioFocus() {
        audioManager.abandonAudioFocus(focusChangeListener)
        if (focusRequest != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            audioManager.abandonAudioFocusRequest(focusRequest!!)
    }

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange: Int ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> if (playbackDelayed || resumeOnFocusGain) {
                synchronized(focusLock) {
                    playbackDelayed = false
                    resumeOnFocusGain = false
                }
                playbackNowAuthorized = true
                listener?.OnAudioFocusGained()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                synchronized(focusLock) {
                    resumeOnFocusGain = false
                    playbackDelayed = false
                }
                listener?.OnAudioFocusLost()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                // ... pausing or ducking depends on your app
                synchronized(focusLock) {
                    resumeOnFocusGain = true
                    playbackDelayed = false
                }
                listener?.OnAudioFocusLost()
            }
        }
    }

    interface AudioState {
        fun OnAudioFocusGained()
        fun OnAudioFocusLost()
        fun OnAudioFocusDelayed()
    }

}