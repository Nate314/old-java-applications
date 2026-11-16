package web.shim.javax.swing;

/** Checks that a child moved in a JLayeredPane stays inside its own layer (NathaNOS: Paint under the taskbar). */
public class LayerCheck {
    public static void main(String[] args) {
        // NathaNOS's desktop, top first: power, outlines, Start menu and calendar, taskbar, two windows, icons.
        int[] layers = {500, 400, 300, 300, 100, 0, 0, -1};
        assert LayerRun.first(layers, 0) == 5 && LayerRun.end(layers, 0) == 7;
        assert LayerRun.clamp(layers, 0, 0) == 5 : "a window brought to the front stays under the taskbar";
        assert LayerRun.clamp(layers, 0, 6) == 6 : "a move inside the layer is kept";
        assert LayerRun.clamp(layers, 0, Integer.MAX_VALUE) == 7 : "the bottom of a layer stays over lower layers";
        assert LayerRun.clamp(layers, 300, 0) == 2 : "a menu brought forward stays under the outlines";
        assert LayerRun.clamp(layers, 500, 0) == 0;
        assert LayerRun.clamp(layers, 200, 0) == 4 && LayerRun.end(layers, 200) == 4 : "an empty layer sits between its neighbours";
        assert LayerRun.clamp(new int[0], 0, 3) == 0;
        System.out.println("LayerCheck passed");
    }
}
