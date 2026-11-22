package web.shim.javax.swing;

/**
 * Which label names a field, as a reader sees it: the nearest one on its left in the same row or just above it in
 * the same column. Boxes are {x, y, width, height} in one window's coordinates. A label in the field's own container
 * may be any distance away; one elsewhere in the window (a label row over a field row) at most a field's height.
 * Free of the DOM so NearLabelCheck can run it on the JVM.
 */
final class NearLabel {
    private NearLabel() {
    }

    /** The index of the label that names the field, or -1. */
    static int pick(int[] field, int[][] labels, boolean[] sibling) {
        int best = -1;
        int bestGap = Integer.MAX_VALUE;
        for (int i = 0; i < labels.length; i++) {
            int[] c = labels[i];
            int left = field[0] - (c[0] + c[2]);
            int above = field[1] - (c[1] + c[3]);
            boolean sameRow = c[1] < field[1] + field[3] && c[1] + c[3] > field[1];
            boolean sameColumn = c[0] < field[0] + field[2] && c[0] + c[2] > field[0];
            // A label's box may overlap the field's edge by a couple of pixels.
            int gap = sameRow && left >= -2 ? left : sameColumn && above >= -2 ? above : Integer.MAX_VALUE;
            if (!sibling[i] && gap > field[3]) {
                continue;
            }
            if (gap < bestGap) {
                best = i;
                bestGap = gap;
            }
        }
        return best;
    }
}
