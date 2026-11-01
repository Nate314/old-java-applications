package web.shim.javax.swing;

import java.util.HashMap;
import java.util.Map;
import web.shim.java.awt.TComponent;

/** Null-layout container whose children are ordered by layer (higher on top), then by position in the layer. */
public class TJLayeredPane extends TJComponent {
    public static final Integer DEFAULT_LAYER = 0;
    public static final Integer PALETTE_LAYER = 100;
    public static final Integer MODAL_LAYER = 200;
    public static final Integer POPUP_LAYER = 300;
    public static final Integer DRAG_LAYER = 400;

    private final Map<TComponent, Integer> layers = new HashMap<>();

    public TJLayeredPane() {
        setLayout(null);
    }

    @Override
    protected void addImpl(TComponent comp, Object constraints, int index) {
        int layer = constraints instanceof Integer ? (Integer) constraints : layerOf(comp);
        if (comp.getParent() == this) {
            remove(comp);
        }
        layers.put(comp, layer);
        // AWT index 0 is the top. Same-layer position 0 means topmost in that layer, -1 means bottom.
        int first = 0;
        while (first < getComponentCount() && layerOf(getComponent(first)) > layer) {
            first++;
        }
        int end = first;
        while (end < getComponentCount() && layerOf(getComponent(end)) == layer) {
            end++;
        }
        int at = index < 0 ? end : Math.min(first + index, end);
        super.addImpl(comp, null, at);
    }

    private int layerOf(TComponent c) {
        Integer l = layers.get(c);
        return l == null ? 0 : l;
    }

    public int getLayer(TComponent c) {
        return layerOf(c);
    }

    public void setLayer(TComponent c, int layer) {
        layers.put(c, layer);
    }
}
