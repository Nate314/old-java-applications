package web.shim.java.awt.event;

import java.util.EventListener;

public interface TFocusListener extends EventListener {
    void focusGained(TFocusEvent e);

    void focusLost(TFocusEvent e);
}
