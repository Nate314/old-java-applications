package web.shim.java.awt;

import java.util.HashMap;

/** Hints are accepted and ignored: canvas drawing is always antialiased. */
public class TRenderingHints extends HashMap<Object, Object> {
    public static class Key {
        private final String name;

        protected Key(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final Key KEY_ANTIALIASING = new Key("Global antialiasing enable key");
    public static final Object VALUE_ANTIALIAS_ON = "Antialiased rendering mode";
    public static final Object VALUE_ANTIALIAS_OFF = "Nonantialiased rendering mode";
    public static final Object VALUE_ANTIALIAS_DEFAULT = "Default antialiasing rendering mode";
    public static final Key KEY_TEXT_ANTIALIASING = new Key("Text-specific antialiasing enable key");
    public static final Object VALUE_TEXT_ANTIALIAS_ON = "Antialiased text mode";
    public static final Object VALUE_TEXT_ANTIALIAS_OFF = "Nonantialiased text mode";
    public static final Object VALUE_TEXT_ANTIALIAS_DEFAULT = "Default antialiasing text mode";
    public static final Key KEY_RENDERING = new Key("Global rendering quality key");
    public static final Object VALUE_RENDER_QUALITY = "Highest quality rendering methods";
    public static final Object VALUE_RENDER_SPEED = "Fastest rendering methods";
    public static final Object VALUE_RENDER_DEFAULT = "Default rendering methods";
    public static final Key KEY_STROKE_CONTROL = new Key("Stroke normalization control key");
    public static final Object VALUE_STROKE_PURE = "Pure stroke conversion for accurate paths";
    public static final Object VALUE_STROKE_NORMALIZE = "Normalize strokes for consistent rendering";
    public static final Object VALUE_STROKE_DEFAULT = "Default stroke normalization";
    public static final Key KEY_INTERPOLATION = new Key("Image interpolation method key");
    public static final Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR = "Nearest Neighbor image interpolation mode";
    public static final Object VALUE_INTERPOLATION_BILINEAR = "Bilinear image interpolation mode";
    public static final Object VALUE_INTERPOLATION_BICUBIC = "Bicubic image interpolation mode";

    public TRenderingHints(Key key, Object value) {
        if (key != null) {
            put(key, value);
        }
    }
}
