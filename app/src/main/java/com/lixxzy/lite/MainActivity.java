package com.lixxzy.lite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    
    // PATH KHUSUS FREE FIRE TH (BIASA)
    private final String TARGET_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + 
            "/Android/data/com.dts.freefireth/files/contentcache/Optional/android/gameassetbundles/";
            
    // NAMA FOLDER ASET DI DALAM APK (Sesuai yang lo bilang: gameassetbundles)
    private final String ASSET_FOLDER = "gameassetbundles";

    private Switch switchMod;
    private boolean isModActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchMod = findViewById(R.id.switchMod);
        Button btnFF = findViewById(R.id.btnFF); // Cuma ada tombol FF TH

        // Minta izin All Files Access (Android 11+)
        mintaIzin();

        // Logika Toggle ON/OFF
        switchMod.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isModActive = isChecked;
            if (isChecked) {
                switchMod.setText("STATUS: ON (Siap Inject)");
                switchMod.setTextColor(0xFF00FFCC); // Cyan
            } else {
                switchMod.setText("STATUS: OFF");
                switchMod.setTextColor(0xFFFF0033); // Merah
                removeFiles(); // Hapus file dari folder FF
            }
        });

        // Tombol START FF TH
        btnFF.setOnClickListener(v -> {
            if (!isModActive) {
                Toast.makeText(this, "Aktifkan Switch ON dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
            injectFiles();
        });
    }

    private void mintaIzin() {
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
    }

    private void injectFiles() {
        Toast.makeText(this, "Mengekstrak file mod...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                File dir = new File(TARGET_DIR);
                if (!dir.exists()) dir.mkdirs();

                String[] files = getAssets().list(ASSET_FOLDER);
                if (files == null || files.length == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "File di folder " + ASSET_FOLDER + " kosong!", Toast.LENGTH_LONG).show());
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
                
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Mod Aktif! Membuka Game...", Toast.LENGTH_SHORT).show();
                    
                    // Otomatis buka Free Fire TH
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    } else {
                        Toast.makeText(MainActivity.this, "Free Fire tidak ditemukan di HP!", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void removeFiles() {
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
                Toast.makeText(this, "File Mod Dihapus, Game Normal", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menghapus file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
        }
