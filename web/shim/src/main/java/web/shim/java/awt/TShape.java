package web.shim.java.awt;

/** Only rectangles are shapes here; Graphics2D.draw and fill accept nothing else. */
public interface TShape {
    TRectangle getBounds();
}
