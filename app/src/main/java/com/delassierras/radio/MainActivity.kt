package com.delassierras.radio

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var playButton: Button
    private lateinit var status: TextView
    private val backgroundColor = Color.rgb(47, 0, 28)
    private val cyan = Color.rgb(60, 235, 245)
    private val pink = Color.rgb(255, 85, 188)
    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = refresh()
        override fun onPlaybackStateChanged(playbackState: Int) = refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(content)

        val hero = FrameLayout(this)
        val heroImage = assetImage("mountains.jpg").apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        hero.addView(heroImage, FrameLayout.LayoutParams(-1, -1))
        val overlay = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0x220D0024, 0x992F001C.toInt(), backgroundColor))
        }
        hero.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        val heroColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20.dp, 18.dp, 20.dp, 10.dp)
        }
        hero.addView(heroColumn, FrameLayout.LayoutParams(-1, -1))
        val logo = assetImage("logo.png").apply { scaleType = ImageView.ScaleType.FIT_CENTER }
        heroColumn.addView(logo, LinearLayout.LayoutParams(220.dp, 220.dp))
        heroColumn.addView(label("COMMERCIAL-FREE  •  BROADCASTING 24/7", 12f, Color.WHITE, true))
        heroColumn.addView(label("80s  •  90s  •  2000s", 15f, cyan, true).apply {
            setPadding(0, 10.dp, 0, 0)
        })
        content.addView(hero, LinearLayout.LayoutParams(-1, 305.dp))

        val playerArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24.dp, 12.dp, 24.dp, 28.dp)
        }
        content.addView(playerArea, LinearLayout.LayoutParams(-1, -2))
        playerArea.addView(label("●  LIVE RADIO", 16f, pink, true))
        status = label("Ready to listen", 17f, Color.WHITE, true)
        playerArea.addView(status.apply { setPadding(0, 14.dp, 0, 20.dp) })
        playButton = Button(this).apply {
            text = "▶  PLAY"
            textSize = 21f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(215, 23, 145), Color.rgb(93, 42, 183))).apply {
                cornerRadius = 30.dp.toFloat()
            }
            setOnClickListener {
                controller?.let { c -> if (c.isPlaying) c.pause() else c.play() }
            }
        }
        playerArea.addView(playButton, LinearLayout.LayoutParams(-1, 65.dp))
        playerArea.addView(label("PLAYING GOOD MUSIC", 14f, cyan, true).apply {
            setPadding(0, 22.dp, 0, 0)
        })
        playerArea.addView(label("Music continues with your screen locked", 12f, Color.LTGRAY).apply {
            setPadding(0, 8.dp, 0, 20.dp)
        })
        val music = assetImage("music.png").apply { scaleType = ImageView.ScaleType.FIT_CENTER; alpha = 0.85f }
        playerArea.addView(music, LinearLayout.LayoutParams(95.dp, 95.dp))
        playerArea.addView(label("DE LAS SIERRAS  •  PLAYING GOOD MUSIC", 11f, Color.LTGRAY).apply {
            setPadding(0, 16.dp, 0, 0)
        })
        setContentView(root)
    }

    private fun assetImage(name: String): ImageView = ImageView(this).apply {
        assets.open(name).use { input -> setImageBitmap(BitmapFactory.decodeStream(input)) }
    }

    private fun label(value: String, size: Float, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(null, Typeface.BOLD)
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
