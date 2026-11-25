package com.autoclicker.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.Locale;

/** Comparación local de una plantilla elegida por el usuario contra una captura elegida. */
public class ImageRecognitionActivity extends Activity {
    private static final int PICK_SCREEN = 8201;
    private static final int PICK_TEMPLATE = 8202;
    private Bitmap screenBitmap;
    private Bitmap templateBitmap;
    private TextView status;
    private ImageView screenPreview;
    private ImageView templatePreview;
    private EditText roiX, roiY, roiW, roiH;
    private SeekBar thresholdBar;
    private TextView thresholdLabel;
    private Button findButton;
    private Button useMatchButton;
    private Button testTapButton;
    private boolean searching = false;
    private int matchCenterX = -1, matchCenterY = -1;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("RECONOCIMIENTO VISUAL POR PLANTILLA");
        title.setTextSize(20);
        root.addView(title);
        TextView help = new TextView(this);
        help.setText("\nSelecciona una captura de pantalla y una imagen pequeña de referencia (por ejemplo, un icono o botón). Legionario VIP buscará la plantilla dentro de la captura usando comparación de píxeles, completamente en el dispositivo. Para mejores resultados usa imágenes nítidas, sin cambios de escala ni animaciones.");
        root.addView(help);

        Button pickScreen = new Button(this);
        pickScreen.setText("1. ELEGIR CAPTURA DE PANTALLA");
        pickScreen.setOnClickListener(v -> chooseImage(PICK_SCREEN));
        root.addView(pickScreen);
        screenPreview = new ImageView(this);
        screenPreview.setAdjustViewBounds(true);
        root.addView(screenPreview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 420));

        Button pickTemplate = new Button(this);
        pickTemplate.setText("2. ELEGIR IMAGEN DE REFERENCIA");
        pickTemplate.setOnClickListener(v -> chooseImage(PICK_TEMPLATE));
        root.addView(pickTemplate);
        templatePreview = new ImageView(this);
        templatePreview.setAdjustViewBounds(true);
        root.addView(templatePreview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 240));

        TextView roiTitle = new TextView(this);
        roiTitle.setText("\n3. REGIÓN DE BÚSQUEDA (PIXELES)");
        roiTitle.setTextSize(16);
        root.addView(roiTitle);
        TextView roiHelp = new TextView(this);
        roiHelp.setText("Limita el análisis a una zona de la captura. X/Y indican la esquina superior izquierda; ancho/alto definen el rectángulo. Deja la región completa para buscar en toda la imagen.");
        root.addView(roiHelp);
        LinearLayout roiRow1 = new LinearLayout(this);
        roiRow1.setOrientation(LinearLayout.HORIZONTAL);
        roiX = numberField("X", "0");
        roiY = numberField("Y", "0");
        roiRow1.addView(roiX, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        roiRow1.addView(roiY, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(roiRow1);
        LinearLayout roiRow2 = new LinearLayout(this);
        roiRow2.setOrientation(LinearLayout.HORIZONTAL);
        roiW = numberField("Ancho", "0");
        roiH = numberField("Alto", "0");
        roiRow2.addView(roiW, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        roiRow2.addView(roiH, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(roiRow2);

        TextView thresholdTitle = new TextView(this);
        thresholdTitle.setText("\n4. UMBRAL MÍNIMO DE SIMILITUD");
        root.addView(thresholdTitle);
        thresholdLabel = new TextView(this);
        thresholdLabel.setText("82%");
        root.addView(thresholdLabel);
        thresholdBar = new SeekBar(this);
        thresholdBar.setMax(49);
        thresholdBar.setProgress(32);
        thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                thresholdLabel.setText((50 + progress) + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        root.addView(thresholdBar);

        findButton = new Button(this);
        findButton.setText("5. BUSCAR IMAGEN EN LA CAPTURA");
        findButton.setOnClickListener(v -> findTemplate());
        root.addView(findButton);
        status = new TextView(this);
        status.setText("\nEsperando las dos imágenes.");
        status.setTextIsSelectable(true);
        root.addView(status);
        useMatchButton = new Button(this);
        useMatchButton.setText("GUARDAR CENTRO COMO PUNTO AUTOCLICK");
        useMatchButton.setEnabled(false);
        useMatchButton.setOnClickListener(v -> confirmUseMatch());
        root.addView(useMatchButton);
        testTapButton = new Button(this);
        testTapButton.setText("EJECUTAR UN TOQUE DE PRUEBA (CON CONFIRMACIÓN)");
        testTapButton.setEnabled(false);
        testTapButton.setOnClickListener(v -> confirmTestTap());
        root.addView(testTapButton);
        Button back = new Button(this);
        back.setText("VOLVER");
        back.setOnClickListener(v -> finish());
        root.addView(back);
        setContentView(scroll);
    }

    private void chooseImage(int request) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, request);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = getContentResolver().openInputStream(data.getData())) {
            Bitmap decoded = BitmapFactory.decodeStream(in);
            if (decoded == null) throw new IllegalArgumentException("Formato de imagen no compatible");
            if (requestCode == PICK_SCREEN) {
                if (screenBitmap != null) screenBitmap.recycle();
                screenBitmap = decoded;
                screenPreview.setImageBitmap(screenBitmap);
                matchCenterX = matchCenterY = -1;
                useMatchButton.setEnabled(false);
                testTapButton.setEnabled(false);
                roiX.setText("0");
                roiY.setText("0");
                roiW.setText(String.valueOf(screenBitmap.getWidth()));
                roiH.setText(String.valueOf(screenBitmap.getHeight()));
                status.setText("Captura cargada: " + screenBitmap.getWidth() + " × " + screenBitmap.getHeight()
                        + "\nLa región de búsqueda se ha ajustado a toda la captura.");
            } else if (requestCode == PICK_TEMPLATE) {
                if (templateBitmap != null) templateBitmap.recycle();
                templateBitmap = decoded;
                templatePreview.setImageBitmap(templateBitmap);
                matchCenterX = matchCenterY = -1;
                useMatchButton.setEnabled(false);
                testTapButton.setEnabled(false);
                status.setText("Plantilla cargada: " + templateBitmap.getWidth() + " × " + templateBitmap.getHeight());
            }
        } catch (Exception e) {
            status.setText("No se pudo abrir la imagen: " + e.getMessage());
        }
    }

    private EditText numberField(String hint, String value) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        field.setHint(hint);
        field.setText(value);
        return field;
    }

    private int readNumber(EditText field, int fallback) {
        try { return Integer.parseInt(field.getText().toString().trim()); }
        catch (Exception ignored) { return fallback; }
    }

    private void findTemplate() {
        if (searching) return;
        if (screenBitmap == null || templateBitmap == null) {
            status.setText("Primero debes elegir la captura y la imagen de referencia.");
            return;
        }
        if (templateBitmap.getWidth() > screenBitmap.getWidth() || templateBitmap.getHeight() > screenBitmap.getHeight()) {
            status.setText("La plantilla debe ser más pequeña que la captura.");
            return;
        }

        final Bitmap screen = screenBitmap;
        final Bitmap template = templateBitmap;
        final int x = Math.max(0, readNumber(roiX, 0));
        final int y = Math.max(0, readNumber(roiY, 0));
        final int width = readNumber(roiW, screen.getWidth());
        final int height = readNumber(roiH, screen.getHeight());
        final int threshold = 50 + thresholdBar.getProgress();
        if (width <= 0 || height <= 0 || x >= screen.getWidth() || y >= screen.getHeight()
                || width > screen.getWidth() - x || height > screen.getHeight() - y) {
            status.setText("La región no es válida. Comprueba X, Y, ancho y alto dentro de los límites de la captura.");
            return;
        }
        if (width < template.getWidth() || height < template.getHeight()) {
            status.setText("La región seleccionada debe ser al menos tan grande como la plantilla.");
            return;
        }

        searching = true;
        findButton.setEnabled(false);
        status.setText("Buscando dentro de la región seleccionada... El análisis se ejecuta localmente.");
        new Thread(() -> {
            Match match = match(screen, template, x, y, width, height);
            runOnUiThread(() -> {
                searching = false;
                findButton.setEnabled(true);
                if (match.score >= threshold) {
                    matchCenterX = match.x + template.getWidth() / 2;
                    matchCenterY = match.y + template.getHeight() / 2;
                    useMatchButton.setEnabled(true);
                    testTapButton.setEnabled(false);
                    status.setText(String.format(Locale.getDefault(),
                            "COINCIDENCIA ENCONTRADA\nSimilitud: %.1f%% (umbral: %d%%)\nPosición superior izquierda: X=%d, Y=%d\nTamaño de plantilla: %d × %d px\nRegión analizada: X=%d, Y=%d, %d × %d px\n\nConfirma visualmente el resultado antes de usar esas coordenadas. La similitud no garantiza que sea el botón correcto.",
                            match.score, threshold, match.x, match.y, template.getWidth(), template.getHeight(),
                            x, y, width, height));
                } else {
                    matchCenterX = matchCenterY = -1;
                    useMatchButton.setEnabled(false);
                    testTapButton.setEnabled(false);
                    status.setText(String.format(Locale.getDefault(),
                            "Sin coincidencia por encima del umbral. Mejor candidato: %.1f%%; umbral requerido: %d%%. Prueba ajustar la región o usar una plantilla más nítida.",
                            match.score, threshold));
                }
            });
        }, "Legionario-ImageMatch").start();
    }

    private void confirmUseMatch() {
        if (matchCenterX < 0 || matchCenterY < 0) return;
        new AlertDialog.Builder(this)
                .setTitle("Confirmar punto de AutoClick")
                .setMessage("Se guardará el centro de la coincidencia como punto configurado de AutoClick:\nX="
                        + matchCenterX + ", Y=" + matchCenterY
                        + "\n\nEsto NO inicia los clics. Revisa que las coordenadas de la captura correspondan a la pantalla actual.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar punto", (dialog, which) -> {
                    LegionarioConfig.saveClickPoint(this, matchCenterX, matchCenterY);
                    testTapButton.setEnabled(true);
                    status.setText(status.getText() + "\n\nPunto guardado en configuración: X="
                            + matchCenterX + ", Y=" + matchCenterY
                            + ". AutoClick permanece detenido. Puedes ejecutar un único toque de prueba con confirmación.");
                })
                .show();
    }

    /** Ejecuta solo un toque, nunca inicia el modo repetitivo de AutoClick. */
    private void confirmTestTap() {
        if (matchCenterX < 0 || matchCenterY < 0) return;
        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        if (matchCenterX >= metrics.widthPixels || matchCenterY >= metrics.heightPixels) {
            status.setText("No se ejecutó el toque: el punto detectado queda fuera de la pantalla actual. "
                    + "La captura puede tener otra resolución u orientación.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Confirmar toque único")
                .setMessage("Legionario ejecutará UN solo toque en la pantalla actual:\nX="
                        + matchCenterX + ", Y=" + matchCenterY
                        + "\n\nVerifica que la aplicación de destino esté visible. No se iniciará AutoClick repetitivo.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ejecutar un toque", (dialog, which) -> {
                    AutoClickService service = AutoClickService.instance;
                    if (service == null) {
                        status.setText("No se ejecutó: activa el servicio de Accesibilidad de Legionario VIP en los ajustes de Android.");
                        return;
                    }
                    boolean accepted = service.tapAt(matchCenterX, matchCenterY, 40);
                    status.setText(accepted
                            ? "Toque único enviado a X=" + matchCenterX + ", Y=" + matchCenterY + "."
                            : "Android no aceptó el gesto. Comprueba que el servicio de Accesibilidad esté activo.");
                })
                .show();
    }

    // Búsqueda gruesa seguida de refinamiento local para reducir trabajo y mejorar coordenadas.
    private Match match(Bitmap screen, Bitmap template, int roiX, int roiY, int roiW, int roiH) {
        final int samples = 20;
        int[][] reference = new int[samples][samples];
        for (int sy = 0; sy < samples; sy++) for (int sx = 0; sx < samples; sx++) {
            reference[sy][sx] = template.getPixel(
                    Math.min(template.getWidth()-1, sx*template.getWidth()/samples),
                    Math.min(template.getHeight()-1, sy*template.getHeight()/samples));
        }

        Match best = new Match(-1, roiX, roiY);
        double[] scales = {1.0, 0.9, 1.1};
        for (double scale : scales) {
            int w = (int)Math.round(template.getWidth()*scale);
            int h = (int)Math.round(template.getHeight()*scale);
            if (w < 12 || h < 12 || w > roiW || h > roiH) continue;
            int coarseStep = Math.max(2, Math.min(w, h)/18);
            Match coarse = scan(screen, reference, samples, roiX, roiY, roiX+roiW-w,
                    roiY+roiH-h, w, h, coarseStep, best);
            if (coarse.score > best.score) best = coarse;
            if (coarse.score >= 0) {
                int radius = coarseStep;
                Match refined = scan(screen, reference, samples,
                        Math.max(roiX, coarse.x-radius), Math.max(roiY, coarse.y-radius),
                        Math.min(roiX+roiW-w, coarse.x+radius),
                        Math.min(roiY+roiH-h, coarse.y+radius),
                        w, h, 1, best);
                if (refined.score > best.score) best = refined;
            }
        }
        return best;
    }

    private Match scan(Bitmap screen, int[][] reference, int samples, int minX, int minY,
                       int maxX, int maxY, int w, int h, int step, Match best) {
        Match localBest = best;
        for (int top = minY; top <= maxY; top += step) {
            for (int left = minX; left <= maxX; left += step) {
                long error = 0;
                for (int sy = 0; sy < samples; sy++) for (int sx = 0; sx < samples; sx++) {
                    int px = left + Math.min(w-1, (int)((sx+0.5)*w/samples));
                    int py = top + Math.min(h-1, (int)((sy+0.5)*h/samples));
                    int a = screen.getPixel(px, py), b = reference[sy][sx];
                    error += Math.abs(((a>>16)&255)-((b>>16)&255));
                    error += Math.abs(((a>>8)&255)-((b>>8)&255));
                    error += Math.abs((a&255)-(b&255));
                }
                double score = 100.0 * (1.0 - error/(double)(samples*samples*765));
                if (score > localBest.score) localBest = new Match(score, left, top);
            }
        }
        return localBest;
    }

    private static class Match {
        double score; int x, y;
        Match(double score, int x, int y) { this.score=score; this.x=x; this.y=y; }
    }

    @Override protected void onDestroy() {
        if (screenBitmap != null && !screenBitmap.isRecycled()) screenBitmap.recycle();
        if (templateBitmap != null && !templateBitmap.isRecycled()) templateBitmap.recycle();
        super.onDestroy();
    }
}
