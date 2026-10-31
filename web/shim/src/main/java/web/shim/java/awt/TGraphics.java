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

    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public abstract void drawString(String str, int x, int y);

    public abstract boolean drawImage(TImage img, int x, int y, TImageObserver observer);

    public abstract boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer);

    public abstract void dispose();
}
