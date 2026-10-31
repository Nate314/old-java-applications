package web.shim.java.awt;

/** java.awt.BorderLayout: one component per region, the last one added to a region wins. */
public class TBorderLayout implements TLayoutManager2 {
    public static final String NORTH = "North";
    public static final String SOUTH = "South";
    public static final String EAST = "East";
    public static final String WEST = "West";
    public static final String CENTER = "Center";

    private final int hgap;
    private final int vgap;
    private TComponent north;
    private TComponent south;
    private TComponent east;
    private TComponent west;
    private TComponent center;

    public TBorderLayout() {
        this(0, 0);
    }

    public TBorderLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    @Override
    public void addLayoutComponent(TComponent comp, Object constraints) {
        addLayoutComponent(constraints instanceof String ? (String) constraints : null, comp);
    }

    @Override
    public void addLayoutComponent(String name, TComponent comp) {
        if (name == null || CENTER.equals(name)) {
            center = comp;
        } else if (NORTH.equals(name)) {
            north = comp;
        } else if (SOUTH.equals(name)) {
            south = comp;
        } else if (EAST.equals(name)) {
            east = comp;
        } else if (WEST.equals(name)) {
            west = comp;
        } else {
            throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
        }
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
        if (comp == center) {
            center = null;
        } else if (comp == north) {
            north = null;
        } else if (comp == south) {
            south = null;
        } else if (comp == east) {
            east = null;
        } else if (comp == west) {
            west = null;
        }
    }

    private static boolean shown(TComponent c) {
        return c != null && c.isVisible();
    }

    @Override
    public TDimension preferredLayoutSize(TContainer target) {
        TDimension dim = new TDimension(0, 0);
        if (shown(east)) {
            TDimension d = east.getPreferredSize();
            dim.width += d.width + hgap;
            dim.height = Math.max(d.height, dim.height);
        }
        if (shown(west)) {
            TDimension d = west.getPreferredSize();
            dim.width += d.width + hgap;
            dim.height = Math.max(d.height, dim.height);
        }
        if (shown(center)) {
            TDimension d = center.getPreferredSize();
            dim.width += d.width;
            dim.height = Math.max(d.height, dim.height);
        }
        if (shown(north)) {
            TDimension d = north.getPreferredSize();
            dim.width = Math.max(d.width, dim.width);
            dim.height += d.height + vgap;
        }
        if (shown(south)) {
            TDimension d = south.getPreferredSize();
            dim.width = Math.max(d.width, dim.width);
            dim.height += d.height + vgap;
        }
        TInsets insets = target.getInsets();
        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom;
        return dim;
    }

    @Override
    public TDimension minimumLayoutSize(TContainer target) {
        return preferredLayoutSize(target);
    }

    @Override
    public void layoutContainer(TContainer target) {
        TInsets insets = target.getInsets();
        int top = insets.top;
        int bottom = target.height - insets.bottom;
        int left = insets.left;
        int right = target.width - insets.right;
        if (shown(north)) {
            int h = north.getPreferredSize().height;
            north.setBounds(left, top, right - left, h);
            top += h + vgap;
        }
        if (shown(south)) {
            int h = south.getPreferredSize().height;
            south.setBounds(left, bottom - h, right - left, h);
            bottom -= h + vgap;
        }
        if (shown(east)) {
            int w = east.getPreferredSize().width;
            east.setBounds(right - w, top, w, bottom - top);
            right -= w + hgap;
        }
        if (shown(west)) {
            int w = west.getPreferredSize().width;
            west.setBounds(left, top, w, bottom - top);
            left += w + hgap;
        }
        if (shown(center)) {
            center.setBounds(left, top, right - left, bottom - top);
        }
    }
}
