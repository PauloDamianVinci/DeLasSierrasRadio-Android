package com.delassierras.radio

import android.content.Intent
import java.util.Locale
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
    private var language = "en"
    private fun tr(en: String, es: String): String = if (language == "es") es else en
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
        // Follow the phone language by default; a manual ES/EN choice takes precedence.
        val deviceLanguage = if (Locale.getDefault().language.equals("es", ignoreCase = true)) "es" else "en"
        language = getSharedPreferences("radio_settings", MODE_PRIVATE)
            .getString("language", null) ?: deviceLanguage
        val root = ScrollView(this).apply {
            setBackgroundColor(backgroundColor)
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(content)

        val languageRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setPadding(12.dp, 8.dp, 16.dp, 0)
        }
        fun languageButton(code: String, caption: String): Button = Button(this).apply {
            text = caption
            textSize = 13f
            isAllCaps = false
            setTextColor(if (language == code) cyan else Color.WHITE)
            background = GradientDrawable().apply {
                setColor(if (language == code) Color.rgb(90, 15, 73) else backgroundColor)
                cornerRadius = 16.dp.toFloat()
                setStroke(1.dp, if (language == code) cyan else pink)
            }
            setOnClickListener {
                if (language != code) {
                    getSharedPreferences("radio_settings", MODE_PRIVATE)
                        .edit().putString("language", code).apply()
                    recreate()
                }
            }
        }
        languageRow.addView(languageButton("es", "ES"), LinearLayout.LayoutParams(62.dp, 38.dp))
        languageRow.addView(languageButton("en", "EN"), LinearLayout.LayoutParams(62.dp, 38.dp).apply {
            marginStart = 8.dp
        })
        content.addView(languageRow)

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
        heroColumn.addView(label(tr("COMMERCIAL-FREE  •  BROADCASTING 24/7", "SIN PUBLICIDAD  •  TRANSMITIENDO 24/7"), 12f, Color.WHITE, true))
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
        playerArea.addView(label(tr("NOW PLAYING", "SUENA AHORA"), 13f, cyan, true).apply {
            setPadding(0, 6.dp, 0, 5.dp)
        })
        nowPlaying = label(tr("Loading song information…", "Cargando información musical…"), 17f, Color.WHITE, true)
        playerArea.addView(nowPlaying)
        status = label("", 15f, pink, true)
        playerArea.addView(status.apply { setPadding(0, 8.dp, 0, 12.dp) })
        playButton = Button(this).apply {
            text = tr("▶  PLAY", "▶  REPRODUCIR")
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
        val shareButton = Button(this).apply {
            text = tr("↗  SHARE RADIO", "↗  COMPARTIR RADIO")
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
        playerArea.addView(shareButton, LinearLayout.LayoutParams(-1, 52.dp).apply {
            topMargin = 16.dp
        })
        playerArea.addView(label("DE LAS SIERRAS  •  PLAYING GOOD MUSIC", 11f, Color.LTGRAY).apply {
            setPadding(0, 18.dp, 0, 0)
        })
        playerArea.addView(label(tr("RECENTLY PLAYED", "ÚLTIMAS CANCIONES"), 15f, cyan, true).apply {
            setPadding(0, 25.dp, 0, 12.dp)
        })
        recentTracks = label(tr("Loading recent tracks…", "Cargando canciones anteriores…"), 14f, Color.WHITE).apply {
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
                    status.text = tr("Unable to connect to player", "No se pudo conectar con el reproductor")
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun refresh() {
        val c = controller ?: return
        playButton.text = if (c.isPlaying) tr("Ⅱ  PAUSE", "Ⅱ  PAUSAR") else tr("▶  PLAY", "▶  REPRODUCIR")
        // No idle, on-air, or radio-ready labels. Show only connection problems/progress.
        status.text = when {
            c.playerError != null && c.playWhenReady ->
                tr("Connection interrupted — retrying", "Conexión interrumpida — reintentando")
            c.playbackState == Player.STATE_BUFFERING || (c.playWhenReady && !c.isPlaying) ->
                tr("Connecting…", "Conectando…")
            else -> ""
        }
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
                    recentTracks.text = tracks.joinToString("\n").ifBlank { tr("No recent tracks available", "No hay canciones anteriores disponibles") }
                }
            } catch (_: Exception) {
                handler.post {
                    if (metadataActive && !isFinishing && !isDestroyed) {
                        if (nowPlaying.text == tr("Loading song information…", "Cargando información musical…")) nowPlaying.text = tr("Song information unavailable", "Información musical no disponible")
                        if (recentTracks.text == tr("Loading recent tracks…", "Cargando canciones anteriores…")) recentTracks.text = tr("History temporarily unavailable", "Historial temporalmente no disponible")
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
                tr("Listen to DE LAS SIERRAS RADIO — Playing Good Music!\nhttps://www.delassierrasradio.com", "Escucha DE LAS SIERRAS RADIO — ¡La mejor música!\nhttps://www.delassierrasradio.com")
            )
        }
        startActivity(Intent.createChooser(intent, tr("Share DE LAS SIERRAS RADIO", "Compartir DE LAS SIERRAS RADIO")))
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
