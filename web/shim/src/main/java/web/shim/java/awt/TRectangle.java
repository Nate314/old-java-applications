package web.shim.java.awt;

public class TRectangle implements TShape {
    public int x;
    public int y;
    public int width;
    public int height;

    public TRectangle() {
    }

    public TRectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public TRectangle(TRectangle r) {
        this(r.x, r.y, r.width, r.height);
    }

    @Override
    public TRectangle getBounds() {
        return new TRectangle(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    /** Grows this rectangle to the smallest one that also covers r. */
    public void add(TRectangle r) {
        int x1 = Math.max(x + width, r.x + r.width);
        int y1 = Math.max(y + height, r.y + r.height);
        x = Math.min(x, r.x);
        y = Math.min(y, r.y);
        width = x1 - x;
        height = y1 - y;
    }
}
