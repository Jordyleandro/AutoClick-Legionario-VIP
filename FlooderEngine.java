package com.autoclicker.android;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FlooderEngine {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private boolean running;
    private long intervalMs = 2200;
    private boolean randomOrder = true;
    private final List<String> commands = new ArrayList<>();
    private int nextIndex = 0;

    public void configure(long interval, boolean random, List<String> selectedCommands) {
        intervalMs = Math.max(500, interval);
        randomOrder = random;
        commands.clear();
        if (selectedCommands != null) {
            for (String c : selectedCommands) if (c != null && !c.trim().isEmpty()) commands.add(c.trim());
        }
        nextIndex = 0;
    }
    public void start() { if (!running && !commands.isEmpty()) { running = true; handler.post(task); } }
    public void stop() { running = false; handler.removeCallbacks(task); }
    public boolean isRunning() { return running; }

    private final Runnable task = new Runnable() {
        @Override public void run() {
            if (!running || commands.isEmpty()) return;
            String command = commands.get(randomOrder ? random.nextInt(commands.size())
                    : (nextIndex++ % commands.size()));
            if (AutoClickService.instance != null) AutoClickService.instance.writeCommandToFocusedField(command);
            handler.postDelayed(this, intervalMs);
        }
    };
}
