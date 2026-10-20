package com.delassierras.radio

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var playButton: Button
    private lateinit var status: TextView
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ScrollView(this).apply { setBackgroundColor(Color.rgb(47, 0, 28)); fillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(26.dp, 54.dp, 26.dp, 30.dp) }
        root.addView(content)
        fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView = TextView(this).apply {
            text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
            if (bold) setTypeface(null, Typeface.BOLD)
        }
        val cyan = Color.rgb(49, 222, 241)
        val yellow = Color.rgb(255, 226, 42)
        val magenta = Color.rgb(240, 67, 196)
        content.addView(text("◢  ◣  ◢  ◣", 42f, cyan, true))
        content.addView(text("DE LAS", 38f, Color.WHITE, true))
        content.addView(text("SIERRAS", 45f, Color.WHITE, true))
        content.addView(text("PLAYING GOOD MUSIC", 17f, Color.WHITE, true))
        content.addView(text("80s  •  90s  •  2000s", 16f, yellow, true).apply { setPadding(0, 26.dp, 0, 12.dp) })
        content.addView(text("COMMERCIAL-FREE  •  BROADCASTING 24/7", 12f, yellow, true))
        content.addView(View(this).apply { setBackgroundColor(magenta) }, LinearLayout.LayoutParams(-1, 2.dp).apply { topMargin = 35.dp; bottomMargin = 32.dp })
        content.addView(text("LIVE RADIO", 15f, cyan, true))
        status = text("Ready to listen", 17f)
        content.addView(status.apply { setPadding(0, 12.dp, 0, 22.dp) })
        playButton = Button(this).apply {
            text = "▶  PLAY"
            textSize = 21f
            setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(171, 12, 123), Color.rgb(81, 38, 167))).apply { cornerRadius = 28.dp.toFloat() }
            setOnClickListener { controller?.let { c -> if (c.isPlaying) c.pause() else { c.play(); refresh() } } }
        }
        content.addView(playButton, LinearLayout.LayoutParams(-1, 65.dp))
        content.addView(text("Music continues when your screen is locked", 13f, Color.LTGRAY).apply { setPadding(0, 22.dp, 0, 0) })
        content.addView(text("DE LAS SIERRAS • PLAYING GOOD MUSIC", 12f, Color.LTGRAY).apply { setPadding(0, 90.dp, 0, 0) })
        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, android.content.ComponentName(this, RadioService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync().also { future ->
            future.addListener({
                try {
                    controller = future.get().also { it.addListener(listener) }
                    refresh()
                } catch (e: Exception) {
                    status.text = "Unable to connect to player"
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun refresh() {
        val c = controller ?: return
        playButton.text = if (c.isPlaying) "Ⅱ  PAUSE" else "▶  PLAY"
        status.text = when {
            c.isPlaying -> "ON AIR"
            c.playbackState == Player.STATE_BUFFERING -> "Connecting…"
            c.playerError != null -> "Connection interrupted — retrying"
            else -> "Ready to listen"
        }
    }

    override fun onStop() {
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
