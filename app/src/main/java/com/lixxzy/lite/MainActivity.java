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
    // Path folder tujuan FF TH
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
                switchMod.setTextColor(0xFF00FFCC); 
            } else {
                switchMod.setText("STATUS: OFF");
                switchMod.setTextColor(0xFFFF0033); 
                if (checkShizukuPermission()) {
                    removeFiles(); // Panggil fungsi hapus yang aman
                }
            }
        });

        btnFF.setOnClickListener(v -> {
            if (!isModActive) {
                Toast.makeText(this, "Aktifkan Switch ON dulu!", Toast.LENGTH_SHORT).show();
                return;
            }
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
                File cacheDir = new File(getExternalFilesDir(null), "mod_temp");
                if (!cacheDir.exists()) cacheDir.mkdirs();

                String[] files = getAssets().list(ASSET_FOLDER);
                if (files == null || files.length == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "File di folder " + ASSET_FOLDER + " kosong!", Toast.LENGTH_LONG).show());
                    return;
                }

                // Copy dari APK ke Cache
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

                // Pindah dari Cache ke Folder FF pakai Shizuku
                String cmd = "mkdir -p " + TARGET_DIR + " && cp -r " + cacheDir.getAbsolutePath() + "/* " + TARGET_DIR;
                executeShizukuCmd(cmd);

                // Bersihkan Cache
                File[] tempFiles = cacheDir.listFiles();
                if (tempFiles != null) {
                    for (File f : tempFiles) f.delete();
                }
                cacheDir.delete();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Mod Aktif! Membuka Game...", Toast.LENGTH_SHORT).show();
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.dts.freefireth");
                    if (launchIntent != null) startActivity(launchIntent);
                });

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // ==========================================
    // FUNGSI HAPUS YANG AMAN (HANYA FILE DARI APK)
    // ==========================================
    private void removeFiles() {
        try {
            String[] files = getAssets().list(ASSET_FOLDER);
            if (files != null && files.length > 0) {
                StringBuilder cmd = new StringBuilder();
                for (String file : files) {
                    // Perintah "rm -f" cuma ngehapus file spesifik, bukan semua folder
                    cmd.append("rm -f ").append(TARGET_DIR).append(file).append("; ");
                }
                executeShizukuCmd(cmd.toString());
                Toast.makeText(this, "File Mod Dihapus, Game Asli Tetap Aman", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Gagal hapus: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ==========================================
    // BAGIAN SHIZUKU
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
            Toast.makeText(this, "Izin Shizuku ditolak permanen.", Toast.LENGTH_LONG).show();
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE);
        }
    }

    private void executeShizukuCmd(String command) {
        try {
            Method method = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            method.setAccessible(true);
            Process process = (Process) method.invoke(null, new String[]{"sh", "-c", command}, null, null);
            process.waitFor(); 
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Shizuku CMD Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_LISTENER = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Shizuku Diizinkan! Klik START lagi.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Izin Shizuku ditolak!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_LISTENER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_LISTENER);
    }
                        }
