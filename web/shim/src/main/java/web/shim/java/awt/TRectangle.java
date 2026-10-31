package web.shim.java.awt;

public class TRectangle {
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

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }
}
