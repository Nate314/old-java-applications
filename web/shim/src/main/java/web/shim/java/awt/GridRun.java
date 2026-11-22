package web.shim.java.awt;

/** java.awt.GridLayout's arithmetic, kept free of shim types so GridLayoutCheck can run it on the desktop. */
public final class GridRun {
    private GridRun() {
    }

    /** Rows and columns for n components: a nonzero row count wins, as in the JDK. */
    public static int[] shape(int n, int rows, int cols) {
        return rows > 0 ? new int[] { rows, (n + rows - 1) / rows } : new int[] { (n + cols - 1) / cols, cols };
    }

    /** The grid's size inside the insets, from the largest cell any component wants. */
    public static int[] size(int n, int rows, int cols, int hgap, int vgap, int cellWidth, int cellHeight) {
        int[] rc = shape(n, rows, cols);
        return new int[] { rc[1] * cellWidth + (rc[1] - 1) * hgap, rc[0] * cellHeight + (rc[0] - 1) * vgap };
    }

    /**
     * x, y, width, height of each of the n cells, row by row, inside a width by height space. Cells are equal and the
     * pixels left over from rounding split evenly on both sides, as in the JDK.
     */
    public static int[] cells(int n, int rows, int cols, int hgap, int vgap, int width, int height) {
        int[] out = new int[n * 4];
        if (n == 0) {
            return out;
        }
        int[] rc = shape(n, rows, cols);
        int nrows = rc[0];
        int ncols = rc[1];
        int gapsW = (ncols - 1) * hgap;
        int cellW = (width - gapsW) / ncols;
        int extraW = (width - (cellW * ncols + gapsW)) / 2;
        int gapsH = (nrows - 1) * vgap;
        int cellH = (height - gapsH) / nrows;
        int extraH = (height - (cellH * nrows + gapsH)) / 2;
        for (int i = 0; i < n; i++) {
            // the shape always has a cell for every component, so i / ncols < nrows
            out[i * 4] = extraW + (i % ncols) * (cellW + hgap);
            out[i * 4 + 1] = extraH + (i / ncols) * (cellH + vgap);
            out[i * 4 + 2] = cellW;
            out[i * 4 + 3] = cellH;
        }
        return out;
    }
}
