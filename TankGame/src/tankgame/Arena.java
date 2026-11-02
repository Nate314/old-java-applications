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
    /** A layout's walls: center X and Y as fractions of the field, then width and height in pixels. */
    public static final class Layout
    {
        final String     name;
        final double[][] walls;

        Layout(String name, double[]... walls)
        {
            this.name = name;
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
            Wall wall = new Wall(cx - w[2] / 2, cy - w[3] / 2, cx + w[2] / 2, cy + w[3] / 2);
            boolean free = true;
            for (Tank tank : tanks)
                free &= !wall.overlaps(tank.x() - Tank.SIZE - 4, tank.y() - Tank.SIZE - 4, tank.x() + Tank.SIZE + 4,
                                tank.y() + Tank.SIZE + 4);
            if (free) walls.add(wall);
        }
        return new Arena(layout, Collections.unmodifiableList(walls));
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
