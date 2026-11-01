package web.shim.java.awt;

/** Same algorithm as java.awt.FlowLayout (rows of preferred sizes, centered by default, 5 px gaps). */
public class TFlowLayout implements TLayoutManager {
    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;
    public static final int LEADING = 3;
    public static final int TRAILING = 4;

    private int align;
    private int hgap;
    private int vgap;

    public TFlowLayout() {
        this(CENTER, 5, 5);
    }

    public TFlowLayout(int align) {
        this(align, 5, 5);
    }

    public TFlowLayout(int align, int hgap, int vgap) {
        this.align = align;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    public int getAlignment() {
        return align;
    }

    public void setAlignment(int align) {
        this.align = align;
    }

    public int getHgap() {
        return hgap;
    }

    public int getVgap() {
        return vgap;
    }

    @Override
    public void addLayoutComponent(String name, TComponent comp) {
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
    }

    @Override
    public TDimension preferredLayoutSize(TContainer target) {
        TDimension dim = new TDimension(0, 0);
        boolean first = true;
        for (TComponent m : target.children) {
            if (m.isVisible()) {
                TDimension d = m.getPreferredSize();
                dim.height = Math.max(dim.height, d.height);
                if (!first) {
                    dim.width += hgap;
                }
                first = false;
                dim.width += d.width;
            }
        }
        TInsets insets = target.getInsets();
        dim.width += insets.left + insets.right + hgap * 2;
        dim.height += insets.top + insets.bottom + vgap * 2;
        return dim;
    }

    @Override
    public TDimension minimumLayoutSize(TContainer target) {
        return preferredLayoutSize(target);
    }

    @Override
    public void layoutContainer(TContainer target) {
        TInsets insets = target.getInsets();
        int maxWidth = target.width - (insets.left + insets.right + hgap * 2);
        int x = 0;
        int y = insets.top + vgap;
        int rowHeight = 0;
        int start = 0;
        int n = target.children.size();
        for (int i = 0; i < n; i++) {
            TComponent m = target.children.get(i);
            if (!m.isVisible()) {
                continue;
            }
            TDimension d = m.getPreferredSize();
            m.setSize(d.width, d.height);
            if (x == 0 || x + d.width <= maxWidth) {
                if (x > 0) {
                    x += hgap;
                }
                x += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            } else {
                moveRow(target, insets.left + hgap, y, maxWidth - x, rowHeight, start, i);
                x = d.width;
                y += vgap + rowHeight;
                rowHeight = d.height;
                start = i;
            }
        }
        moveRow(target, insets.left + hgap, y, maxWidth - x, rowHeight, start, n);
    }

    private void moveRow(TContainer target, int x, int y, int spare, int rowHeight, int start, int end) {
        if (align == CENTER) {
            x += spare / 2;
        } else if (align == RIGHT || align == TRAILING) {
            x += spare;
        }
        for (int i = start; i < end; i++) {
            TComponent m = target.children.get(i);
            if (m.isVisible()) {
                m.setLocation(x, y + (rowHeight - m.height) / 2);
                x += m.width + hgap;
            }
        }
    }
}
