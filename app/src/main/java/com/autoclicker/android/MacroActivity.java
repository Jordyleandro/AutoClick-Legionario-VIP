package com.autoclicker.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Editor y reproductor de macros de toques locales. */
public class MacroActivity extends Activity {
    private EditText script;
    private MacroEngine engine;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        engine = new MacroEngine();
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(24, 24, 24, 24);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("LEGIONARIO VIP - MACROS");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        page.addView(title);

        TextView help = new TextView(this);
        help.setText("\\nUna acción por línea, formato X,Y,ESPERA_MS.\\nEjemplo:\\n500,500,300\\n650,500,500\\n500,650,1000\\n\\nLas coordenadas son de la pantalla Android. No son las coordenadas del EXE Windows. Usa únicamente puntos de pantalla que tengas permiso para automatizar.");
        help.setTextColor(Color.DKGRAY);
        page.addView(help);

        script = new EditText(this);
        script.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        script.setMinLines(7);
        script.setText(getPreferences(MODE_PRIVATE).getString("macro_script",
                "500,500,300\\n650,500,500\\n500,650,1000"));
        page.addView(script, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(this);
        status.setText("Macro detenida");
        page.addView(status);

        Button save = new Button(this);
        save.setText("GUARDAR MACRO");
        save.setOnClickListener(v -> {
            getPreferences(MODE_PRIVATE).edit().putString("macro_script", script.getText().toString()).apply();
            Toast.makeText(this, "Macro guardada", Toast.LENGTH_SHORT).show();
        });
        page.addView(save);

        Button start = new Button(this);
        start.setText("REPRODUCIR MACRO");
        start.setOnClickListener(v -> {
            engine.stop();
            engine.setListener(new MacroEngine.Listener() {
                @Override public void onMacroFinished() { status.setText("Macro finalizada"); }
                @Override public void onMacroStep(int current, int total) { status.setText("Paso " + current + " de " + total); }
                @Override public void onMacroError(String message) { status.setText(message); }
            });
            if (engine.load(script.getText().toString())) {
                engine.start();
                status.setText("Macro iniciada");
            } else {
                status.setText("Revisa el formato de las líneas");
            }
        });
        page.addView(start);

        Button stop = new Button(this);
        stop.setText("DETENER MACRO");
        stop.setOnClickListener(v -> { engine.stop(); status.setText("Macro detenida"); });
        page.addView(stop);

        Button back = new Button(this);
        back.setText("VOLVER");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        setContentView(scroll);
    }

    @Override protected void onDestroy() {
        if (engine != null) engine.stop();
        super.onDestroy();
    }
}
