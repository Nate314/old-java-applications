package web.shim.javax.swing.event;

import web.shim.javax.swing.text.TDocument;

public interface TDocumentEvent {
    TDocument getDocument();

    int getOffset();

    int getLength();
}
