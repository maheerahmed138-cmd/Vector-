package com.mobashir.vector;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.os.Build;
import android.widget.Toast;
import android.content.ActivityNotFoundException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQUEST_EXPORT_BACKUP = 501;
    private static final int REQUEST_IMPORT_BACKUP = 502;
    private WebView webView;
    private String pendingBackupJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.rgb(11, 18, 32));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(11, 18, 32));
        getWindow().getDecorView().setSystemUiVisibility(0);

        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(11, 18, 32));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);

        webView.addJavascriptInterface(new BackupBridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleExternalUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    return handleExternalUrl(request.getUrl().toString());
                }
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }


    /**
     * Routes intent:// links to their installed apps, PDF links to an external
     * PDF viewer, and HLS .m3u8 links to a compatible external video player.
     */
    private boolean handleExternalUrl(String url) {
        if (url == null) return false;

        try {
            if (url.startsWith("intent:")) {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null && !fallback.isEmpty()) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fallback)));
                    } else {
                        Toast.makeText(this, "Required app is not installed", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }

            Uri uri = Uri.parse(url);
            String path = uri.getPath();
            String lowerPath = path == null ? "" : path.toLowerCase();

            // Open PDF documents in an installed PDF viewer instead of WebView,
            // which does not reliably render PDF files directly.
            if (lowerPath.endsWith(".pdf") || lowerPath.contains(".pdf/")) {
                Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
                pdfIntent.setDataAndType(uri, "application/pdf");
                pdfIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(Intent.createChooser(pdfIntent, "Open PDF with"));
                } catch (ActivityNotFoundException e) {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(browserIntent);
                    } catch (ActivityNotFoundException ignored) {
                        Toast.makeText(this, "No PDF viewer found. Please install a PDF reader.", Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            }

            if (path != null && path.toLowerCase().contains(".m3u8")) {
                Intent videoIntent = new Intent(Intent.ACTION_VIEW);
                videoIntent.setDataAndType(uri, "video/*");
                videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(videoIntent);
                } catch (ActivityNotFoundException e) {
                    // Some players register HLS as a generic URL rather than video/*.
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    } catch (ActivityNotFoundException ignored) {
                        Toast.makeText(this, "No compatible video player found", Toast.LENGTH_LONG).show();
                    }
                }
                return true;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    private class BackupBridge {
        @JavascriptInterface
        public void openVideo(String videoUrl, String videoTitle) {
            runOnUiThread(() -> {
                if (videoUrl == null || videoUrl.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Video link missing", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent playerIntent = new Intent(MainActivity.this, PlayerActivity.class);
                playerIntent.putExtra(PlayerActivity.EXTRA_VIDEO_URL, videoUrl);
                playerIntent.putExtra(PlayerActivity.EXTRA_VIDEO_TITLE,
                        videoTitle == null ? "VECTOR Player" : videoTitle);
                startActivity(playerIntent);
            });
        }

        @JavascriptInterface
        public void exportBackup(String json) {
            runOnUiThread(() -> {
                pendingBackupJson = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "vector-backup.json");
                startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
            });
        }

        @JavascriptInterface
        public void importBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"application/json", "text/json", "application/octet-stream"});
                startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingBackupJson = null;
            return;
        }

        Uri uri = data.getData();
        if (requestCode == REQUEST_EXPORT_BACKUP) {
            writeBackup(uri);
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            readBackup(uri);
        }
    }

    private void writeBackup(Uri uri) {
        if (pendingBackupJson == null) {
            Toast.makeText(this, "No backup data available", Toast.LENGTH_SHORT).show();
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new Exception("Unable to open destination");
            output.write(pendingBackupJson.getBytes(StandardCharsets.UTF_8));
            output.flush();
            Toast.makeText(this, "Backup saved", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup export failed", Toast.LENGTH_LONG).show();
        } finally {
            pendingBackupJson = null;
        }
    }

    private void readBackup(Uri uri) {
        try (InputStream input = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
            String json = content.toString();
            String quoted = org.json.JSONObject.quote(json);
            webView.evaluateJavascript(
                    "window.restoreBackupFromAndroid(" + quoted + ");", null);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to import backup", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
