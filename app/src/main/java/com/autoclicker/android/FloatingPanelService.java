package com.autoclicker.android;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class FloatingPanelService extends Service {
    private WindowManager wm;
    private LinearLayout panel;
    private WindowManager.LayoutParams panelParams;
    private FlooderEngine flooder;
    private TextView status;
    private boolean autoclickEnabled;
    private boolean pointVisible;

    @Override public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Autoriza ventanas flotantes primero", Toast.LENGTH_LONG).show();
            stopSelf(); return;
        }
        flooder = new FlooderEngine();
        List<String> enabled = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (LegionarioConfig.commandEnabled(this, i)) {
                String c = LegionarioConfig.command(this, i);
                if (!c.trim().isEmpty()) enabled.add(c);
            }
        }
        flooder.configure(LegionarioConfig.flooderInterval(this),
                LegionarioConfig.flooderRandom(this), enabled);

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(14, 8, 14, 8);
        panel.setBackgroundColor(0xEE20242A);

        TextView title = new TextView(this);
        title.setText("LEGIONARIO VIP 6.7  ⋮  MOVER PANEL");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(16);
        title.setPadding(4, 8, 4, 8);
        panel.addView(title);
        title.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private int startX, startY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    downX = e.getRawX(); downY = e.getRawY();
                    startX = panelParams.x; startY = panelParams.y;
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    panelParams.x = Math.max(0, startX + Math.round(e.getRawX() - downX));
                    panelParams.y = Math.max(0, startY + Math.round(e.getRawY() - downY));
                    try { wm.updateViewLayout(panel, panelParams); } catch (Exception ignored) {}
                    return true;
                }
                return e.getAction() == MotionEvent.ACTION_UP;
            }
        });

        TextView cfg = new TextView(this);
        cfg.setText("Intervalo: " + LegionarioConfig.interval(this) + " ms | Burst: "
                + LegionarioConfig.burst(this) + "\nPunto: X " + Math.round(LegionarioConfig.pointX(this))
                + " / Y " + Math.round(LegionarioConfig.pointY(this)));
        cfg.setTextColor(0xFFFFFFFF);
        panel.addView(cfg);

        status = new TextView(this);
        status.setTextColor(0xFFB8E0B8);
        status.setPadding(2, 5, 2, 5);
        panel.addView(status);

        Button startClick = new Button(this);
        startClick.setText("INICIAR");
        startClick.setOnClickListener(v -> {
            if (AutoClickService.instance == null) {
                Toast.makeText(this, "Activa Legionario VIP en Accesibilidad", Toast.LENGTH_SHORT).show(); return;
            }
            AutoClickService.instance.startClicking(
                    LegionarioConfig.pointX(this), LegionarioConfig.pointY(this),
                    LegionarioConfig.interval(this), LegionarioConfig.burst(this),
                    LegionarioConfig.burstDelay(this), LegionarioConfig.pause(this),
                    LegionarioConfig.maxClicks(this));
            autoclickEnabled = true;
            updateStatus();
        });
        panel.addView(startClick);

        Button stopClick = new Button(this);
        stopClick.setText("DETENER");
        stopClick.setOnClickListener(v -> {
            if (AutoClickService.instance != null) AutoClickService.instance.stopClicking();
            autoclickEnabled = false;
            updateStatus();
        });
        panel.addView(stopClick);

        Button startFlooder = new Button(this);
        startFlooder.setText("INICIAR FLOODER");
        startFlooder.setOnClickListener(v -> {
            if (AutoClickService.instance == null) {
                Toast.makeText(this, "Activa Accesibilidad", Toast.LENGTH_SHORT).show(); return;
            }
            if (LegionarioConfig.flooderAutoClick(this) && !AutoClickService.instance.isClicking()) {
                AutoClickService.instance.startClicking(
                        LegionarioConfig.pointX(this), LegionarioConfig.pointY(this),
                        LegionarioConfig.interval(this), LegionarioConfig.burst(this),
                        LegionarioConfig.burstDelay(this), LegionarioConfig.pause(this),
                        LegionarioConfig.maxClicks(this));
                autoclickEnabled = true;
            }
            flooder.start(); updateStatus();
        });
        panel.addView(startFlooder);

        Button stop = new Button(this);
        stop.setText("DETENER TODO");
        stop.setOnClickListener(v -> stopEngines());
        panel.addView(stop);

        Button showPoint = new Button(this);
        showPoint.setText("MOSTRAR / OCULTAR PUNTO X-Y");
        showPoint.setOnClickListener(v -> {
            if (pointVisible) {
                stopService(new Intent(this, CoordinatePointService.class));
                pointVisible = false;
            } else {
                startService(new Intent(this, CoordinatePointService.class));
                pointVisible = true;
            }
            updateStatus();
        });
        panel.addView(showPoint);

        Button close = new Button(this);
        close.setText("CERRAR PANEL");
        close.setOnClickListener(v -> stopSelf());
        panel.addView(close);

        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        panelParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.START; panelParams.x = 20; panelParams.y = 120;
        wm.addView(panel, panelParams);
        updateStatus();
    }

    private void updateStatus() {
        if (status != null) status.setText("AutoClick: "
                + ((AutoClickService.instance != null && AutoClickService.instance.isClicking()) ? "ACTIVO" : "DETENIDO")
                + " | Flooder: " + (flooder != null && flooder.isRunning() ? "ACTIVO" : "DETENIDO")
                + " | Punto: " + (pointVisible ? "VISIBLE" : "OCULTO"));
    }
    private void stopEngines() {
        if (AutoClickService.instance != null) AutoClickService.instance.stopClicking();
        if (flooder != null) flooder.stop();
        autoclickEnabled = false;
        updateStatus();
    }
    @Override public void onDestroy() {
        stopEngines();
        if (wm != null && panel != null) try { wm.removeView(panel); } catch (Exception ignored) {}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
