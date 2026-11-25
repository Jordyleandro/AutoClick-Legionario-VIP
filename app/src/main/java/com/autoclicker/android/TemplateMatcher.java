package com.autoclicker.android;

import android.graphics.Bitmap;

/** Comparación local por muestreo de color; no envía imágenes fuera del dispositivo. */
final class TemplateMatcher {
    static final class Result {
        final double score;
        final int x, y, width, height;
        Result(double score, int x, int y, int width, int height) {
            this.score = score;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    static Result find(Bitmap screen, Bitmap template) {
        if (screen == null || template == null
                || template.getWidth() < 12 || template.getHeight() < 12
                || template.getWidth() > screen.getWidth()
                || template.getHeight() > screen.getHeight()) {
            return new Result(0, -1, -1, 0, 0);
        }

        final int samples = 8;
        int[][] ref = new int[samples][samples];
        for (int sy = 0; sy < samples; sy++) {
            for (int sx = 0; sx < samples; sx++) {
                ref[sy][sx] = template.getPixel(
                        Math.min(template.getWidth() - 1, sx * template.getWidth() / samples),
                        Math.min(template.getHeight() - 1, sy * template.getHeight() / samples));
            }
        }

        Result best = new Result(-1, 0, 0, template.getWidth(), template.getHeight());
        double[] scales = {1.0, 0.9, 1.1};
        for (double scale : scales) {
            int w = (int) Math.round(template.getWidth() * scale);
            int h = (int) Math.round(template.getHeight() * scale);
            if (w < 12 || h < 12 || w > screen.getWidth() || h > screen.getHeight()) continue;

            int step = Math.max(6, Math.min(w, h) / 3);
            Result coarse = scan(screen, ref, samples, 0, 0,
                    screen.getWidth() - w, screen.getHeight() - h, w, h, step, best);
            if (coarse.score > best.score) best = coarse;

            int radius = step;
            Result refined = scan(screen, ref, samples,
                    Math.max(0, coarse.x - radius), Math.max(0, coarse.y - radius),
                    Math.min(screen.getWidth() - w, coarse.x + radius),
                    Math.min(screen.getHeight() - h, coarse.y + radius),
                    w, h, 1, best);
            if (refined.score > best.score) best = refined;
        }
        return best;
    }

    private static Result scan(Bitmap screen, int[][] ref, int samples,
                               int minX, int minY, int maxX, int maxY,
                               int w, int h, int step, Result best) {
        Result local = best;
        for (int top = minY; top <= maxY; top += step) {
            for (int left = minX; left <= maxX; left += step) {
                long error = 0;
                for (int sy = 0; sy < samples; sy++) {
                    for (int sx = 0; sx < samples; sx++) {
                        int px = left + Math.min(w - 1, (int) ((sx + 0.5) * w / samples));
                        int py = top + Math.min(h - 1, (int) ((sy + 0.5) * h / samples));
                        int a = screen.getPixel(px, py);
                        int b = ref[sy][sx];
                        error += Math.abs(((a >> 16) & 255) - ((b >> 16) & 255));
                        error += Math.abs(((a >> 8) & 255) - ((b >> 8) & 255));
                        error += Math.abs((a & 255) - (b & 255));
                    }
                }
                double score = 100.0 * (1.0 - error / (double) (samples * samples * 765));
                if (score > local.score) local = new Result(score, left, top, w, h);
            }
        }
        return local;
    }

    private TemplateMatcher() {}
}
