package com.autoclicker.android;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;

public class AutoClickService extends AccessibilityService {
    public static AutoClickService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;
    private long intervalMs = 25, burstDelayMs = 30, pauseMs = 35, maxClicks = 0;
    private int burstCount = 1, clicksInBurst = 0;
    private long completedClicks = 0;
    private float x = 500, y = 500;

    @Override public void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { stopClicking(); }
    @Override public void onDestroy() { stopClicking(); instance = null; super.onDestroy(); }

    public void startClicking(float px, float py, long interval) {
        startClicking(px, py, interval, 1, 30, 35, 0);
    }

    public void startClicking(float px, float py, long interval, int burst,
                              long burstDelay, long pause, long limit) {
        x = px; y = py;
        intervalMs = Math.max(25, interval);
        burstCount = Math.max(1, Math.min(100, burst));
        burstDelayMs = Math.max(25, burstDelay);
        pauseMs = Math.max(0, pause);
        maxClicks = Math.max(0, limit);
        clicksInBurst = 0;
        completedClicks = 0;
        if (!running) { running = true; handler.post(clickTask); }
    }

    public void stopClicking() {
        running = false;
        handler.removeCallbacks(clickTask);
    }
    public boolean isClicking() { return running; }

    /** Ejecuta un toque individual en coordenadas de pantalla Android. */
    public boolean tapAt(float px, float py, long durationMs) {
        Path path = new Path();
        path.moveTo(px, py);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, Math.max(40, durationMs));
        return dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }
    public long getCompletedClicks() { return completedClicks; }

    public boolean writeCommandToFocusedField(String command) {
        if (command == null || command.trim().isEmpty()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        AccessibilityNodeInfo field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) {
            root.recycle();
            return false;
        }
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, command);
        boolean written = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        if (written && Build.VERSION.SDK_INT >= 30) {
            field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
        }
        field.recycle();
        root.recycle();
        return written;
    }

    private final Runnable clickTask = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (maxClicks > 0 && completedClicks >= maxClicks) {
                stopClicking();
                return;
            }
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, 40);
            boolean accepted = dispatchGesture(
                    new GestureDescription.Builder().addStroke(stroke).build(), null, null);
            if (accepted) completedClicks++;
            clicksInBurst++;
            if (clicksInBurst >= burstCount) {
                clicksInBurst = 0;
                handler.postDelayed(this, pauseMs);
            } else {
                handler.postDelayed(this, Math.max(intervalMs, burstDelayMs));
            }
        }
    };
}
