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
        int[] order = layersWithout(comp);
        int end = LayerRun.end(order, layer);
        int at = index < 0 ? end : Math.min(LayerRun.first(order, layer) + index, end);
        super.addImpl(comp, null, at);
    }

    /** As in Swing, a child moves only within its own layer: a frame brought forward stays under the taskbar and menus. */
    @Override
    public void setComponentZOrder(TComponent comp, int index) {
        super.setComponentZOrder(comp, LayerRun.clamp(layersWithout(comp), layerOf(comp), index));
    }

    /** The other children's layers, top first. */
    private int[] layersWithout(TComponent comp) {
        int[] order = new int[getComponentCount()];
        int n = 0;
        for (int i = 0; i < order.length; i++) {
            if (getComponent(i) != comp) {
                order[n++] = layerOf(getComponent(i));
            }
        }
        return java.util.Arrays.copyOf(order, n);
    }

    /** remove(comp) and removeAll() come here too, so a removed component's layer entry no longer keeps it alive. */
    @Override
    public void remove(int index) {
        layers.remove(getComponent(index));
        super.remove(index);
    }

    private int layerOf(TComponent c) {
        Integer l = layers.get(c);
        return l == null ? 0 : l;
    }

    public int getLayer(TComponent c) {
        return layerOf(c);
    }

    /** Like Swing, a child given a new layer goes to the bottom of that layer. */
    public void setLayer(TComponent c, int layer) {
        layers.put(c, layer);
        setComponentZOrder(c, Integer.MAX_VALUE);
    }
}
