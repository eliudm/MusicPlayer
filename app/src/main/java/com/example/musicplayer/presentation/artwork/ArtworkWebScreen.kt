package com.example.musicplayer.presentation.artwork

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

private const val JS_INJECT = """
(function() {
    if (window.__artworkListenerAttached) return;
    window.__artworkListenerAttached = true;
    document.addEventListener('click', function(e) {
        var el = e.target;
        for (var i = 0; i < 5; i++) {
            if (!el || el === document.body) break;
            if (el.tagName === 'IMG') {
                var src = el.src || el.getAttribute('data-src') || el.getAttribute('data-original');
                if (src && src.indexOf('http') === 0) {
                    Android.onImageTapped(src);
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }
            }
            el = el.parentElement;
        }
    }, true);
})();
"""

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkWebScreen(
    onNavigateBack: () -> Unit,
    viewModel: ArtworkWebViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var pendingImageUrl by remember { mutableStateOf<String?>(null) }
    var pageTitle by remember { mutableStateOf("") }
    var isPageLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate back when artwork saved
    LaunchedEffect(state.saved) {
        if (state.saved) onNavigateBack()
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Confirmation dialog
    pendingImageUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingImageUrl = null },
            title = { Text("Use this image as artwork?") },
            text = {
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.downloadAndSave(url)
                    pendingImageUrl = null
                }) { Text("Use") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImageUrl = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            pageTitle.ifBlank { "Browse Images" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { webViewRef?.goBack() },
                            enabled = canGoBack
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(
                            onClick = { webViewRef?.goForward() },
                            enabled = canGoForward
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }
                )
                if (isPageLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.initialUrl.isBlank()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val jsInterface = remember {
                    object {
                        @JavascriptInterface
                        fun onImageTapped(url: String) {
                            Handler(Looper.getMainLooper()).post {
                                pendingImageUrl = url
                            }
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"

                            addJavascriptInterface(jsInterface, "Android")

                            webChromeClient = object : WebChromeClient() {
                                override fun onReceivedTitle(view: WebView, title: String) {
                                    pageTitle = title
                                }
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    isPageLoading = newProgress < 100
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    canGoBack = view.canGoBack()
                                    canGoForward = view.canGoForward()
                                }
                                override fun onPageFinished(view: WebView, url: String) {
                                    canGoBack = view.canGoBack()
                                    canGoForward = view.canGoForward()
                                    view.evaluateJavascript(JS_INJECT, null)
                                }
                                override fun shouldOverrideUrlLoading(
                                    view: WebView, request: WebResourceRequest
                                ): Boolean {
                                    val url = request.url.toString().lowercase()
                                    if (url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                                        url.endsWith(".png") || url.endsWith(".webp")) {
                                        Handler(Looper.getMainLooper()).post {
                                            pendingImageUrl = request.url.toString()
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }

                            loadUrl(state.initialUrl)
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (state.isDownloading) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Saving artwork…")
                    }
                }
            }
        }
    }
}
