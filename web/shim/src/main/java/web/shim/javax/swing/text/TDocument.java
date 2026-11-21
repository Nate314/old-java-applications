package web.shim.javax.swing.text;

import web.shim.javax.swing.event.TDocumentListener;

/** The part of a text field's document apps use: its length and its listeners. */
public interface TDocument {
    int getLength();

    void addDocumentListener(TDocumentListener l);

    void removeDocumentListener(TDocumentListener l);
}
