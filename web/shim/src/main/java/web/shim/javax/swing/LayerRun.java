package web.shim.javax.swing;

/**
 * Where a layer's children sit in a JLayeredPane: children are kept top first with higher layers first, so each
 * layer is one run of indexes. Plain Java, so web/shim/test/LayerCheck.java can run it on the desktop.
 */
final class LayerRun {
    private LayerRun() {
    }

    /** Index of the first child not above layer; layers are the children's layers, top first. */
    static int first(int[] layers, int layer) {
        int i = 0;
        while (i < layers.length && layers[i] > layer) {
            i++;
        }
        return i;
    }

    /** One past the last child in layer. */
    static int end(int[] layers, int layer) {
        int i = first(layers, layer);
        while (i < layers.length && layers[i] == layer) {
            i++;
        }
        return i;
    }

    /** A z-order index kept inside layer's run, so moving a child never lifts it over a higher layer. */
    static int clamp(int[] layers, int layer, int index) {
        return Math.max(first(layers, layer), Math.min(index, end(layers, layer)));
    }
}
