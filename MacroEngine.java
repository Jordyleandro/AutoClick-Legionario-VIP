package com.autoclicker.android;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/**
 * Reproductor básico de macros de toques.
 * Formato de cada línea: X,Y,ESPERA_MS
 * Las coordenadas son de pantalla Android y deben calibrarse manualmente.
 */
public class MacroEngine {
    public interface Listener {
        void onMacroFinished();
        void onMacroStep(int current, int total);
        void onMacroError(String message);
    }

    private static class Step {
        final float x, y;
        final long wait;
        Step(float x, float y, long wait) { this.x = x; this.y = y; this.wait = wait; }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Step> steps = new ArrayList<>();
    private Listener listener;
    private int index;
    private boolean running;

    public void setListener(Listener l) { listener = l; }

    public boolean load(String script) {
        steps.clear();
        if (script == null) return false;
        String[] lines = script.split("\\r?\\n");
        try {
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] parts = trimmed.split(",");
                if (parts.length != 3) throw new IllegalArgumentException("Formato esperado: X,Y,ESPERA_MS");
                float x = Float.parseFloat(parts[0].trim());
                float y = Float.parseFloat(parts[1].trim());
                long wait = Long.parseLong(parts[2].trim());
                if (x < 0 || y < 0 || wait < 0) throw new IllegalArgumentException("No se permiten valores negativos");
                steps.add(new Step(x, y, wait));
            }
        } catch (Exception e) {
            steps.clear();
            if (listener != null) listener.onMacroError(e.getMessage());
            return false;
        }
        return !steps.isEmpty();
    }

    public void start() {
        if (running || steps.isEmpty()) return;
        if (AutoClickService.instance == null) {
            if (listener != null) listener.onMacroError("Activa el servicio de Accesibilidad primero");
            return;
        }
        running = true;
        index = 0;
        handler.post(runStep);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(runStep);
    }

    public boolean isRunning() { return running; }

    private final Runnable runStep = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (index >= steps.size()) {
                running = false;
                if (listener != null) listener.onMacroFinished();
                return;
            }
            Step step = steps.get(index);
            AutoClickService service = AutoClickService.instance;
            if (service == null) {
                running = false;
                if (listener != null) listener.onMacroError("Servicio de Accesibilidad desconectado");
                return;
            }
            service.tapAt(step.x, step.y, 40);
            index++;
            if (listener != null) listener.onMacroStep(index, steps.size());
            handler.postDelayed(this, step.wait);
        }
    };
}
