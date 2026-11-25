package com.autoclicker.android;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class ConfigActivity extends Activity {
    private EditText interval, burst, burstDelay, pause, maxClicks, pointX, pointY, floodInterval;
    private EditText[] commands = new EditText[5];
    private CheckBox[] enabled = new CheckBox[5];
    private CheckBox randomOrder, floodAutoClick;

    private EditText numberField(LinearLayout parent, String label, String value) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.BLACK);
        parent.addView(t);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setInputType( android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL );
        e.setText(value);
        parent.addView(e, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return e;
    }

    private long getLong(EditText e, long fallback) {
        try { return (long) Double.parseDouble(e.getText().toString().trim()); }
        catch (Exception ignored) { return fallback; }
    }
    private float getFloat(EditText e, float fallback) {
        try { return Float.parseFloat(e.getText().toString().trim()); }
        catch (Exception ignored) { return fallback; }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 24, 28, 24);
        scroll.addView(page);

        TextView title = new TextView(this);
        title.setText("LEGIONARIO VIP - CONFIGURACIÓN");
        title.setTextSize(21);
        title.setTextColor(Color.BLACK);
        page.addView(title);

        interval = numberField(page, "INTERVAL_MS (mínimo Android: 25)", "" + LegionarioConfig.interval(this));
        burst = numberField(page, "BURST (cantidad de clics por ráfaga)", "" + LegionarioConfig.burst(this));
        burstDelay = numberField(page, "BURST_DELAY_MS", "" + LegionarioConfig.burstDelay(this));
        pause = numberField(page, "PAUSE_MS", "" + LegionarioConfig.pause(this));
        maxClicks = numberField(page, "MAX_CLICKS (0 = ilimitado)", "" + LegionarioConfig.maxClicks(this));
        pointX = numberField(page, "SETPOINT_X Android", "" + LegionarioConfig.pointX(this));
        pointY = numberField(page, "SETPOINT_Y Android", "" + LegionarioConfig.pointY(this));

        TextView floodTitle = new TextView(this);
        floodTitle.setText("\\nFLOODER");
        floodTitle.setTextSize(18);
        floodTitle.setTextColor(Color.BLACK);
        page.addView(floodTitle);
        floodInterval = numberField(page, "FLOODER_INTERVAL_MS", "" + LegionarioConfig.flooderInterval(this));
        randomOrder = new CheckBox(this);
        randomOrder.setText("FLOODER_RANDOM");
        randomOrder.setChecked(LegionarioConfig.flooderRandom(this));
        page.addView(randomOrder);
        floodAutoClick = new CheckBox(this);
        floodAutoClick.setText("FLOODER_AUTOCLICK (mantener AutoClick al iniciar Flooder)");
        floodAutoClick.setChecked(LegionarioConfig.flooderAutoClick(this));
        page.addView(floodAutoClick);

        TextView cmdTitle = new TextView(this);
        cmdTitle.setText("\\nCOMANDOS");
        cmdTitle.setTextSize(18);
        cmdTitle.setTextColor(Color.BLACK);
        page.addView(cmdTitle);
        for (int i = 0; i < 5; i++) {
            enabled[i] = new CheckBox(this);
            enabled[i].setText("Activar comando " + (i + 1));
            enabled[i].setChecked(LegionarioConfig.commandEnabled(this, i));
            page.addView(enabled[i]);
            commands[i] = new EditText(this);
            commands[i].setSingleLine(true);
            commands[i].setHint("Comando " + (i + 1));
            commands[i].setText(LegionarioConfig.command(this, i));
            page.addView(commands[i]);
        }

        Button save = new Button(this);
        save.setText("GUARDAR CONFIGURACIÓN");
        save.setOnClickListener(v -> saveConfig());
        page.addView(save);
        Button back = new Button(this);
        back.setText("VOLVER");
        back.setOnClickListener(v -> finish());
        page.addView(back);
        setContentView(scroll);
    }

    private void saveConfig() {
        String[] cmd = new String[5];
        boolean[] on = new boolean[5];
        for (int i = 0; i < 5; i++) {
            cmd[i] = commands[i].getText().toString();
            on[i] = enabled[i].isChecked();
        }
        LegionarioConfig.save(this,
                getLong(interval, 25),
                (int) getLong(burst, 1),
                getLong(burstDelay, 30),
                getLong(pause, 35),
                getLong(maxClicks, 0),
                getFloat(pointX, 500),
                getFloat(pointY, 500),
                getLong(floodInterval, 2200),
                randomOrder.isChecked(),
                floodAutoClick.isChecked(),
                cmd, on);
        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
        finish();
    }
}
