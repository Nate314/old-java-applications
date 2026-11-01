package web.shim.java.awt.event;

import java.util.EventListener;

public interface TKeyListener extends EventListener {
    void keyTyped(TKeyEvent e);

    void keyPressed(TKeyEvent e);

    void keyReleased(TKeyEvent e);
}
