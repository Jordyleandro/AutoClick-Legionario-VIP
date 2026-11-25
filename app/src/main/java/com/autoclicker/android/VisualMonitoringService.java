package com.autoclicker.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.content.pm.ServiceInfo;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Vigilancia visual continua, con autorización explícita de MediaProjection. */
public class VisualMonitoringService extends Service {
    public static final String ACTION_STATE = "com.autoclicker.android.VISUAL_MONITOR_STATE";
    public static final String ACTION_STOP = "com.autoclicker.android.VISUAL_MONITOR_STOP";
    public static final String EXTRA_RESULT_CODE = "monitor_result_code";
    public static final String EXTRA_RESULT_DATA = "monitor_result_data";
    public static final String EXTRA_TEMPLATE_URI = "monitor_template_uri";
    public static final String EXTRA_INTERVAL = "monitor_interval";
    public static final String EXTRA_THRESHOLD = "monitor_threshold";
    public static final String EXTRA_PRESENT = "monitor_present";
    public static final String EXTRA_X = "monitor_x";
    public static final String EXTRA_Y = "monitor_y";
    public static final String EXTRA_SCORE = "monitor_score";
    public static final String EXTRA_MESSAGE = "monitor_message";
    public static final String EXTRA_RUNNING = "monitor_running";

    private static final String CHANNEL_ID = "legionario_visual_monitor";
    private static final int NOTIFICATION_ID = 7306;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private Bitmap template;
    private long intervalMs = 1500;
    private int threshold = 82;
    private long lastSampleAt;
    private boolean active;
    private boolean processing;
    private boolean targetPresent;
    private boolean stopping;
    private int matchX = -1, matchY = -1;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (ACTION_STOP.equals(intent.getAction())) {
            stopMonitoring();
            return START_NOT_STICKY;
        }

        startForegroundNotification();
        try {
            intervalMs = Math.max(500, Math.min(10000,
                    intent.getLongExtra(EXTRA_INTERVAL, 1500)));
            threshold = Math.max(50, Math.min(99,
                    intent.getIntExtra(EXTRA_THRESHOLD, 82)));
            String uriText = intent.getStringExtra(EXTRA_TEMPLATE_URI);
            if (uriText == null) throw new IllegalArgumentException("Falta la imagen de referencia.");
            try (InputStream in = getContentResolver().openInputStream(Uri.parse(uriText))) {
                template = BitmapFactory.decodeStream(in);
            }
            if (template == null || template.getWidth() < 12 || template.getHeight() < 12) {
                throw new IllegalArgumentException("La plantilla no se pudo leer o es demasiado pequeña.");
            }

            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData;
            if (Build.VERSION.SDK_INT >= 33) {
                resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
            } else {
                //noinspection deprecation
                resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            }
            if (resultCode == 0 || resultData == null) {
                throw new IllegalArgumentException("No se recibió la autorización de captura.");
            }

            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(resultCode, resultData);
            if (projection == null) throw new IllegalStateException("Android no entregó MediaProjection.");
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() {
                    if (!stopping) stopMonitoring();
                }
            }, handler);
            createDisplay();
            active = true;
            targetPresent = false;
            lastSampleAt = 0;
            sendState(false, -1, -1, 0, "Vigilancia visual iniciada.");
        } catch (Exception e) {
            sendState(false, -1, -1, 0,
                    "No se pudo iniciar la vigilancia: " + safeMessage(e));
            stopMonitoring();
        }
        return START_NOT_STICKY;
    }

    private void startForegroundNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Vigilancia visual", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Indica cuándo la vigilancia visual está activa.");
            manager.createNotificationChannel(channel);
        }
        Intent open = new Intent(this, VisualMonitoringActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 7306, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Legionario VIP 6.6")
                .setContentText("Vigilancia visual activa")
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int width = Math.max(1, metrics.widthPixels);
        int height = Math.max(1, metrics.heightPixels);
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
        reader.setOnImageAvailableListener(this::onFrameAvailable, handler);
        display = projection.createVirtualDisplay("LegionarioVIP-VisualMonitor",
                width, height, metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, handler);
    }

    private void onFrameAvailable(ImageReader source) {
        Image image = null;
        try {
            image = source.acquireLatestImage();
            if (image == null) return;
            long now = SystemClock.elapsedRealtime();
            if (!active || processing || now - lastSampleAt < intervalMs) return;
            lastSampleAt = now;
            processing = true;
            Bitmap frame = imageToBitmap(image);
            Bitmap templateCopy = template.copy(Bitmap.Config.ARGB_8888, false);
            if (frame == null || templateCopy == null) {
                if (frame != null) frame.recycle();
                if (templateCopy != null) templateCopy.recycle();
                processing = false;
                return;
            }
            worker.execute(() -> {
                TemplateMatcher.Result result = new TemplateMatcher.Result(0, -1, -1, 0, 0);
                try {
                    result = TemplateMatcher.find(frame, templateCopy);
                } catch (Exception ignored) {
                    // La siguiente muestra volverá a intentarlo.
                } finally {
                    frame.recycle();
                    templateCopy.recycle();
                }
                TemplateMatcher.Result completed = result;
                handler.post(() -> handleMatch(completed));
            });
        } catch (Exception e) {
            processing = false;
            sendState(targetPresent, matchX, matchY, 0, "Error al analizar la captura: " + safeMessage(e));
        } finally {
            if (image != null) image.close();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowPadding = rowStride - pixelStride * width;
        int paddedWidth = width + rowPadding / pixelStride;
        Bitmap padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
        buffer.rewind();
        padded.copyPixelsFromBuffer(buffer);
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
        padded.recycle();
        return cropped;
    }

    private void handleMatch(TemplateMatcher.Result result) {
        processing = false;
        if (!active || result == null) return;
        boolean found = result.score >= threshold;
        if (found) {
            matchX = result.x + result.width / 2;
            matchY = result.y + result.height / 2;
        }
        if (found != targetPresent) {
            targetPresent = found;
            if (found) {
                sendState(true, matchX, matchY, result.score,
                        "Objetivo detectado. Revisa la pantalla antes de cualquier toque.");
                showDetectionAlert(result.score);
            } else {
                matchX = matchY = -1;
                sendState(false, -1, -1, result.score,
                        "El objetivo ya no está visible. Se avisará si vuelve a aparecer.");
            }
        } else if (found) {
            sendState(true, matchX, matchY, result.score, "Objetivo visible.");
        }
    }

    private void showDetectionAlert(double score) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent open = new Intent(this, VisualMonitoringActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 7307, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification alert = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Legionario VIP: objetivo detectado")
                .setContentText(String.format(java.util.Locale.getDefault(),
                        "Coincidencia visual %.1f%%. Abre la app para revisar.", score))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        manager.notify(7307, alert);
    }

    private void sendState(boolean present, int x, int y, double score, String message) {
        getSharedPreferences("visual_monitor_state", MODE_PRIVATE).edit()
                .putBoolean("running", active)
                .putBoolean("present", present)
                .putInt("x", x)
                .putInt("y", y)
                .putFloat("score", (float) score)
                .putString("message", message)
                .apply();
        Intent event = new Intent(ACTION_STATE);
        event.setPackage(getPackageName());
        event.putExtra(EXTRA_RUNNING, active);
        event.putExtra(EXTRA_PRESENT, present);
        event.putExtra(EXTRA_X, x);
        event.putExtra(EXTRA_Y, y);
        event.putExtra(EXTRA_SCORE, score);
        event.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(event);
    }

    private void stopMonitoring() {
        if (stopping) return;
        stopping = true;
        active = false;
        processing = false;
        sendState(false, -1, -1, 0, "Vigilancia detenida.");
        if (display != null) {
            try { display.release(); } catch (Exception ignored) {}
            display = null;
        }
        if (reader != null) {
            try { reader.close(); } catch (Exception ignored) {}
            reader = null;
        }
        MediaProjection oldProjection = projection;
        projection = null;
        if (oldProjection != null) {
            try { oldProjection.stop(); } catch (Exception ignored) {}
        }
        if (template != null && !template.isRecycled()) {
            template.recycle();
            template = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private String safeMessage(Exception e) {
        return e.getLocalizedMessage() == null ? "error desconocido" : e.getLocalizedMessage();
    }

    @Override public void onDestroy() {
        active = false;
        handler.removeCallbacksAndMessages(null);
        if (display != null) try { display.release(); } catch (Exception ignored) {}
        if (reader != null) try { reader.close(); } catch (Exception ignored) {}
        if (projection != null) try { projection.stop(); } catch (Exception ignored) {}
        if (template != null && !template.isRecycled()) template.recycle();
        worker.shutdownNow();
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
