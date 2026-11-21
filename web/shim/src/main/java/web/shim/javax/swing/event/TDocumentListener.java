package web.shim.javax.swing.event;

import java.util.EventListener;

public interface TDocumentListener extends EventListener {
    void insertUpdate(TDocumentEvent e);

    void removeUpdate(TDocumentEvent e);

    void changedUpdate(TDocumentEvent e);
}
