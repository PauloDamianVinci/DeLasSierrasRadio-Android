package com.delassierras.radio

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var retries = 0
    private val retry = Runnable {
        if (player.playWhenReady) {
            player.prepare()
            player.play()
        }
    }

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
            setMediaItem(MediaItem.Builder()
                .setUri(STREAM_URL)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle("DE LAS SIERRAS — LIVE")
                    .setArtist("Playing Good Music • 80s • 90s • 2000s")
                    .setIsPlayable(true)
                    .build())
                .build())
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    // Retry only while playback is desired; exponential delay capped at 30 seconds.
                    if (playWhenReady) {
                        val delay = (2000L * (1L shl retries.coerceAtMost(4))).coerceAtMost(30000L)
                        retries++
                        handler.removeCallbacks(retry)
                        handler.postDelayed(retry, delay)
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        retries = 0
                        handler.removeCallbacks(retry)
                    }
                }
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    if (!playWhenReady) {
                        // Radio is live: discard buffered audio instead of retaining a paused position.
                        handler.removeCallbacks(retry)
                        retries = 0
                        stop()
                    } else if (playbackState == Player.STATE_IDLE) {
                        // Start a fresh connection to the live stream after a stop.
                        prepare()
                    }
                }
            })
            prepare()
        }
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, player).setSessionActivity(pendingIntent).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }

    companion object {
        const val STREAM_URL = "https://server.streamcasthd.com/8682/stream"
    }
}
