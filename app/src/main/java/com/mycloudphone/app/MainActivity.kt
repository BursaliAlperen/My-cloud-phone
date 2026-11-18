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
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.text.InputType
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class MainActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var address: EditText
    private lateinit var apiToken: EditText
    private lateinit var message: TextView
    private val prefs by lazy { getSharedPreferences("cloudphone", MODE_PRIVATE) }
    private val pickApkRequest = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        makeScreen()
        setupBrowser()
        prefs.getString("server_url", null)?.let {
            address.setText(it)
            connect(it)
        }
        apiToken.setText(prefs.getString("upload_token", ""))
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
            hint = "https://sunucu-adin.example/vnc.html"
            textSize = 14f
            singleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(35, 47, 68))
        }
        root.addView(address)
        apiToken = EditText(this).apply {
            hint = "APK aktarım API anahtarı"
            textSize = 14f
            singleLine = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(35, 47, 68))
        }
        root.addView(apiToken)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        row.addView(Button(this).apply {
            text = "Bağlan"
            setOnClickListener {
                val value = address.text.toString().trim()
                if (isSafeHttps(value)) {
                    prefs.edit().putString("server_url", value)
                        .putString("upload_token", apiToken.text.toString()).apply()
                    connect(value)
                    hideKeyboard()
                } else message.text = "Lütfen geçerli bir HTTPS sunucu adresi gir."
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply {
            text = "Yenile"
            setOnClickListener { browser.reload() }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)

        root.addView(Button(this).apply {
            text = "＋ Ana cihazdan APK içe aktar"
            setOnClickListener {
                val server = address.text.toString().trim()
                if (!isSafeHttps(server)) {
                    message.text = "Önce HTTPS bulut sunucu adresini kaydet."
                } else {
                    prefs.edit().putString("server_url", server)
                        .putString("upload_token", apiToken.text.toString()).apply()
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/vnd.android.package-archive"
                    }
                    startActivityForResult(intent, pickApkRequest)
                }
            }
        })
        message = TextView(this).apply {
            text = "Sunucu adresini gir. APK içe aktarma için sunucuda /api/apps/install uç noktası gerekir."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(3, 5, 3, 8)
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
                if (request?.isForMainFrame == true) message.text = "Bağlantı hatası. Sunucu ve HTTPS adresini kontrol et."
            }
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickApkRequest && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            uploadApk(uri)
        }
    }

    private fun uploadApk(uri: Uri) {
        val base = address.text.toString().trim()
        val token = apiToken.text.toString().trim()
        if (!isSafeHttps(base)) {
            message.text = "Geçerli HTTPS sunucu adresi gerekli."
            return
        }
        message.text = "APK sunucuya aktarılıyor…"
        Thread {
            var connection: HttpURLConnection? = null
            try {
                val parsed = Uri.parse(base)
                val portSuffix = if (parsed.port != -1) ":" + parsed.port else ""
                val endpoint = "https://" + parsed.host + portSuffix + "/api/apps/install"
                val boundary = "----MyCloudPhone-" + UUID.randomUUID().toString()
                connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 120000
                    doOutput = true
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
                    setRequestProperty("Accept", "application/json")
                    if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer " + token)
                    setChunkedStreamingMode(64 * 1024)
                }
                val output = connection.outputStream
                output.write(("--" + boundary + "\r\n").toByteArray())
                output.write("Content-Disposition: form-data; name=\"apk\"; filename=\"upload.apk\"\r\n".toByteArray())
                output.write("Content-Type: application/vnd.android.package-archive\r\n\r\n".toByteArray())
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Dosya okunamadı." }
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                }
                output.write(("\r\n--" + boundary + "--\r\n").toByteArray())
                output.flush()
                output.close()
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                runOnUiThread {
                    message.text = if (code in 200..299)
                        "APK sunucuya gönderildi. Sunucu yanıtı: " + body.take(180)
                    else "APK aktarımı başarısız (HTTP \u0024code). Sunucu API'sini kontrol et."
                }
            } catch (e: Exception) {
                runOnUiThread { message.text = "APK aktarılamadı: " + (e.message ?: "bağlantı hatası") }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun hideKeyboard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(address.windowToken, 0)
    }

    private fun isSafeHttps(value: String): Boolean = try {
        val uri = Uri.parse(value)
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
