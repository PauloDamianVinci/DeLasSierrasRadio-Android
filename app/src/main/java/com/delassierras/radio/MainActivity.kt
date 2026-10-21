package com.delassierras.radio

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
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
    private lateinit var nowPlaying: TextView
    private lateinit var recentTracks: TextView
    private lateinit var artwork: ImageView
    private lateinit var connectionIndicator: TextView
    private var lastArtworkUrl: String? = null
    private val apiUrl = "https://server.streamcasthd.com/cp/get_info.php?p=8682"
    private val handler = Handler(Looper.getMainLooper())
    private val network = Executors.newSingleThreadExecutor()
    private var metadataActive = false
    private val metadataPoll = object : Runnable {
        override fun run() {
            if (!metadataActive) return
            loadMetadata()
            handler.postDelayed(this, 15_000L)
        }
    }
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
        connectionIndicator = label("●  RADIO EN VIVO", 15f, pink, true)
        playerArea.addView(connectionIndicator)
        playerArea.addView(label("NOW PLAYING", 13f, cyan, true).apply {
            setPadding(0, 18.dp, 0, 5.dp)
        })
        nowPlaying = label("Loading song information…", 17f, Color.WHITE, true)
        playerArea.addView(nowPlaying)
        artwork = assetImage("music.png").apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0, 10.dp, 0, 10.dp)
        }
        playerArea.addView(artwork, LinearLayout.LayoutParams(155.dp, 155.dp))
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
            setPadding(0, 20.dp, 0, 0)
        })
        playerArea.addView(label("Music continues with your screen locked", 12f, Color.LTGRAY).apply {
            setPadding(0, 8.dp, 0, 14.dp)
        })
        val shareButton = Button(this).apply {
            text = "↗  SHARE RADIO"
            textSize = 15f
            isAllCaps = false
            setTextColor(cyan)
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                setStroke(2.dp, cyan)
                cornerRadius = 25.dp.toFloat()
            }
            setOnClickListener { shareRadio() }
        }
        playerArea.addView(shareButton, LinearLayout.LayoutParams(-1, 52.dp))
        playerArea.addView(label("DE LAS SIERRAS  •  PLAYING GOOD MUSIC", 11f, Color.LTGRAY).apply {
            setPadding(0, 18.dp, 0, 0)
        })
        playerArea.addView(label("RECENTLY PLAYED", 15f, cyan, true).apply {
            setPadding(0, 25.dp, 0, 12.dp)
        })
        recentTracks = label("Loading recent tracks…", 14f, Color.WHITE).apply {
            gravity = Gravity.START
            setLineSpacing(5.dp.toFloat(), 1f)
        }
        playerArea.addView(recentTracks, LinearLayout.LayoutParams(-1, -2))
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
        metadataActive = true
        handler.post(metadataPoll)
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
        val message = when {
            c.isPlaying -> "ON AIR"
            c.playerError != null && c.playWhenReady -> "Connection interrupted — retrying"
            c.playbackState == Player.STATE_BUFFERING -> "Connecting…"
            c.playWhenReady -> "Connecting…"
            else -> "Ready to listen"
        }
        status.text = message
        connectionIndicator.text = when (message) {
            "ON AIR" -> "●  LIVE RADIO"
            "Connecting…" -> "◌  CONNECTING"
            "Connection interrupted — retrying" -> "◌  RECONNECTING"
            else -> "○  RADIO READY"
        }
        connectionIndicator.setTextColor(if (c.isPlaying) cyan else pink)
    }

    private fun loadMetadata() {
        network.execute {
            try {
                val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                }
                val body = try {
                    conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } finally {
                    conn.disconnect()
                }
                val data = JSONObject(body)
                val title = cleanText(data.optString("title")).ifBlank { "DE LAS SIERRAS RADIO" }
                val history = data.optJSONArray("history")
                val tracks = (0 until minOf(history?.length() ?: 0, 10)).mapNotNull { index ->
                    history?.optString(index)?.let { cleanText(it) }?.takeIf { it.isNotBlank() }
                }
                handler.post {
                    if (!metadataActive || isFinishing || isDestroyed) return@post
                    nowPlaying.text = title
                    recentTracks.text = tracks.joinToString("\n").ifBlank { "No recent tracks available" }
                }
                // Album art is optional. It is fetched separately so metadata remains responsive.
                val artUrl = data.optString("art")
                if (artUrl != lastArtworkUrl) {
                    lastArtworkUrl = artUrl
                    handler.post {
                        if (metadataActive && !isFinishing && !isDestroyed) {
                            artwork.setImageDrawable(null)
                            assets.open("music.png").use { artwork.setImageBitmap(BitmapFactory.decodeStream(it)) }
                        }
                    }
                }
                if (artUrl.startsWith("https://")) {
                    try {
                        val imageConn = (URL(artUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 6000
                            readTimeout = 6000
                        }
                        val bitmap = try {
                            imageConn.inputStream.use { BitmapFactory.decodeStream(it) }
                        } finally { imageConn.disconnect() }
                        if (bitmap != null) handler.post {
                            if (metadataActive && !isFinishing && !isDestroyed) artwork.setImageBitmap(bitmap)
                        }
                    } catch (_: Exception) { /* Keep default artwork. */ }
                }
            } catch (_: Exception) {
                handler.post {
                    if (metadataActive && !isFinishing && !isDestroyed) {
                        if (nowPlaying.text == "Loading song information…") nowPlaying.text = "Song information unavailable"
                        if (recentTracks.text == "Loading recent tracks…") recentTracks.text = "History temporarily unavailable"
                    }
                }
            }
        }
    }

    private fun shareRadio() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Listen to DE LAS SIERRAS RADIO — Playing Good Music!\\nhttps://www.delassierrasradio.com"
            )
        }
        startActivity(Intent.createChooser(intent, "Share DE LAS SIERRAS RADIO"))
    }

    private fun cleanText(value: String): String = Html.fromHtml(
        value.replace(Regex("^\\s*\\d+\\.\\)\\s*"), ""), Html.FROM_HTML_MODE_LEGACY
    ).toString().trim()

    override fun onStop() {
        metadataActive = false
        handler.removeCallbacks(metadataPoll)
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
}
