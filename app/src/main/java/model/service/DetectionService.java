package model.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import model.helper.DetectorCallback;
import model.helper.DetectorThread;
import model.helper.DetectorType;
import model.helper.RecorderThread;

/**
 * A detection service that runs as a foreground service,
 * continuously listening for sound (whistle detection).
 */
public class DetectionService extends Service implements DetectorCallback {

    private static final String TAG = "DetectionService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "detection_channel_id";

    private DetectorThread mDetectorThread;
    private RecorderThread mRecorderThread;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private static Context _context;

    /**
     * Start the detection service.
     */
    public static void startDetection(Context context) {
        _context = context;
        Toast.makeText(context, "Detection Service Started!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, DetectionService.class);
        intent.putExtra("action", "start");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Stop the detection service.
     */
    public static void stopDetection(Context context) {
        _context = context;
        Toast.makeText(context, "Detection Service Stopped!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(context, DetectionService.class);
        intent.putExtra("action", "stop");
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Acquire a wake lock to keep CPU running when the screen is off
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DetectionService::Wakelock");
            wakeLock.acquire();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

        // Ensure the app has the required permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO permission not granted");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Start the service in the foreground
        try {
            Notification notification = createNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Failed to start foreground service", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Initialize Vibrator service
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (intent != null && intent.getExtras() != null) {
            String action = intent.getStringExtra("action");
            Log.d(TAG, "Action: " + action);
            if ("start".equals(action)) {
                startDetectionTask();
            } else if ("stop".equals(action)) {
                stopDetectionTask();
                stopSelf();
            }
        } else {
            startDetectionTask();
            Log.d(TAG, "Intent or extras are null, defaulting to start detection");
        }

        return START_STICKY;
    }

    /**
     * Creates a notification channel (Required for Android O and above).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Detection Service Channel";
            String description = "Channel for ongoing detection service";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Creates a notification for the foreground service.
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Detection Service")
                .setContentText("Listening for whistle sounds...")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true) // Ensure it is a foreground service
                .build();
    }

    /**
     * Called when a whistle is detected.
     */
    @Override
    public void onWhistleDetected() {
        Log.d(TAG, "onWhistleDetected");
        triggerVibration();
    }

    /**
     * Starts the detection process.
     */
    private void startDetectionTask() {
        Log.d(TAG, "Starting detection task...");
        try {
            stopDetectionTask();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize and start the RecorderThread
        mRecorderThread = new RecorderThread();
        mRecorderThread.startRecording();

        // Initialize and start the DetectorThread for whistle detection
        mDetectorThread = new DetectorThread(mRecorderThread, DetectorType.WHISTLE, this);
        mDetectorThread.start();
    }

    /**
     * Triggers vibration when detection is activated.
     */
    private void triggerVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] vibrationPattern = {0, 500, 250, 500}; // Vibrate, pause, vibrate
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(vibrationPattern, -1); // No repeat
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(vibrationPattern, -1); // Deprecated in API 26 but works for older devices
            }
            Log.d(TAG, "Vibration triggered.");
            Toast.makeText(_context, "Vibration triggered.", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "Device does not support vibration.");
            Toast.makeText(_context, "Vibration not triggered.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stops the detection process.
     */
    private void stopDetectionTask() {
        Log.d(TAG, "Stopping detection task...");
        if (mDetectorThread != null) {
            mDetectorThread.stopDetection();
            mDetectorThread = null;
        }
        if (mRecorderThread != null) {
            mRecorderThread.stopRecording();
            mRecorderThread = null;
        }
    }

    @Override
    public boolean stopService(Intent name) {
        stopDetectionTask();
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        stopDetectionTask();
        // Release the wake lock if it is held.
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // This service is not designed for binding.
        return null;
    }
}