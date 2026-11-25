package com.autoclicker.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;

/** Panel de vigilancia visual continua con confirmación manual para cada toque individual. */
public class VisualMonitoringActivity extends Activity {
    private static final int PICK_TEMPLATE = 7601;
    private static final int REQUEST_CAPTURE = 7602;
    private Uri templateUri;
    private Intent permissionData;
    private int permissionResult;
    private int matchX = -1, matchY = -1;
    private boolean targetPresent;
    private boolean monitoringRunning;
    private TextView status, intervalLabel, thresholdLabel;
    private Button startButton, stopButton, tapButton;
    private SeekBar intervalBar, thresholdBar;
    private long intervalMs = 1500;
    private int threshold = 82;
    private boolean receiverRegistered;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            applyState(intent.getBooleanExtra(VisualMonitoringService.EXTRA_RUNNING, false),
                    intent.getBooleanExtra(VisualMonitoringService.EXTRA_PRESENT, false),
                    intent.getIntExtra(VisualMonitoringService.EXTRA_X, -1),
                    intent.getIntExtra(VisualMonitoringService.EXTRA_Y, -1),
                    intent.getDoubleExtra(VisualMonitoringService.EXTRA_SCORE, 0),
                    intent.getStringExtra(VisualMonitoringService.EXTRA_MESSAGE));
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("LEGIONARIO VIP 6.6 — VIGILANCIA VISUAL");
        title.setTextSize(20);
        page.addView(title);

        TextView help = new TextView(this);
        help.setText("\nElige una imagen de referencia. Android solicitará permiso oficial para compartir la pantalla. La búsqueda se ejecuta localmente en este dispositivo. La vigilancia se puede detener en cualquier momento.");
        page.addView(help);

        Button choose = new Button(this);
        choose.setText("1. ELEGIR IMAGEN DE REFERENCIA");
        choose.setOnClickListener(v -> chooseTemplate());
        page.addView(choose);

        intervalLabel = new TextView(this);
        page.addView(intervalLabel);
        intervalBar = new SeekBar(this);
        intervalBar.setMax(95);
        intervalBar.setProgress(10);
        intervalBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                intervalMs = 500L + progress * 100L;
                intervalLabel.setText("Intervalo entre búsquedas: " + intervalMs + " ms");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        page.addView(intervalBar);

        thresholdLabel = new TextView(this);
        page.addView(thresholdLabel);
        thresholdBar = new SeekBar(this);
        thresholdBar.setMax(49);
        thresholdBar.setProgress(32);
        thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                threshold = 50 + progress;
                thresholdLabel.setText("Similitud mínima: " + threshold + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        page.addView(thresholdBar);

        startButton = new Button(this);
        startButton.setText("2. INICIAR VIGILANCIA");
        startButton.setOnClickListener(v -> requestCapturePermission());
        page.addView(startButton);

        stopButton = new Button(this);
        stopButton.setText("DETENER VIGILANCIA");
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(v -> stopMonitoring());
        page.addView(stopButton);

        status = new TextView(this);
        status.setText("\nEstado: esperando imagen de referencia.");
        status.setTextIsSelectable(true);
        page.addView(status);

        tapButton = new Button(this);
        tapButton.setText("CONFIRMAR TOQUE ÚNICO");
        tapButton.setEnabled(false);
        tapButton.setOnClickListener(v -> confirmSingleTap());
        page.addView(tapButton);

        Button back = new Button(this);
        back.setText("VOLVER");
        back.setOnClickListener(v -> finish());
        page.addView(back);

        setContentView(page);
        intervalLabel.setText("Intervalo entre búsquedas: " + intervalMs + " ms");
        thresholdLabel.setText("Similitud mínima: " + threshold + "%");
        IntentFilter filter = new IntentFilter(VisualMonitoringService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(stateReceiver, filter);
        receiverRegistered = true;
        refreshSavedState();
    }

    private void chooseTemplate() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_TEMPLATE);
    }

    private void requestCapturePermission() {
        if (templateUri == null) {
            status.setText("Primero elige una imagen de referencia.");
            return;
        }
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            status.setText("La captura de pantalla no está disponible en este dispositivo.");
            return;
        }
        status.setText("Esperando tu autorización en el diálogo oficial de Android...");
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TEMPLATE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            templateUri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(templateUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
            status.setText("Imagen de referencia seleccionada. Ajusta el intervalo y pulsa INICIAR VIGILANCIA.");
        } else if (requestCode == REQUEST_CAPTURE) {
            if (resultCode != RESULT_OK || data == null) {
                status.setText("Permiso de captura cancelado. La vigilancia no se inició.");
                return;
            }
            permissionResult = resultCode;
            permissionData = data;
            startMonitoring();
        }
    }

    private void startMonitoring() {
        Intent service = new Intent(this, VisualMonitoringService.class);
        service.putExtra(VisualMonitoringService.EXTRA_RESULT_CODE, permissionResult);
        service.putExtra(VisualMonitoringService.EXTRA_RESULT_DATA, permissionData);
        service.putExtra(VisualMonitoringService.EXTRA_TEMPLATE_URI, templateUri.toString());
        service.putExtra(VisualMonitoringService.EXTRA_INTERVAL, intervalMs);
        service.putExtra(VisualMonitoringService.EXTRA_THRESHOLD, threshold);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
        permissionData = null;
        monitoringRunning = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        status.setText("Vigilancia iniciándose. La búsqueda se repetirá cada " + intervalMs + " ms.");
    }

    private void stopMonitoring() {
        Intent stop = new Intent(this, VisualMonitoringService.class);
        stop.setAction(VisualMonitoringService.ACTION_STOP);
        startService(stop);
        monitoringRunning = false;
        targetPresent = false;
        matchX = matchY = -1;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        tapButton.setEnabled(false);
        status.setText("Solicitud de detención enviada.");
    }

    private void applyState(boolean running, boolean present, int x, int y, double score, String message) {
        monitoringRunning = running;
        startButton.setEnabled(!running);
        stopButton.setEnabled(running);
        targetPresent = running && present;
        matchX = x;
        matchY = y;
        tapButton.setEnabled(running && present && x >= 0 && y >= 0);
        String text = message == null ? "Estado actualizado." : message;
        if (present) {
            text += String.format(Locale.getDefault(),
                    "\nCoincidencia: %.1f%%\nCentro detectado: X=%d, Y=%d",
                    score, x, y);
        }
        status.setText(text);
    }

    private void refreshSavedState() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("visual_monitor_state", MODE_PRIVATE);
        boolean running = prefs.getBoolean("running", false);
        boolean present = prefs.getBoolean("present", false);
        applyState(running, present, prefs.getInt("x", -1), prefs.getInt("y", -1),
                prefs.getFloat("score", 0), prefs.getString("message", "Estado recuperado."));
    }

    private void confirmSingleTap() {
        if (!targetPresent || matchX < 0 || matchY < 0) return;
        new AlertDialog.Builder(this)
                .setTitle("Confirmar toque único")
                .setMessage("Se enviará UN solo toque a las coordenadas detectadas:\nX="
                        + matchX + ", Y=" + matchY
                        + "\n\nComprueba que el objetivo siga visible. No se repetirá automáticamente.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar y tocar una vez", (dialog, which) -> {
                    AutoClickService service = AutoClickService.instance;
                    if (service == null) {
                        status.setText("No se realizó el toque. Activa el servicio de Accesibilidad de Legionario VIP.");
                        return;
                    }
                    boolean accepted = service.tapAt(matchX, matchY, 40);
                    status.setText(accepted
                            ? "Toque único enviado. Cada toque requiere esta confirmación."
                            : "Android no aceptó el gesto. Comprueba el servicio de Accesibilidad.");
                })
                .show();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshSavedState();
    }

    @Override protected void onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(stateReceiver); } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
        super.onDestroy();
    }
}
