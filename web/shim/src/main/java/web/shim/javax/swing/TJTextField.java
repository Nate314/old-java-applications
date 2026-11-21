package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.rt.Edt;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TFontMetrics;
import web.shim.java.awt.event.TActionEvent;
import web.shim.java.awt.event.TActionListener;
import web.shim.java.awt.event.TKeyEvent;
import web.shim.javax.swing.event.TDocumentEvent;
import web.shim.javax.swing.event.TDocumentListener;
import web.shim.javax.swing.text.TDocument;

/** A DOM text input. The DOM holds the text once the element exists; editing is native. */
public class TJTextField extends TJComponent {
    /** Metal text field border insets (2 px) plus margin. */
    private static final int PAD = 3;

    private final List<TActionListener> actionListeners = new ArrayList<>();
    private final List<TDocumentListener> documentListeners = new ArrayList<>();
    private final TDocument document = new TDocument() {
        @Override
        public int getLength() {
            return getText().length();
        }

        @Override
        public void addDocumentListener(TDocumentListener l) {
            if (l != null) {
                documentListeners.add(l);
            }
        }

        @Override
        public void removeDocumentListener(TDocumentListener l) {
            documentListeners.remove(l);
        }
    };
    /** The text listeners were last told about; the DOM may be ahead of it until its input event. */
    private String text;
    private int columns;
    private boolean editable = true;

    public TJTextField() {
        this("", 0);
    }

    public TJTextField(String text) {
        this(text, 0);
    }

    public TJTextField(int columns) {
        this("", columns);
    }

    public TJTextField(String text, int columns) {
        this.text = text == null ? "" : text;
        this.columns = columns;
        setOpaque(true);
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("input");
        Js.setClass(el, "c textfield");
        Js.setAttr(el, "type", "text");
        Js.setAttr(el, "spellcheck", "false");
        Js.setAttr(el, "autocomplete", "off");
        Js.setValue(el, text);
        // Every edit the browser makes (keys, paste or cut with the mouse, the phone keyboard, autofill) reaches the document.
        Js.listen(el, "input", e -> {
            TextChange change = TextChange.between(text, Js.getValue(el));
            text = Js.getValue(el);
            if (change != null) {
                Edt.post(() -> fireChange(change.offset, change.removed, change.inserted));
            }
        });
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        Js.setDisabled(element(), !isEnabled());
        if (editable) {
            Js.removeAttr(element(), "readonly");
        } else {
            Js.setAttr(element(), "readonly", "");
        }
    }

    @Override
    protected void syncEnabled() {
        if (hasElement()) {
            Js.setDisabled(element(), !isEnabled());
        }
    }

    @Override
    protected TColor defaultBackground() {
        return TEXT_BACKGROUND;
    }

    @Override
    protected TColor defaultForeground() {
        return TEXT_FOREGROUND;
    }

    @Override
    protected TFont defaultFont() {
        return TEXT_FONT;
    }

    @Override
    public boolean acceptsFocus() {
        return isFocusable();
    }

    @Override
    protected void focusGained() {
        Js.focus(element());
    }

    @Override
    protected TDimension computePreferredSize() {
        TFontMetrics fm = getFontMetrics(getFont());
        int w = columns > 0 ? columns * fm.charWidth('m') : fm.stringWidth(getText()) + 1;
        return new TDimension(w + 2 * PAD, fm.getHeight() + 2 * PAD);
    }

    @Override
    protected void processKeyEvent(TKeyEvent e) {
        super.processKeyEvent(e);
        if (e.getID() == TKeyEvent.KEY_PRESSED && e.getKeyCode() == 10) {
            TActionEvent action = new TActionEvent(this, TActionEvent.ACTION_PERFORMED, getText());
            for (TActionListener l : new ArrayList<>(actionListeners)) {
                l.actionPerformed(action);
            }
        }
    }

    public String getText() {
        return hasElement() ? Js.getValue(element()) : text;
    }

    public void setText(String t) {
        int old = getText().length();
        text = t == null ? "" : t;
        if (hasElement()) {
            Js.setValue(element(), text);
        }
        // As in Swing: the old text is removed, then the new one inserted, each heard only when not empty.
        fireChange(0, old, text.length());
    }

    public TDocument getDocument() {
        return document;
    }

    private void fireChange(int offset, int removed, int inserted) {
        if (removed > 0) {
            fireDocument(offset, removed, false);
        }
        if (inserted > 0) {
            fireDocument(offset, inserted, true);
        }
    }

    private void fireDocument(int offset, int length, boolean insert) {
        TDocumentEvent e = new TDocumentEvent() {
            @Override
            public TDocument getDocument() {
                return document;
            }

            @Override
            public int getOffset() {
                return offset;
            }

            @Override
            public int getLength() {
                return length;
            }
        };
        for (TDocumentListener l : new ArrayList<>(documentListeners)) {
            if (insert) {
                l.insertUpdate(e);
            } else {
                l.removeUpdate(e);
            }
        }
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
        invalidate();
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean b) {
        editable = b;
        if (hasElement()) {
            syncAll();
        }
    }

    public void addActionListener(TActionListener l) {
        if (l != null) {
            actionListeners.add(l);
        }
    }

    public void removeActionListener(TActionListener l) {
        actionListeners.remove(l);
    }

}
