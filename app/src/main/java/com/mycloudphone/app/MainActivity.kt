package com.mycloudphone.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.WindowManager
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.text.InputType

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var message: TextView
    private val prefs by lazy { getSharedPreferences("cloudphone", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        makeScreen()
        setupBrowser()
        prefs.getString("server_url", null)?.let {
            address.setText(it)
            connect(it)
        }
    }

    private fun makeScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 8)
            setBackgroundColor(Color.rgb(13, 20, 34))
        }
        root.addView(TextView(this).apply {
            text = "MyCloudPhone"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(4, 4, 4, 12)
        })
        address = EditText(this).apply {
            hint = "https://alan-adin.example/vnc.html"
            textSize = 14f
            singleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(35, 47, 68))
        }
        root.addView(address)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(Button(this).apply {
            text = "Bağlan"
            setOnClickListener {
                val value = address.text.toString().trim()
                if (isSafeHttps(value)) {
                    prefs.edit().putString("server_url", value).apply()
                    connect(value)
                    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(address.windowToken, 0)
                } else message.text = "Lütfen geçerli bir HTTPS sunucu adresi gir."
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "Yenile"
            setOnClickListener { browser.reload() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        message = TextView(this).apply {
            text = "Bulut telefon sunucunun HTTPS adresini gir."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(3, 2, 3, 8)
        }
        root.addView(message)
        browser = WebView(this)
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun setupBrowser() {
        browser.settings.javaScriptEnabled = true
        browser.settings.domStorageEnabled = true
        browser.settings.loadsImagesAutomatically = true
        browser.settings.mediaPlaybackRequiresUserGesture = false
        browser.settings.allowFileAccess = false
        browser.settings.allowContentAccess = false
        browser.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        browser.webChromeClient = WebChromeClient()
        browser.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return request?.url?.scheme != "https"
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                message.text = "Uzak ekran sayfası yüklendi."
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    message.text = "Bağlantı hatası. Sunucu ve HTTPS adresini kontrol et."
                }
            }
        }
    }

    private fun isSafeHttps(value: String): Boolean = try {
        val uri = android.net.Uri.parse(value)
        uri.scheme == "https" && !uri.host.isNullOrBlank()
    } catch (_: Exception) { false }

    private fun connect(value: String) {
        if (!isSafeHttps(value)) {
            message.text = "Yalnızca HTTPS adresleri desteklenir."
            return
        }
        message.text = "Bulut telefonuna bağlanılıyor…"
        browser.loadUrl(value)
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (this::browser.isInitialized && browser.canGoBack()) browser.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        if (this::browser.isInitialized) browser.destroy()
        super.onDestroy()
    }
}
