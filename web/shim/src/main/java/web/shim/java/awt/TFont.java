package web.shim.java.awt;

public class TFont {
    public static final int PLAIN = 0;
    public static final int BOLD = 1;
    public static final int ITALIC = 2;
    public static final String DIALOG = "Dialog";
    public static final String SANS_SERIF = "SansSerif";
    public static final String SERIF = "Serif";
    public static final String MONOSPACED = "Monospaced";

    protected String name;
    protected int style;
    protected int size;
    private String css;

    public TFont(String name, int style, int size) {
        this.name = name == null ? "Default" : name;
        this.style = (style & ~3) == 0 ? style : PLAIN;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getFontName() {
        return name;
    }

    public String getFamily() {
        return name;
    }

    public int getStyle() {
        return style;
    }

    public int getSize() {
        return size;
    }

    public float getSize2D() {
        return size;
    }

    public boolean isBold() {
        return (style & BOLD) != 0;
    }

    public boolean isItalic() {
        return (style & ITALIC) != 0;
    }

    public boolean isPlain() {
        return style == 0;
    }

    public TFont deriveFont(float newSize) {
        return new TFont(name, style, (int) newSize);
    }

    public TFont deriveFont(int newStyle) {
        return new TFont(name, newStyle, size);
    }

    public TFont deriveFont(int newStyle, float newSize) {
        return new TFont(name, newStyle, (int) newSize);
    }

    /** CSS font shorthand. Java2D font sizes are pixels at 72 dpi user space, which map to CSS px. */
    public String css() {
        if (css == null) {
            css = (isItalic() ? "italic " : "") + (isBold() ? "bold " : "") + size + "px " + cssFamily(name);
        }
        return css;
    }

    static String cssFamily(String name) {
        switch (name.toLowerCase()) {
            case "dialog":
            case "dialoginput":
            case "sansserif":
            case "default":
                return "Arial, Helvetica, sans-serif";
            case "serif":
                return "\"Times New Roman\", Times, serif";
            case "monospaced":
                return "\"Courier New\", monospace";
            case "helvetica":
                return "Helvetica, Arial, sans-serif";
            default:
                return "\"" + name + "\", Arial, sans-serif";
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TFont)) {
            return false;
        }
        TFont f = (TFont) obj;
        return f.name.equals(name) && f.style == style && f.size == size;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ style ^ size;
    }

    @Override
    public String toString() {
        return "java.awt.Font[family=" + name + ",name=" + name + ",style=" + (isBold() ? "bold" : "plain") + ",size=" + size + "]";
    }
}
