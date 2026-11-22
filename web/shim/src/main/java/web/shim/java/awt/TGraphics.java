package web.shim.java.awt;

import web.shim.java.awt.image.TImageObserver;

public abstract class TGraphics {
    protected TGraphics() {
    }

    public abstract TGraphics create();

    public abstract void translate(int x, int y);

    public abstract TColor getColor();

    public abstract void setColor(TColor c);

    public abstract TFont getFont();

    public abstract void setFont(TFont font);

    public TFontMetrics getFontMetrics() {
        return getFontMetrics(getFont());
    }

    public TFontMetrics getFontMetrics(TFont f) {
        return new TFontMetrics(f);
    }

    public abstract void drawLine(int x1, int y1, int x2, int y2);

    public abstract void fillRect(int x, int y, int width, int height);

    public abstract void drawRect(int x, int y, int width, int height);

    public abstract void clearRect(int x, int y, int width, int height);

    public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public abstract void drawOval(int x, int y, int width, int height);

    public abstract void fillOval(int x, int y, int width, int height);

    /** An arc of the oval in the box, from {@code startAngle} degrees (0 is 3 o'clock) through {@code arcAngle}, counterclockwise. */
    public abstract void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public void drawPolygon(TPolygon p) {
        drawPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    public void fillPolygon(TPolygon p) {
        fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /** Drawing is kept inside this rectangle (in user space now; later transforms do not move it), as in AWT. */
    public abstract void setClip(int x, int y, int width, int height);

    /** null drops the clip. */
    public abstract void setClip(TShape clip);

    public abstract TShape getClip();

    /** Narrows the clip to its overlap with this rectangle (or sets it, when there is none). */
    public abstract void clipRect(int x, int y, int width, int height);

    /** The clip's box in user space, or null with no clip. */
    public abstract TRectangle getClipBounds();

    public abstract void drawString(String str, int x, int y);

    public abstract boolean drawImage(TImage img, int x, int y, TImageObserver observer);

    public abstract boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer);

    public abstract void dispose();
}
