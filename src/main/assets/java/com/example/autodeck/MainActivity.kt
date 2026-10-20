package com.example.autodeck

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JsPromptResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.settings.apply {
            // Enable JavaScript execution for HTML logic
            javaScriptEnabled = true
            
            // Enable LocalStorage API for persisting custom shortcuts/bookmarks
            domStorageEnabled = true
            databaseEnabled = true
            
            // Allow responsive mobile scaling
            useWideViewPort = true
            loadWithOverviewMode = true
            
            // Bypass YouTube/Google anti-embed blocks by identifying as standard mobile Chrome
            userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        // Keep page transitions inside the app container
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        // Enable JavaScript alert/prompt dialogs (e.g., adding bookmark titles)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                // Handled natively by Android WebChromeClient for prompt UI
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }
        }

        // Load the local timonauto.html dashboard from app/src/main/assets/
        webView.loadUrl("file:///android_asset/timonauto.html")
    }

    // Handle back button navigation inside WebView history
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
