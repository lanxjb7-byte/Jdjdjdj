package com.lixxzy.lite;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private final String ASSET_FOLDER = "gameassetbundles";
    // Path folder tujuan FF TH (Free Fire Biasa)
    private final String TARGET_DIR = "/storage/emulated/0/Android/data/com.dts.freefireth/files/contentcache/Optional/android/gameassetbundles/";
    
    private Switch switchMod;
    private boolean isModActive = false;
    private static final int SHIZUKU_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        switchMod = findViewById(R.id.switchMod);
        Button btnFF = findViewById(R.id.btnFF);

        // Minta izin storage standar (buat baca file)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        // Toggle ON / OFF
        switchMod.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isModActive = isChecked;
            if (isChecked) {
                switchMod.setText("STATUS: ON (Siap Inject)");
                switchMod.setTextColor(0xFF00FFCC); // Cyan
            } else {
                switchMod.setText("STATUS: OFF");
                switchMod.setTextColor(0xFFFF0033); // Merah
                if (checkShizukuPermission()) {
                    executeShizukuCmd("rm -rf " + TARGET_DIR + "*"); // Hapus file pakai Shizuku
                    Toast.makeText(this, "File Mod Dihapus, Game Normal", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Tombol START
        btnFF.setOnClickListener(v -> {
            if (!isModActive) {
                Toast.makeText(this, "Aktifkan Switch ON dulu!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cek izin Shizuku dulu
            if (!checkShizukuPermission()) {
                requestShizukuPermission();
                return;
            }

            injectFiles();
        });
    }

    private void injectFiles() {
        Toast.makeText(this, "Mengekstrak file mod...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                // 1. Ekstrak file dari dalam APK ke folder cache sementara
                File cacheDir = new File(getExternalFilesDir(null), "mod_temp");
                if (!cacheDir.exists()) cacheDir.mkdirs();

                String[] files = getAssets().list(ASSET_FOLDER);
                if (files == null || files.length == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "File di folder " + ASSET_FOLDER + " kosong!", Toast.LENGTH_LONG).show());
                    return;
                }

                for (String file : files) {
                    InputStream in = getAssets().open(ASSET_FOLDER + "/" + file);
                    OutputStream out = new FileOutputStream(new File(cacheDir, file));
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();
                    out.close();
                }

                // 2. Pakai Shizuku buat mindahin dari cache ke folder FF
                String cmd = "mkdir -p " + TARGET_DIR + " && cp -r " + cacheDir.getAbsolutePath() + "/* " + TARGET_DIR;
                executeShizukuCmd(cmd);

                // 3. Bersihkan folder cache
                File[] tempFiles = cacheDir.listFiles();
                if (tempFiles != null) {
                    for (File f : tempFiles) f.delete();
                }
                cacheDir.delete();

                // 4. Buka Game
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Mod Aktif! Membuka Game...", Toast.LENGTH_SHORT).show();
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
                    if (launchIntent != null) {
                        startActivity(launchIntent);
                    } else {
                        Toast.makeText(MainActivity.this, "Game Free Fire tidak ditemukan!", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ==========================================
    // BAGIAN SHIZUKU (DIPERBAIKI)
    // ==========================================

    private boolean checkShizukuPermission() {
        if (Shizuku.isPreV11()) return false;
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    private void requestShizukuPermission() {
        if (Shizuku.isPreV11()) {
            Toast.makeText(this, "Versi Shizuku terlalu lama.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "Izin Shizuku ditolak permanen. Buka aplikasi Shizuku.", Toast.LENGTH_LONG).show();
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE);
        }
    }

    // Fungsi buat ngejalanin perintah Shell lewat Shizuku pakai Java Reflection
    private void executeShizukuCmd(String command) {
        try {
            // Trik Reflection buat nembus method private Shizuku.newProcess
            Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            method.setAccessible(true);
            
            // Jalanin perintah shell-nya
            Process process = (Process) method.invoke(null, new String[]{"sh", "-c", command}, null, null);
            process.waitFor(); // Tunggu proses copy selesai
            
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Shizuku CMD Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // Listener buat nangkep pop-up izin dari Shizuku
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_LISTENER = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku Diizinkan! Silakan klik START lagi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin Shizuku ditolak!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Daftarin listener pas aplikasi dibuka
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_LISTENER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hapus listener pas aplikasi di-minimize
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_LISTENER);
    }
                                              }
