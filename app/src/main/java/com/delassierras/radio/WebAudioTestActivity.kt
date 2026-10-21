package com.delassierras.radio

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Foreground-only audio experiment. This activity is deliberately separate from
 * the Media3 foreground service. Never use it as the production background player.
 */
class WebAudioTestActivity : Activity() {
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val spanish = intent.getStringExtra("language") == "es"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(22.dp, 28.dp, 22.dp, 20.dp)
            setBackgroundColor(Color.rgb(47, 0, 28))
        }
        fun info(text: String, size: Float): TextView = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 18)
        }
        root.addView(info(if (spanish) "PRUEBA DE AUDIO WEB" else "WEB AUDIO TEST", 22f))
        root.addView(info(
            if (spanish)
                "Comparación experimental con Media3. Usa los controles de audio de abajo. " +
                "Esta prueba funciona solo con la pantalla abierta; no tiene reproducción en segundo plano."
            else
                "Experimental comparison with Media3. Use the audio controls below. " +
                "This test works only while the screen is open; background playback is not supported.",
            15f
        ))
        webView = WebView(this).apply {
            setBackgroundColor(Color.WHITE)
            settings.javaScriptEnabled = false
            webViewClient = WebViewClient()
            loadDataWithBaseURL(
                "https://server.streamcasthd.com/",
                """<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="font-family:sans-serif;padding:12px">
                <audio controls preload="none" style="width:100%">
                <source src="https://server.streamcasthd.com/8682/stream" type="audio/aac">
                </audio></body></html>""",
                "text/html", "UTF-8", null
            )
        }
        root.addView(webView, LinearLayout.LayoutParams(-1, 110))
        root.addView(info(
            if (spanish) "Al volver, el reproductor Media3 seguirá disponible."
            else "Media3 remains available when you return.", 13f
        ))
        root.addView(Button(this).apply {
            text = if (spanish) "VOLVER A LA RADIO" else "BACK TO RADIO"
            setOnClickListener { finish() }
        })
        setContentView(root)
    }

    override fun onStop() {
        // Stop test playback when leaving the foreground, including screen lock.
        webView?.loadUrl("about:blank")
        super.onStop()
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
