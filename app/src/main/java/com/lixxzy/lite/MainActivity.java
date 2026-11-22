package com.lixxzy.lite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    
    // Path folder tujuan di dalam Free Fire
    private final String TARGET_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + 
            "/Android/data/com.dts.freefireth/files/contentcache/Optional/android/gameassetbundles/";
            
    // Nama folder di dalam APK lo
    private final String ASSET_FOLDER = "gameassetbundlesl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Minta izin All Files Access (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        webView = new WebView(this);
        setContentView(webView);
        
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webView.setWebViewClient(new WebViewClient());
        
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void injectFiles(String mode) {
            try {
                File dir = new File(TARGET_DIR);
                if (!dir.exists()) dir.mkdirs();

                String[] files = getAssets().list(ASSET_FOLDER);
                if (files == null || files.length == 0) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "File di dalam APK kosong!", Toast.LENGTH_SHORT).show());
                    return;
                }

                // Proses Copy File
                for (String file : files) {
                    InputStream in = getAssets().open(ASSET_FOLDER + "/" + file);
                    OutputStream out = new FileOutputStream(new File(dir, file));
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                }
                
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Mod " + mode + " Aktif! Membuka Game...", Toast.LENGTH_SHORT).show());

                // OTOMATIS BUKA GAME
                String packageName = mode.contains("MAX") ? "com.dts.freefiremax" : "com.dts.freefireth";
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Game tidak ditemukan di HP!", Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void removeFiles() {
            try {
                File dir = new File(TARGET_DIR);
                if (dir.exists()) {
                    String[] files = getAssets().list(ASSET_FOLDER);
                    if (files != null) {
                        for (String file : files) {
                            File f = new File(dir, file);
                            if (f.exists()) f.delete();
                        }
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Mod Dimatikan & File Dibersihkan", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal menghapus: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }
                  }
