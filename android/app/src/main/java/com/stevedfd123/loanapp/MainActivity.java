package com.stevedfd123.loanapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Thin native shell around the existing web app.
 *
 * The whole UI lives in assets/www/index.html (a copy of the repo's index.html),
 * so the HTML/CSS/JS stays the single source of truth and can be re-published to
 * GitHub Pages without touching this project.
 */
public class MainActivity extends Activity {

    private WebView webView;
    private ProgressBar progressBar;

    /** The published CSV link the web app reads its loan data from. */
    private static final String CSV_URL =
            "https://docs.google.com/spreadsheets/d/e/"
                    + "2PACX-1vSqEJvs5Tnz2x1oCo5xOURlss-qdpBeF8Ew2-7wllFw17LU_jaxJ0YF-Gw8ncqUpAru6"
                    + "LfK25HlL__U/pub?gid=0&single=true&output=csv";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebChromeClient(new WebChromeClient() {
            // Without a WebChromeClient, alert()/confirm() are silently dropped.
        });

        webView.setWebViewClient(new WebViewClient() {

            /**
             * The web app is served from file:///android_asset, whose origin is
             * "null", so the CSV request (the URL held in index.html as CSV_URL)
             * is a cross-origin fetch. Fetching it natively here sidesteps CORS
             * entirely and guarantees the sheet loads even if Google's CORS
             * headers change.
             */
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view,
                                                              WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://docs.google.com/spreadsheets/")
                        && (url.contains("output=csv") || url.contains("tqx=out:csv"))) {
                    byte[] body = download(url);
                    if (body != null) {
                        return new WebResourceResponse(
                                "text/csv",
                                "UTF-8",
                                new ByteArrayInputStream(body));
                    }
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep http/https in the shell, hand tel:/mailto:/market: to the system.
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if (scheme == null) {
                    return false;
                }
                if (scheme.equals("http") || scheme.equals("https")
                        || scheme.equals("file") || scheme.equals("data")) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl("file:///android_asset/www/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    /** Downloads the published CSV with no CORS restrictions. */
    private byte[] download(String urlString) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            InputStream in = conn.getInputStream();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }
}
