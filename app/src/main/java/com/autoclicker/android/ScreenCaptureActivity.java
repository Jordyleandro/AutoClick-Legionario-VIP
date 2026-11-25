package com.autoclicker.android;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/** Captura puntual autorizada por Android y lectura OCR local de la imagen. */
public class ScreenCaptureActivity extends Activity {
    private static final int REQUEST_CAPTURE = 7101;
    private TextView status;
    private TextView ocrOutput;
    private ImageView preview;
    private Button captureButton;
    private Intent pendingPermissionData;
    private TextRecognizer textRecognizer;
    private boolean receiverRegistered;

    private final BroadcastReceiver captureReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String path = intent.getStringExtra(ScreenCaptureService.EXTRA_FILE_PATH);
            if (path == null || path.trim().isEmpty()) {
                status.setText("No se pudo guardar la captura. Comprueba que Android haya mantenido activa la autorización.");
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap == null) {
                status.setText("La captura se guardó, pero no se pudo abrir la imagen.\n" + path);
                return;
            }
            preview.setImageBitmap(bitmap);
            status.setText("Captura guardada en el almacenamiento privado de la aplicación:\n" + path);
            runLocalOcr(bitmap);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("LEGIONARIO VIP - CAPTURA Y OCR");
        title.setTextSize(21);
        page.addView(title);

        TextView explanation = new TextView(this);
        explanation.setText(
                "\nAndroid mostrará su diálogo oficial de captura de pantalla. "
                + "La captura solo comienza si aceptas expresamente. Puedes cancelar "
                + "sin conceder el permiso.\n\n"
                + "Se realiza una captura puntual, guardada en el almacenamiento privado "
                + "de la aplicación. Después, el texto visible se analiza localmente "
                + "mediante OCR; ni la imagen ni el texto se envían a Internet.");
        page.addView(explanation);

        status = new TextView(this);
        status.setText("\nEstado: esperando autorización.");
        page.addView(status);

        captureButton = new Button(this);
        captureButton.setText("SOLICITAR PERMISO OFICIAL");
        captureButton.setOnClickListener(v -> {
            if (pendingPermissionData == null) requestCapturePermission();
            else startCaptureAndReturnToPreviousApp();
        });
        page.addView(captureButton);

        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        imageParams.topMargin = 18;
        page.addView(preview, imageParams);

        TextView ocrTitle = new TextView(this);
        ocrTitle.setText("\nTEXTO RECONOCIDO (OCR)");
        ocrTitle.setTextSize(18);
        page.addView(ocrTitle);

        ocrOutput = new TextView(this);
        ocrOutput.setText("Aquí aparecerá el texto detectado en la captura.");
        ocrOutput.setTextIsSelectable(true);
        page.addView(ocrOutput);

        Button back = new Button(this);
        back.setText("VOLVER");
        back.setOnClickListener(v -> finish());
        page.addView(back);

        setContentView(scroll);
        IntentFilter filter = new IntentFilter(ScreenCaptureService.ACTION_CAPTURE_SAVED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(captureReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(captureReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void runLocalOcr(Bitmap bitmap) {
        if (textRecognizer == null) return;
        ocrOutput.setText("Analizando texto de la captura en el dispositivo...");
        InputImage input = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(input)
                .addOnSuccessListener(result -> {
                    if (isFinishing() || isDestroyed()) return;
                    String recognized = result.getText();
                    if (recognized == null || recognized.trim().isEmpty()) {
                        ocrOutput.setText("No se detectó texto legible en esta captura.");
                    } else {
                        ocrOutput.setText(recognized);
                    }
                })
                .addOnFailureListener(error -> {
                    if (isFinishing() || isDestroyed()) return;
                    ocrOutput.setText("No se pudo completar el OCR: "
                            + (error.getLocalizedMessage() == null ? "error de reconocimiento" : error.getLocalizedMessage()));
                });
    }

    private void requestCapturePermission() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            status.setText("MediaProjection no está disponible en este dispositivo.");
            return;
        }
        status.setText("Esperando tu decisión en el diálogo oficial de Android...");
        startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE) return;
        if (resultCode != RESULT_OK || data == null) {
            pendingPermissionData = null;
            status.setText("Permiso cancelado. No se realizó ninguna captura.");
            return;
        }
        pendingPermissionData = data;
        status.setText("Permiso concedido. Pulsa el botón para enviar Legionario VIP al segundo plano y capturar la aplicación que estaba abierta antes.");
        captureButton.setText("CAPTURAR PANTALLA ANTERIOR");
    }

    private void startCaptureAndReturnToPreviousApp() {
        Intent service = new Intent(this, ScreenCaptureService.class);
        service.putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, RESULT_OK);
        service.putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, pendingPermissionData);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
        pendingPermissionData = null;
        status.setText("Captura en curso. La notificación de Android indicará cuándo termina.");
        moveTaskToBack(true);
    }

    @Override protected void onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(captureReceiver); } catch (IllegalArgumentException ignored) {}
            receiverRegistered = false;
        }
        if (textRecognizer != null) {
            textRecognizer.close();
            textRecognizer = null;
        }
        super.onDestroy();
    }
}
