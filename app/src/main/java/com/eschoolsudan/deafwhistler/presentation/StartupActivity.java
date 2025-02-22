package com.eschoolsudan.deafwhistler.presentation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.eschoolsudan.deafwhistler.R;
import com.eschoolsudan.deafwhistler.databinding.ActivityStartupBinding;

import model.service.DetectionService;

public class StartupActivity extends FragmentActivity {

    private ActivityStartupBinding binding;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private boolean isStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStartupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Request permissions dynamically
        if (!hasPermissions()) {
            requestPermissions();
        }

        binding.txtstatus.setText(getString(R.string.stopped));

        binding.btnstartstop.setOnClickListener(v -> {
            if (!hasPermissions()) {
                Toast.makeText(this, "Please grant all permissions!", Toast.LENGTH_SHORT).show();
                requestPermissions();
                return;
            }

            isStarted = !isStarted;
            if (isStarted) {
                binding.btnstartstop.setText(getString(R.string.stop));
                binding.txtstatus.setText(getString(R.string.started));
                binding.txtstatus.setTextColor(getResources().getColor(android.R.color.holo_green_light, null));
                DetectionService.startDetection(StartupActivity.this);
                Toast.makeText(this, "Detection started", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnstartstop.setText(getString(R.string.start));
                binding.txtstatus.setText(getString(R.string.stopped));
                binding.txtstatus.setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                DetectionService.stopDetection(StartupActivity.this);
                Toast.makeText(this, "Detection stopped", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Checks if all required permissions are granted.
     */
    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request required permissions dynamically.
     */
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.VIBRATE,
                        Manifest.permission.FOREGROUND_SERVICE
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Required permissions not granted. The app may not work correctly!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
