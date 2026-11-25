package com.autoclicker.android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/** Punto flotante arrastrable que guarda el centro como coordenadas de toque. */
public class CoordinatePointService extends Service {
    private WindowManager windowManager;
    private TextView point;
    private WindowManager.LayoutParams params;
    private int pointSize;

    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Autoriza ventanas flotantes primero", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        pointSize = Math.round(44 * getResources().getDisplayMetrics().density);
        point = new TextView(this);
        point.setText("●");
        point.setTextSize(30);
        point.setGravity(Gravity.CENTER);
        point.setTextColor(0xFFFFFFFF);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xEE00AEEF);
        bg.setStroke(Math.round(2 * getResources().getDisplayMetrics().density), 0xFFFFFFFF);
        point.setBackground(bg);
        point.setContentDescription("Punto de coordenadas. Arrastra para mover");
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        params = new WindowManager.LayoutParams(pointSize, pointSize, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.max(0, Math.round(LegionarioConfig.pointX(this) - pointSize / 2f));
        params.y = Math.max(0, Math.round(LegionarioConfig.pointY(this) - pointSize / 2f));
        point.setOnTouchListener(new View.OnTouchListener() {
            private float downRawX, downRawY;
            private int startX, startY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = params.x;
                    startY = params.y;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = Math.max(0, startX + Math.round(event.getRawX() - downRawX));
                    params.y = Math.max(0, startY + Math.round(event.getRawY() - downRawY));
                    try { windowManager.updateViewLayout(point, params); } catch (Exception ignored) {}
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float centerX = params.x + pointSize / 2f;
                    float centerY = params.y + pointSize / 2f;
                    LegionarioConfig.saveClickPoint(CoordinatePointService.this, centerX, centerY);
                    Toast.makeText(CoordinatePointService.this,
                            "Punto guardado: X " + Math.round(centerX) + " / Y " + Math.round(centerY),
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
                return true;
            }
        });
        try {
            windowManager.addView(point, params);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo mostrar el punto flotante", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }
    @Override public void onDestroy() {
        if (windowManager != null && point != null) {
            try { windowManager.removeView(point); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
