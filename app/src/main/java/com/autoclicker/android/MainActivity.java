package com.autoclicker.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 42, 30, 30);

        TextView title = new TextView(this);
        title.setText("LEGIONARIO VIP 6.6");
        title.setTextSize(26);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("\nVersión 6.6 basada en la 6.5. Se conservan AutoClick, Flooder, macros, OCR y captura puntual.");
        layout.addView(info);

        Button config = new Button(this);
        config.setText("CONFIGURAR AUTOCLICK Y FLOODER");
        config.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)));
        layout.addView(config);

        Button macros = new Button(this);
        macros.setText("CONFIGURAR Y REPRODUCIR MACROS");
        macros.setOnClickListener(v -> startActivity(new Intent(this, MacroActivity.class)));
        layout.addView(macros);

        Button capture = new Button(this);
        capture.setText("CAPTURA DE PANTALLA Y OCR");
        capture.setOnClickListener(v -> startActivity(new Intent(this, ScreenCaptureActivity.class)));
        layout.addView(capture);

        Button visual = new Button(this);
        visual.setText("RECONOCIMIENTO VISUAL POR PLANTILLA");
        visual.setOnClickListener(v -> startActivity(new Intent(this, ImageRecognitionActivity.class)));
        layout.addView(visual);

        Button monitor = new Button(this);
        monitor.setText("NUEVO: VIGILANCIA VISUAL CONTINUA 6.6");
        monitor.setOnClickListener(v -> startActivity(new Intent(this, VisualMonitoringActivity.class)));
        layout.addView(monitor);

        Button accessibility = new Button(this);
        accessibility.setText("ACTIVAR SERVICIO DE ACCESIBILIDAD");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        layout.addView(accessibility);

        Button overlay = new Button(this);
        overlay.setText("AUTORIZAR PANEL FLOTANTE");
        overlay.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));
        layout.addView(overlay);

        Button start = new Button(this);
        start.setText("ABRIR PANEL FLOTANTE");
        start.setOnClickListener(v -> startService(new Intent(this, FloatingPanelService.class)));
        layout.addView(start);
        setContentView(layout);
    }
}
