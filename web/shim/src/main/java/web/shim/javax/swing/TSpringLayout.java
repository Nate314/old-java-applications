package web.shim.javax.swing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import web.shim.java.awt.TComponent;
import web.shim.java.awt.TContainer;
import web.shim.java.awt.TDimension;
import web.shim.java.awt.TInsets;
import web.shim.java.awt.TLayoutManager2;

/**
 * SpringLayout for fixed-pad constraints (putConstraint with an int pad), which is all these apps use.
 * Like Swing, each axis remembers the last two edges set on a component; the remaining edge and the size come
 * from those two, falling back to the preferred size.
 */
public class TSpringLayout implements TLayoutManager2 {
    public static final String NORTH = "North";
    public static final String SOUTH = "South";
    public static final String EAST = "East";
    public static final String WEST = "West";
    public static final String HORIZONTAL_CENTER = "HorizontalCenter";
    public static final String VERTICAL_CENTER = "VerticalCenter";
    public static final String WIDTH = "Width";
    public static final String HEIGHT = "Height";

    private static final class Edge {
        final String name;
        final int pad;
        final String anchorEdge;
        final TComponent anchor;

        Edge(String name, int pad, String anchorEdge, TComponent anchor) {
            this.name = name;
            this.pad = pad;
            this.anchorEdge = anchorEdge;
            this.anchor = anchor;
        }
    }

    private static final class Constraints {
        final List<Edge> horizontal = new ArrayList<>();
        final List<Edge> vertical = new ArrayList<>();
    }

    private final Map<TComponent, Constraints> constraints = new HashMap<>();
    private TContainer parent;
    private Map<TComponent, int[]> solved;

    public void putConstraint(String e1, TComponent c1, int pad, String e2, TComponent c2) {
        Constraints c = constraints.computeIfAbsent(c1, k -> new Constraints());
        List<Edge> axis = isHorizontal(e1) ? c.horizontal : c.vertical;
        axis.removeIf(edge -> edge.name.equals(e1));
        axis.add(new Edge(e1, pad, e2, c2));
        if (axis.size() > 2) {
            axis.remove(0);
        }
    }

    private static boolean isHorizontal(String edge) {
        return WEST.equals(edge) || EAST.equals(edge) || HORIZONTAL_CENTER.equals(edge) || WIDTH.equals(edge);
    }

    @Override
    public void addLayoutComponent(TComponent comp, Object constraints) {
    }

    @Override
    public void addLayoutComponent(String name, TComponent comp) {
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
        constraints.remove(comp);
    }

    @Override
    public TDimension preferredLayoutSize(TContainer target) {
        return new TDimension(target.getWidth(), target.getHeight());
    }

    @Override
    public TDimension minimumLayoutSize(TContainer target) {
        return new TDimension(0, 0);
    }

    @Override
    public void layoutContainer(TContainer target) {
        parent = target;
        solved = new IdentityHashMap<>();
        for (int i = 0; i < target.getComponentCount(); i++) {
            TComponent c = target.getComponent(i);
            int[] b = bounds(c);
            c.setBounds(b[0], b[1], b[2], b[3]);
        }
        solved = null;
    }

    /** x, y, width, height of a child, solving its constraints (and those it depends on) once per layout. */
    private int[] bounds(TComponent c) {
        int[] b = solved.get(c);
        if (b != null) {
            return b;
        }
        b = new int[4];
        solved.put(c, b); // guards against cycles
        TDimension pref = c.getPreferredSize();
        Constraints cs = constraints.get(c);
        int[] h = solveAxis(cs == null ? null : cs.horizontal, pref.width, WEST, EAST, HORIZONTAL_CENTER);
        int[] v = solveAxis(cs == null ? null : cs.vertical, pref.height, NORTH, SOUTH, VERTICAL_CENTER);
        b[0] = h[0];
        b[1] = v[0];
        b[2] = h[1];
        b[3] = v[1];
        return b;
    }

    private int[] solveAxis(List<Edge> edges, int pref, String start, String end, String center) {
        Integer s = null;
        Integer e = null;
        Integer m = null;
        if (edges != null) {
            for (Edge edge : edges) {
                int value = edgeValue(edge.anchor, edge.anchorEdge) + edge.pad;
                if (edge.name.equals(start)) {
                    s = value;
                } else if (edge.name.equals(end)) {
                    e = value;
                } else if (edge.name.equals(center)) {
                    m = value;
                }
            }
        }
        if (s != null && e != null) {
            return new int[] { s, e - s };
        } else if (s != null && m != null) {
            return new int[] { s, 2 * (m - s) };
        } else if (e != null && m != null) {
            return new int[] { e - 2 * (e - m), 2 * (e - m) };
        } else if (s != null) {
            return new int[] { s, pref };
        } else if (e != null) {
            return new int[] { e - pref, pref };
        } else if (m != null) {
            return new int[] { m - pref / 2, pref };
        }
        return new int[] { 0, pref };
    }

    private int edgeValue(TComponent c, String edge) {
        int x;
        int y;
        int w;
        int h;
        if (c == parent) {
            TInsets in = parent.getInsets();
            x = in.left;
            y = in.top;
            w = parent.getWidth() - in.left - in.right;
            h = parent.getHeight() - in.top - in.bottom;
        } else {
            int[] b = bounds(c);
            x = b[0];
            y = b[1];
            w = b[2];
            h = b[3];
        }
        switch (edge) {
            case WEST: return x;
            case EAST: return x + w;
            case HORIZONTAL_CENTER: return x + w / 2;
            case WIDTH: return w;
            case NORTH: return y;
            case SOUTH: return y + h;
            case VERTICAL_CENTER: return y + h / 2;
            case HEIGHT: return h;
            default: throw new IllegalArgumentException("Unsupported SpringLayout edge: " + edge);
        }
    }
}
