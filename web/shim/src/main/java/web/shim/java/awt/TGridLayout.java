package web.shim.java.awt;

/** java.awt.GridLayout: equal cells, filled row by row (left to right), every component counted, visible or not. */
public class TGridLayout implements TLayoutManager {
    private int hgap;
    private int vgap;
    private int rows;
    private int cols;

    public TGridLayout() {
        this(1, 0, 0, 0);
    }

    public TGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    public TGridLayout(int rows, int cols, int hgap, int vgap) {
        if (rows == 0 && cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        if (rows == 0 && cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    public int getColumns() {
        return cols;
    }

    public void setColumns(int cols) {
        if (rows == 0 && cols == 0) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    public int getHgap() {
        return hgap;
    }

    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    public int getVgap() {
        return vgap;
    }

    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    @Override
    public void addLayoutComponent(String name, TComponent comp) {
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
    }

    @Override
    public TDimension preferredLayoutSize(TContainer parent) {
        return layoutSize(parent, true);
    }

    @Override
    public TDimension minimumLayoutSize(TContainer parent) {
        return layoutSize(parent, false);
    }

    private TDimension layoutSize(TContainer parent, boolean preferred) {
        int w = 0;
        int h = 0;
        for (TComponent c : parent.children) {
            TDimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
            w = Math.max(w, d.width);
            h = Math.max(h, d.height);
        }
        TInsets in = parent.getInsets();
        int[] size = GridRun.size(parent.children.size(), rows, cols, hgap, vgap, w, h);
        return new TDimension(in.left + in.right + size[0], in.top + in.bottom + size[1]);
    }

    @Override
    public void layoutContainer(TContainer parent) {
        TInsets in = parent.getInsets();
        int n = parent.children.size();
        int[] cells = GridRun.cells(n, rows, cols, hgap, vgap, parent.width - (in.left + in.right),
                parent.height - (in.top + in.bottom));
        for (int i = 0; i < n; i++) {
            parent.children.get(i).setBounds(in.left + cells[i * 4], in.top + cells[i * 4 + 1], cells[i * 4 + 2], cells[i * 4 + 3]);
        }
    }

    @Override
    public String toString() {
        return "java.awt.GridLayout[hgap=" + hgap + ",vgap=" + vgap + ",rows=" + rows + ",cols=" + cols + "]";
    }
}
