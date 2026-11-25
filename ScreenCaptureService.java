package com.autoclicker.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.content.Context;
import android.content.pm.ServiceInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

/** Servicio de primer plano para obtener una sola imagen usando MediaProjection. */
public class ScreenCaptureService extends Service {
    public static final String ACTION_CAPTURE_SAVED =
            "com.autoclicker.android.ACTION_CAPTURE_SAVED";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";
    public static final String EXTRA_FILE_PATH = "file_path";

    private static final String CHANNEL_ID = "legionario_screen_capture";
    private static final int NOTIFICATION_ID = 7102;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private boolean captureFinished;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !intent.hasExtra(EXTRA_RESULT_DATA)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        startCaptureForeground();
        int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
        Intent resultData;
        if (Build.VERSION.SDK_INT >= 33) {
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent.class);
        } else {
            //noinspection deprecation
            resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
        }
        if (resultData == null || resultCode == 0) {
            finishWithError();
            return START_NOT_STICKY;
        }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection = manager.getMediaProjection(resultCode, resultData);
            if (projection == null) {
                finishWithError();
                return START_NOT_STICKY;
            }
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() {
                    releaseCaptureResources();
                    stopSelf();
                }
            }, handler);
            createVirtualDisplay();
            // Da tiempo al sistema para presentar el contenido en el VirtualDisplay.
            handler.postDelayed(this::readOneFrame, 1800);
        } catch (SecurityException | IllegalStateException e) {
            finishWithError();
        }
        return START_NOT_STICKY;
    }

    private void startCaptureForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Captura de pantalla", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Notificación temporal durante una captura autorizada");
            nm.createNotificationChannel(channel);
        }
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Legionario VIP")
                .setContentText("Realizando una captura autorizada")
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void createVirtualDisplay() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);
        int width = Math.max(1, metrics.widthPixels);
        int height = Math.max(1, metrics.heightPixels);
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            // La lectura se programa desde readOneFrame para evitar consumir
            // imágenes antes de que el sistema haya terminado de dibujar.
        }, handler);
        virtualDisplay = projection.createVirtualDisplay(
                "LegionarioVIP-ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);
    }

    private void readOneFrame() {
        if (captureFinished || imageReader == null) return;
        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image == null) {
                handler.postDelayed(this::readOneFrame, 350);
                return;
            }
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int width = image.getWidth();
            int height = image.getHeight();
            int rowPadding = rowStride - pixelStride * width;
            int paddedWidth = width + rowPadding / pixelStride;

            Bitmap padded = Bitmap.createBitmap(
                    paddedWidth, height, Bitmap.Config.ARGB_8888);
            buffer.rewind();
            padded.copyPixelsFromBuffer(buffer);
            Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
            padded.recycle();

            String path = saveBitmap(cropped);
            cropped.recycle();
            captureFinished = true;
            broadcastResult(path);
            stopProjectionAndSelf();
        } catch (Exception e) {
            finishWithError();
        } finally {
            if (image != null) image.close();
        }
    }

    private String saveBitmap(Bitmap bitmap) throws Exception {
        File pictures = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (pictures == null) pictures = getFilesDir();
        File folder = new File(pictures, "LegionarioVIP");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("No se pudo crear la carpeta de capturas");
        }
        File file = new File(folder, "captura_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IllegalStateException("No se pudo codificar la imagen");
            }
            output.flush();
        }
        return file.getAbsolutePath();
    }

    private void broadcastResult(String path) {
        Intent result = new Intent(ACTION_CAPTURE_SAVED);
        result.setPackage(getPackageName());
        result.putExtra(EXTRA_FILE_PATH, path);
        sendBroadcast(result);
    }

    private void finishWithError() {
        if (!captureFinished) {
            captureFinished = true;
            broadcastResult(null);
        }
        stopProjectionAndSelf();
    }

    private void stopProjectionAndSelf() {
        releaseCaptureResources();
        if (projection != null) {
            try { projection.stop(); } catch (Exception ignored) {}
            projection = null;
        }
        stopForeground(true);
        stopSelf();
    }

    private void releaseCaptureResources() {
        if (virtualDisplay != null) {
            try { virtualDisplay.release(); } catch (Exception ignored) {}
            virtualDisplay = null;
        }
        if (imageReader != null) {
            try { imageReader.close(); } catch (Exception ignored) {}
            imageReader = null;
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseCaptureResources();
        if (projection != null) {
            try { projection.stop(); } catch (Exception ignored) {}
            projection = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
