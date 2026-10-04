package com.unipack.creator

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.*
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        } else {
            fileCallback?.onReceiveValue(null)
        }
        fileCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
            allowContentAccess = true
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(wv: WebView?, cb: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = cb
                try { fileChooserLauncher.launch(params?.createIntent()) } 
                catch (e: Exception) { fileCallback = null; return false }
                return true
            }
        }
        
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }
}

class WebAppInterface(private val context: Context) {
    @JavascriptInterface
    fun saveZip(base64: String, filename: String) {
        try {
            val bytes = Base64.decode(base64.substringAfter(","), Base64.DEFAULT)
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                (context as Activity).runOnUiThread {
                    Toast.makeText(context, "UniPack Saved to Downloads", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) { }
    }
}
