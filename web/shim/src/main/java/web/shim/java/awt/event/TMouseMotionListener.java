package web.shim.java.awt.event;

import java.util.EventListener;

public interface TMouseMotionListener extends EventListener {
    void mouseDragged(TMouseEvent e);

    void mouseMoved(TMouseEvent e);
}
