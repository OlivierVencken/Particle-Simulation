package io.github.simulation.util;

public final class PaletteUtil {

    public static float[][] generateEvenHue(int groups) {
        return generateEvenHue(groups, 0.82f, 1.0f, true);
    }

    public static float[][] generateEvenHue(int groups, float sat, float val, boolean alternateBrightness) {
        float[][] rgba = new float[groups][4];
        for (int i = 0; i < groups; i++) {
            float h = (i / (float) groups);           // [0,1) around the hue circle
            float v = val;
            if (alternateBrightness && groups > 8) {
                v = (i % 2 == 0) ? val : Math.max(0.7f, val - 0.25f);
            }
            float[] rgb = hsvToRgb(h, sat, v);
            rgba[i][0] = rgb[0];
            rgba[i][1] = rgb[1];
            rgba[i][2] = rgb[2];
            rgba[i][3] = 1.0f;
        }
        return rgba;
    }

    public static float[][] generateGoldenHue(int groups) {
        float[][] rgba = new float[groups][4];
        float hue = 0.0f;
        final float golden = 0.61803398875f;
        for (int i = 0; i < groups; i++) {
            hue = (hue + golden) % 1.0f;
            float[] rgb = hsvToRgb(hue, 0.82f, (i % 2 == 0) ? 1.0f : 0.78f);
            rgba[i][0] = rgb[0];
            rgba[i][1] = rgb[1];
            rgba[i][2] = rgb[2];
            rgba[i][3] = 1.0f;
        }
        return rgba;
    }

    // HSV -> RGB [0,1]
    private static float[] hsvToRgb(float h, float s, float v) {
        float r, g, b;
        float i = (float) Math.floor(h * 6.0f);
        float f = h * 6.0f - i;
        float p = v * (1.0f - s);
        float q = v * (1.0f - f * s);
        float t = v * (1.0f - (1.0f - f) * s);
        switch (((int) i) % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return new float[] { r, g, b };
    }
}