package tankgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The battlefield of one round: concrete walls that stop tanks and shells. Shells bounce off a wall as they do off the
 * edge of the field (once in all), so a wall is cover and a cushion for bank shots. Each round is fought on the next
 * layout, placed by fractions of the field so it fits any window; a wall that would sit on a tank is left out.
 */
public class Arena
{
    /**
     * A layout's walls: center X and Y as fractions of the field, then width and height in pixels (or, for a
     * battlefield built by the player, as fractions of the field too, so it fills any window the same way).
     */
    public static final class Layout
    {
        final String     name;
        final double[][] walls;
        final boolean    fractional;

        Layout(String name, double[]... walls)
        {
            this(name, false, walls);
        }

        Layout(String name, boolean fractional, double[]... walls)
        {
            this.name = name;
            this.fractional = fractional;
            this.walls = walls;
        }

        public String name()
        {
            return name;
        }
    }

    /** One wall, a rectangle. */
    public static final class Wall
    {
        public final double left;
        public final double top;
        public final double right;
        public final double bottom;

        Wall(double left, double top, double right, double bottom)
        {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        boolean overlaps(double l, double t, double r, double b)
        {
            return l < right && r > left && t < bottom && b > top;
        }

        /**
         * Where the segment from (x1, y1) to (x2, y2) enters this wall grown by {@code margin}: the fraction along the
         * segment and the side it came through (0 left or right, 1 top or bottom), or null when it misses. A segment
         * that starts inside gets fraction 0 and side -1.
         */
        double[] entry(double x1, double y1, double x2, double y2, double margin)
        {
            double t0 = 0;
            double t1 = 1;
            int side = -1;
            double[] lo = { left - margin, top - margin };
            double[] hi = { right + margin, bottom + margin };
            double[] from = { x1, y1 };
            double[] delta = { x2 - x1, y2 - y1 };
            for (int axis = 0; axis < 2; axis++)
            {
                if (delta[axis] == 0)
                {
                    if (from[axis] < lo[axis] || from[axis] > hi[axis]) return null;
                    continue;
                }
                double a = (lo[axis] - from[axis]) / delta[axis];
                double b = (hi[axis] - from[axis]) / delta[axis];
                if (Math.min(a, b) > t0)
                {
                    t0 = Math.min(a, b);
                    side = axis;
                }
                t1 = Math.min(t1, Math.max(a, b));
                if (t0 > t1) return null;
            }
            return new double[] { t0, side };
        }
    }

    public static final List<Layout> LAYOUTS = Collections.unmodifiableList(Arrays.asList(
                    new Layout("Stepping Stones", new double[] { 0.5, 0.17, 54, 54 }, new double[] { 0.5, 0.5, 54, 54 },
                                    new double[] { 0.5, 0.83, 54, 54 }),
                    new Layout("The Bunker", new double[] { 0.5, 0.5, 26, 160 }, new double[] { 0.27, 0.18, 36, 36 },
                                    new double[] { 0.73, 0.82, 36, 36 }),
                    new Layout("Gates", new double[] { 0.5, 0.1, 26, 120 }, new double[] { 0.5, 0.9, 26, 120 },
                                    new double[] { 0.3, 0.2, 44, 44 }, new double[] { 0.7, 0.8, 44, 44 }),
                    new Layout("Trenches", new double[] { 0.5, 0.29, 130, 26 }, new double[] { 0.5, 0.71, 130, 26 },
                                    new double[] { 0.2, 0.18, 40, 40 }, new double[] { 0.8, 0.82, 40, 40 })));

    /** A field with no walls, as the game was before walls. */
    public static final List<Layout> OPEN  = Collections.singletonList(new Layout("Open Field"));
    public static final Arena        EMPTY = new Arena(OPEN.get(0), Collections.<Wall>emptyList());

    /** The player's own battlefield is laid on a grid of this many cells across and down the field. */
    public static final int          GRID  = 12;

    private final Layout             layout;
    private final List<Wall>         walls;

    private Arena(Layout layout, List<Wall> walls)
    {
        this.layout = layout;
        this.walls = walls;
    }

    /** Places the layout's walls on a field of this size, leaving out any that would overlap a tank. */
    public static Arena build(Layout layout, int width, int height, Tank... tanks)
    {
        List<Wall> walls = new ArrayList<>();
        for (double[] w : layout.walls)
        {
            double cx = w[0] * width;
            double cy = w[1] * height;
            double ww = layout.fractional ? w[2] * width : w[2];
            double wh = layout.fractional ? w[3] * height : w[3];
            Wall wall = new Wall(cx - ww / 2, cy - wh / 2, cx + ww / 2, cy + wh / 2);
            if (clearOf(wall, tanks)) walls.add(wall);
        }
        return new Arena(layout, Collections.unmodifiableList(walls));
    }

    /** Whether a wall leaves room round every tank (where they start, when the arena is placed). */
    private static boolean clearOf(Wall wall, Tank... tanks)
    {
        for (Tank tank : tanks)
            if (wall.overlaps(tank.x() - Tank.SIZE - 4, tank.y() - Tank.SIZE - 4, tank.x() + Tank.SIZE + 4,
                            tank.y() + Tank.SIZE + 4)) return false;
        return true;
    }

    /**
     * Whether one tank can drive to the other round the walls: a search over the spots a tank may stand on (inside
     * the field and off the walls, as {@link Tank} keeps it), 5 px apart, the distance a tank drives in a tick.
     */
    public boolean connected(int width, int height, Tank from, Tank to)
    {
        int step = Tank.SPEED;
        int cols = Math.max(1, (width - 2 * Tank.SIZE) / step + 1);
        int rows = Math.max(1, (height - 2 * Tank.SIZE) / step + 1);
        boolean[] seen = new boolean[cols * rows];
        int[] queue = new int[cols * rows];
        int start = spot(from.x(), from.y(), cols, rows);
        int head = 0;
        int tail = 0;
        queue[tail++] = start;
        seen[start] = true;
        while (head < tail)
        {
            int at = queue[head++];
            int c = at % cols;
            int r = at / cols;
            double x = Tank.SIZE + c * step;
            double y = Tank.SIZE + r * step;
            if (Math.abs(x - to.x()) <= step && Math.abs(y - to.y()) <= step) return true;
            int[][] moves = { { c - 1, r }, { c + 1, r }, { c, r - 1 }, { c, r + 1 } };
            for (int[] m : moves)
            {
                if (m[0] < 0 || m[1] < 0 || m[0] >= cols || m[1] >= rows || seen[m[1] * cols + m[0]]) continue;
                if (blocks(Tank.SIZE + m[0] * step, Tank.SIZE + m[1] * step, Tank.SIZE)) continue;
                seen[m[1] * cols + m[0]] = true;
                queue[tail++] = m[1] * cols + m[0];
            }
        }
        return false;
    }

    private static int spot(double x, double y, int cols, int rows)
    {
        int c = (int) Math.max(0, Math.min(cols - 1, Math.round((x - Tank.SIZE) / Tank.SPEED)));
        int r = (int) Math.max(0, Math.min(rows - 1, Math.round((y - Tank.SIZE) / Tank.SPEED)));
        return r * cols + c;
    }

    /** Whether a wall may go in this cell of the grid: not on or right beside a tank. */
    public static boolean cellFree(int col, int row, int width, int height, Tank... tanks)
    {
        return clearOf(new Wall(col * (double) width / GRID, row * (double) height / GRID,
                        (col + 1) * (double) width / GRID, (row + 1) * (double) height / GRID), tanks);
    }

    /** The player's battlefield: each run of wall cells along a row of the grid is one wall. */
    public static Layout custom(boolean[][] cells)
    {
        List<double[]> walls = new ArrayList<>();
        for (int row = 0; row < GRID; row++)
            for (int col = 0; col < GRID; col++)
            {
                if (!cells[row][col]) continue;
                int end = col;
                while (end + 1 < GRID && cells[row][end + 1]) end++;
                walls.add(new double[] { (col + end + 1) / 2.0 / GRID, (row + 0.5) / GRID, (end - col + 1.0) / GRID,
                                1.0 / GRID });
                col = end;
            }
        return new Layout("Your Battlefield", true, walls.toArray(new double[0][]));
    }

    /** The grid as text for the record file: rows of {@code #} (wall) and {@code .} joined by {@code /}. */
    public static String text(boolean[][] cells)
    {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < GRID; row++)
        {
            if (row > 0) text.append('/');
            for (int col = 0; col < GRID; col++) text.append(cells[row][col] ? '#' : '.');
        }
        return text.toString();
    }

    /** Reads {@link #text}; anything damaged or missing reads as no wall. */
    public static boolean[][] cells(String text)
    {
        boolean[][] cells = new boolean[GRID][GRID];
        String[] rows = text == null ? new String[0] : text.split("/");
        for (int row = 0; row < Math.min(GRID, rows.length); row++)
            for (int col = 0; col < Math.min(GRID, rows[row].length()); col++) cells[row][col] = rows[row].charAt(col) == '#';
        return cells;
    }

    public String name()
    {
        return layout.name;
    }

    public Layout layout()
    {
        return layout;
    }

    public List<Wall> walls()
    {
        return walls;
    }

    /** Whether a tank's square (unturned, like the edge of the field) centered here would touch a wall. */
    public boolean blocks(double x, double y, double half)
    {
        for (Wall wall : walls)
            if (wall.overlaps(x - half, y - half, x + half, y + half)) return true;
        return false;
    }

    /**
     * The first wall the segment runs into, grown by {@code margin}: the fraction along it, the side it came through
     * (see {@link Wall#entry}), or null when the way is clear.
     */
    public double[] firstHit(double x1, double y1, double x2, double y2, double margin)
    {
        double[] first = null;
        for (Wall wall : walls)
        {
            double[] hit = wall.entry(x1, y1, x2, y2, margin);
            if (hit != null && (first == null || hit[0] < first[0])) first = hit;
        }
        return first;
    }

    /** Whether a shell could fly straight from one point to the other without touching a wall. */
    public boolean clear(double x1, double y1, double x2, double y2)
    {
        return firstHit(x1, y1, x2, y2, Shell.RADIUS) == null;
    }
}
