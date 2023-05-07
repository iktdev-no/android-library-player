package no.iktdev.player.helper

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import no.iktdev.networking.Security
import no.iktdev.networking.client.Http
import no.iktdev.player.SourceMode
import no.iktdev.player.classes.Subtitle

class MediaSourceUtil(val context: Context, private val transferListener: TransferListener? = null, val mode: SourceMode) {
    val agent: String = Util.getUserAgent(context, "StreamIT")
    private var dataSourceFactory: DataSource.Factory = if (mode == SourceMode.STORAGE) getLocalDataSource() else getWebDataSource()

    private fun getWebDataSource(): HttpDataSource.Factory {
        val httpSource: HttpDataSource.Factory = DefaultHttpDataSource.Factory().apply { setUserAgent(agent); setTransferListener(transferListener) }
        if (mode == SourceMode.CONNECTION_AUTHENTICATED) {
            httpSource.setDefaultRequestProperties(mapOf(
                Pair("Authorization", "Bearer ${Security.authorizationBearerToken}")
            ))
        }
        return httpSource
    }

    private fun getLocalDataSource(): DataSource.Factory {
        return DefaultDataSource.Factory(context)
    }

    fun createMediaSource(uri: Uri): MediaSource {
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
    }

    fun createSubtitleMediaSource(subtitles: List<Subtitle>): MediaSource? {
        var mediaSource: MediaSource? = null
        for (sub: Subtitle in subtitles) {
            val subMediaItem = MediaItem.SubtitleConfiguration.Builder(Uri.parse(sub.subtitlePath))
                .setId(subtitles.indexOf(sub).toString())
                .setMimeType(mediaSourceMimetype(sub.format))
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setLanguage(sub.language)
                .build()
            val subMediaSource = SingleSampleMediaSource.Factory(dataSourceFactory).createMediaSource(subMediaItem, C.TIME_END_OF_SOURCE) // TIME_UNSET
            mediaSource = if (mediaSource == null) subMediaSource else MergingMediaSource(mediaSource, subMediaSource)
        }
        return mediaSource
    }

    private fun mediaSourceMimetype(format: String): String {
        return when (format) {
            "ASS", "SSA" -> MimeTypes.TEXT_SSA
            "SRT" -> MimeTypes.APPLICATION_SUBRIP
            "VTT" -> MimeTypes.TEXT_VTT
            else -> MimeTypes.TEXT_UNKNOWN
        }
    }





}