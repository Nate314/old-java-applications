package web.shim.java.awt;

public class TColor {
    public static final TColor white = new TColor(255, 255, 255);
    public static final TColor WHITE = white;
    public static final TColor lightGray = new TColor(192, 192, 192);
    public static final TColor LIGHT_GRAY = lightGray;
    public static final TColor gray = new TColor(128, 128, 128);
    public static final TColor GRAY = gray;
    public static final TColor darkGray = new TColor(64, 64, 64);
    public static final TColor DARK_GRAY = darkGray;
    public static final TColor black = new TColor(0, 0, 0);
    public static final TColor BLACK = black;
    public static final TColor red = new TColor(255, 0, 0);
    public static final TColor RED = red;
    public static final TColor pink = new TColor(255, 175, 175);
    public static final TColor PINK = pink;
    public static final TColor orange = new TColor(255, 200, 0);
    public static final TColor ORANGE = orange;
    public static final TColor yellow = new TColor(255, 255, 0);
    public static final TColor YELLOW = yellow;
    public static final TColor green = new TColor(0, 255, 0);
    public static final TColor GREEN = green;
    public static final TColor magenta = new TColor(255, 0, 255);
    public static final TColor MAGENTA = magenta;
    public static final TColor cyan = new TColor(0, 255, 255);
    public static final TColor CYAN = cyan;
    public static final TColor blue = new TColor(0, 0, 255);
    public static final TColor BLUE = blue;

    private final int value;
    private String css;

    public TColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    public TColor(int r, int g, int b, int a) {
        if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255 || a < 0 || a > 255) {
            // Same check and message as the JDK, so apps that compute out-of-range colors fail the same way.
            throw new IllegalArgumentException("Color parameter outside of expected range:"
                    + (a < 0 || a > 255 ? " Alpha" : "") + (r < 0 || r > 255 ? " Red" : "")
                    + (g < 0 || g > 255 ? " Green" : "") + (b < 0 || b > 255 ? " Blue" : ""));
        }
        value = (a << 24) | (r << 16) | (g << 8) | b;
    }

    public TColor(int rgb) {
        value = 0xFF000000 | rgb;
    }

    public TColor(int rgba, boolean hasAlpha) {
        value = hasAlpha ? rgba : 0xFF000000 | rgba;
    }

    public TColor(float r, float g, float b) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5));
    }

    public TColor(float r, float g, float b, float a) {
        this((int) (r * 255 + 0.5), (int) (g * 255 + 0.5), (int) (b * 255 + 0.5), (int) (a * 255 + 0.5));
    }

    public static TColor getHSBColor(float h, float s, float b) {
        return new TColor(HSBtoRGB(h, s, b));
    }

    /** Same conversion as java.awt.Color.HSBtoRGB. */
    public static int HSBtoRGB(float hue, float saturation, float brightness) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0) {
            r = g = b = (int) (brightness * 255.0f + 0.5f);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0f;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0f - saturation);
            float q = brightness * (1.0f - saturation * f);
            float t = brightness * (1.0f - (saturation * (1.0f - f)));
            switch ((int) h) {
                case 0: r = (int) (brightness * 255 + 0.5f); g = (int) (t * 255 + 0.5f); b = (int) (p * 255 + 0.5f); break;
                case 1: r = (int) (q * 255 + 0.5f); g = (int) (brightness * 255 + 0.5f); b = (int) (p * 255 + 0.5f); break;
                case 2: r = (int) (p * 255 + 0.5f); g = (int) (brightness * 255 + 0.5f); b = (int) (t * 255 + 0.5f); break;
                case 3: r = (int) (p * 255 + 0.5f); g = (int) (q * 255 + 0.5f); b = (int) (brightness * 255 + 0.5f); break;
                case 4: r = (int) (t * 255 + 0.5f); g = (int) (p * 255 + 0.5f); b = (int) (brightness * 255 + 0.5f); break;
                case 5: r = (int) (brightness * 255 + 0.5f); g = (int) (p * 255 + 0.5f); b = (int) (q * 255 + 0.5f); break;
                default: break;
            }
        }
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return value & 0xFF;
    }

    public int getAlpha() {
        return (value >>> 24) & 0xFF;
    }

    public int getRGB() {
        return value;
    }

    public TColor brighter() {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int i = (int) (1.0 / 0.3);
        if (r == 0 && g == 0 && b == 0) {
            return new TColor(i, i, i, getAlpha());
        }
        if (r > 0 && r < i) {
            r = i;
        }
        if (g > 0 && g < i) {
            g = i;
        }
        if (b > 0 && b < i) {
            b = i;
        }
        return new TColor(Math.min((int) (r / 0.7), 255), Math.min((int) (g / 0.7), 255), Math.min((int) (b / 0.7), 255), getAlpha());
    }

    public TColor darker() {
        return new TColor(Math.max((int) (getRed() * 0.7), 0), Math.max((int) (getGreen() * 0.7), 0),
                Math.max((int) (getBlue() * 0.7), 0), getAlpha());
    }

    /** CSS color string, cached since paint code sets colors thousands of times per frame. */
    public String css() {
        if (css == null) {
            css = getAlpha() == 255 ? "rgb(" + getRed() + "," + getGreen() + "," + getBlue() + ")"
                    : "rgba(" + getRed() + "," + getGreen() + "," + getBlue() + "," + (getAlpha() / 255.0) + ")";
        }
        return css;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TColor && ((TColor) obj).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return "java.awt.Color[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
    }
}
