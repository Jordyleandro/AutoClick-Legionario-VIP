package com.autoclicker.android;

import android.content.Context;
import android.content.SharedPreferences;

public final class LegionarioConfig {
    private static final String FILE = "legionario_vip_config";
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    public static long interval(Context c) { return prefs(c).getLong("interval", 25); }
    public static int burst(Context c) { return prefs(c).getInt("burst", 1); }
    public static long burstDelay(Context c) { return prefs(c).getLong("burst_delay", 30); }
    public static long pause(Context c) { return prefs(c).getLong("pause", 35); }
    public static long maxClicks(Context c) { return prefs(c).getLong("max_clicks", 0); }
    public static float pointX(Context c) { return prefs(c).getFloat("point_x", 500f); }
    public static float pointY(Context c) { return prefs(c).getFloat("point_y", 500f); }
    public static long flooderInterval(Context c) { return prefs(c).getLong("flooder_interval", 2200); }
    public static boolean flooderRandom(Context c) { return prefs(c).getBoolean("flooder_random", true); }
    public static boolean flooderAutoClick(Context c) { return prefs(c).getBoolean("flooder_autoclick", true); }
    public static boolean commandEnabled(Context c, int i) { return prefs(c).getBoolean("command_" + i, i < 3); }
    public static String command(Context c, int i) {
        String[] defaults = {"/action 6", "/action 3", "/action 1", "", ""};
        return prefs(c).getString("command_text_" + i, defaults[i]);
    }
    /** Actualiza únicamente el punto de AutoClick tras confirmación del usuario. */
    public static void saveClickPoint(Context c, float x, float y) {
        prefs(c).edit()
                .putFloat("point_x", Math.max(0, x))
                .putFloat("point_y", Math.max(0, y))
                .apply();
    }

    public static void save(Context c, long interval, int burst, long burstDelay, long pause,
                            long maxClicks, float x, float y, long flooderInterval,
                            boolean random, boolean flooderAutoClick,
                            String[] commands, boolean[] enabled) {
        SharedPreferences.Editor e = prefs(c).edit();
        e.putLong("interval", Math.max(25, interval));
        e.putInt("burst", Math.max(1, Math.min(100, burst)));
        e.putLong("burst_delay", Math.max(25, burstDelay));
        e.putLong("pause", Math.max(0, pause));
        e.putLong("max_clicks", Math.max(0, maxClicks));
        e.putFloat("point_x", Math.max(0, x));
        e.putFloat("point_y", Math.max(0, y));
        e.putLong("flooder_interval", Math.max(500, flooderInterval));
        e.putBoolean("flooder_random", random);
        e.putBoolean("flooder_autoclick", flooderAutoClick);
        for (int i = 0; i < 5; i++) {
            e.putString("command_text_" + i, commands[i] == null ? "" : commands[i]);
            e.putBoolean("command_" + i, enabled[i]);
        }
        e.apply();
    }
    private LegionarioConfig() {}
}
