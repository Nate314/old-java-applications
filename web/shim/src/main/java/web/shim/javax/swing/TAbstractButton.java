package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.List;
import org.teavm.jso.JSObject;
import web.rt.Edt;
import web.rt.Js;
import web.shim.java.awt.TColor;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TFont;
import web.shim.java.awt.TFontMetrics;
import web.shim.java.awt.TImage;
import web.shim.java.awt.event.TActionEvent;
import web.shim.java.awt.event.TActionListener;

/** A Swing button rendered as a DOM button: icon then text, centered, like Metal's button layout. */
public abstract class TAbstractButton extends TJComponent implements TSwingConstants {
    /** Metal button insets: 2/14 px margin plus a 3 px border on each side. */
    private static final int PAD_X = 17;
    private static final int PAD_Y = 5;
    private static final int ICON_GAP = 4;

    private final List<TActionListener> actionListeners = new ArrayList<>();
    private String text = "";
    private TIcon icon;
    private String actionCommand;
    private boolean selected;
    private boolean contentAreaFilled = true;
    private boolean borderPainted = true;
    private JSObject iconEl;
    private JSObject textEl;

    protected TAbstractButton() {
        setOpaque(true);
    }

    @Override
    protected JSObject createElement() {
        JSObject el = Js.create("button");
        Js.setClass(el, "c button");
        Js.setAttr(el, "type", "button");
        iconEl = Js.create("span");
        Js.setClass(iconEl, "icon");
        Js.append(el, iconEl);
        textEl = Js.create("span");
        Js.setClass(textEl, "text");
        Js.append(el, textEl);
        Js.listen(el, "click", e -> {
            if (isEnabled()) {
                Edt.post(this::fireActionPerformed);
            }
        });
        return el;
    }

    @Override
    protected void syncAll() {
        super.syncAll();
        Js.setText(textEl, text);
        syncIcon();
        Js.toggleClass(element(), "no-border", !borderPainted);
    }

    private void syncIcon() {
        if (iconEl == null) {
            return;
        }
        Js.setText(iconEl, "");
        TImage image = icon instanceof TImageIcon ? ((TImageIcon) icon).getImage() : null;
        if (image != null && image.getWidth(null) > 0) {
            Js.append(iconEl, image.toCanvas());
        }
        Js.toggleClass(iconEl, "empty", image == null || image.getWidth(null) <= 0);
    }

    @Override
    protected void syncEnabled() {
        if (hasElement()) {
            Js.setDisabled(element(), !isEnabled());
        }
    }

    @Override
    protected TColor defaultBackground() {
        return CONTROL;
    }

    @Override
    protected TColor defaultForeground() {
        return CONTROL_TEXT;
    }

    @Override
    protected TFont defaultFont() {
        return CONTROL_FONT;
    }

    @Override
    public boolean isOpaque() {
        return super.isOpaque() && contentAreaFilled;
    }

    @Override
    public boolean acceptsFocus() {
        return isFocusable();
    }

    @Override
    protected TDimension computePreferredSize() {
        TFontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(text);
        int h = text.isEmpty() ? 0 : fm.getHeight();
        if (icon != null && icon.getIconWidth() > 0) {
            w += icon.getIconWidth() + (text.isEmpty() ? 0 : ICON_GAP);
            h = Math.max(h, icon.getIconHeight());
        }
        return new TDimension(w + 2 * PAD_X, h + 2 * PAD_Y);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        if (textEl != null) {
            Js.setText(textEl, this.text);
        }
        syncAccessible();
        invalidate();
    }

    @Deprecated
    public String getLabel() {
        return getText();
    }

    public TIcon getIcon() {
        return icon;
    }

    public void setIcon(TIcon icon) {
        this.icon = icon;
        syncIcon();
        syncAccessible();
        invalidate();
    }

    /**
     * Without text or a tooltip (its title), a button is named by its icon's description, as Swing's ImageIcon
     * describes itself, or, as a tile in a grid of same-size buttons (Minesweeper's first field), by its row and column.
     */
    @Override
    protected String fallbackName() {
        if (!text.trim().isEmpty() || getToolTipText() != null) { // Minesweeper's first field shows an opened 0 as " "
            return null;
        }
        if (icon instanceof TImageIcon && ((TImageIcon) icon).getDescription() != null) {
            return ((TImageIcon) icon).getDescription();
        }
        int gap = Math.min(tileGap(sibling(-1)), tileGap(sibling(1)));
        // shortcut: counts rows and columns from the container's corner, so a grid offset by a tile or more is misnumbered.
        return gap == Integer.MAX_VALUE ? null
                : "Row " + (getY() / (getHeight() + gap) + 1) + ", column " + (getX() / (getWidth() + gap) + 1);
    }

    /**
     * The gap to c when it is a same-size button next to this one in a row or a column, at most half a tile away
     * (RubiksCube2D's first stickers; the first SynthesizerKeyboard's black keys, a quarter key apart), else MAX_VALUE.
     */
    private int tileGap(TComponent c) {
        int w = getWidth();
        int h = getHeight();
        if (!(c instanceof TAbstractButton) || w <= 0 || h <= 0 || c.getWidth() != w || c.getHeight() != h) {
            return Integer.MAX_VALUE;
        }
        int gap = c.getY() == getY() ? Math.abs(c.getX() - getX()) - w : c.getX() == getX() ? Math.abs(c.getY() - getY()) - h : -1;
        return gap >= 0 && gap * 2 <= Math.min(w, h) ? gap : Integer.MAX_VALUE;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean moved = x != getX() || y != getY() || width != getWidth() || height != getHeight();
        super.setBounds(x, y, width, height);
        if (moved) {
            // A tile's name depends on its neighbours' places, and theirs on its.
            syncAccessible();
            for (int offset = -1; offset <= 1; offset += 2) {
                TComponent c = sibling(offset);
                if (c instanceof TAbstractButton) {
                    c.syncAccessible();
                }
            }
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

    public TActionListener[] getActionListeners() {
        return actionListeners.toArray(new TActionListener[0]);
    }

    public String getActionCommand() {
        return actionCommand != null ? actionCommand : text;
    }

    public void setActionCommand(String command) {
        actionCommand = command;
    }

    public void doClick() {
        fireActionPerformed();
    }

    protected void fireActionPerformed() {
        TActionEvent e = new TActionEvent(this, TActionEvent.ACTION_PERFORMED, getActionCommand());
        for (TActionListener l : new ArrayList<>(actionListeners)) {
            l.actionPerformed(e);
        }
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean b) {
        selected = b;
    }

    public void setFocusPainted(boolean b) {
    }

    /** Kept, so a button told this before it is shown (Minesweeper's painted tiles) loses its skin border too. */
    public void setBorderPainted(boolean b) {
        borderPainted = b;
        if (hasElement()) {
            Js.toggleClass(element(), "no-border", !b);
        }
    }

    public void setContentAreaFilled(boolean b) {
        contentAreaFilled = b;
        syncColors();
    }

    public boolean isBorderPainted() {
        return borderPainted;
    }

    public boolean isContentAreaFilled() {
        return contentAreaFilled;
    }
}
