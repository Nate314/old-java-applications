package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;

/**
 * The ground a battlefield is fought on: each of the four layouts has its own (a meadow, desert sand, snow, mud), so
 * every round looks like a new place. Your own battlefield and the title are on the meadow. A ground also builds the
 * walls in its own material (mossy fieldstone, sandstone, snow-capped concrete, sandbags), gives the tracks what they
 * throw up (bits of turf, dust, powder snow, clods of mud), and has its own weather (see Weather).
 */
public enum Terrain
{
    MEADOW("in the meadow", new Color(106, 122, 72),
                    new Color[] { new Color(136, 150, 88, 34), new Color(84, 100, 56, 34), new Color(132, 112, 78, 40) },
                    new Color[] { new Color(64, 86, 40, 170), new Color(150, 168, 96, 150) }, 900,
                    new Color(160, 154, 142), new Color(52, 44, 30)),
    DESERT("in the desert", new Color(196, 168, 116),
                    new Color[] { new Color(222, 196, 142, 46), new Color(168, 136, 88, 40), new Color(206, 160, 104, 36) },
                    new Color[] { new Color(150, 120, 76, 120), new Color(232, 210, 160, 120) }, 700,
                    new Color(150, 128, 98), new Color(110, 82, 50)),
    SNOW("in the snow", new Color(220, 226, 232),
                    new Color[] { new Color(176, 190, 214, 46), new Color(246, 248, 252, 70), new Color(196, 206, 222, 40) },
                    new Color[] { new Color(96, 88, 78, 150), new Color(130, 120, 104, 120) }, 2600,
                    new Color(110, 112, 118), new Color(70, 80, 100)),
    MUD("in the mud", new Color(110, 88, 62),
                    new Color[] { new Color(74, 62, 46, 60), new Color(140, 116, 84, 40), new Color(82, 92, 96, 70) },
                    new Color[] { new Color(92, 110, 56, 170), new Color(126, 140, 78, 140) }, 1800,
                    new Color(134, 120, 100), new Color(40, 30, 20));

    /** Said on the round's banner after the battlefield's name. */
    public final String  where;
    /** The ground's plain color, also behind the field while it shakes. */
    public final Color   base;
    /** What a track presses into this ground (its alpha is set where it is drawn). */
    public final Color   track;
    private final Color[] patches;
    private final Color[] tufts;
    private final int     tuftArea;
    private final Color   stone;

    Terrain(String where, Color base, Color[] patches, Color[] tufts, int tuftArea, Color stone, Color track)
    {
        this.where = where;
        this.base = base;
        this.patches = patches;
        this.tufts = tufts;
        this.tuftArea = tuftArea;
        this.stone = stone;
        this.track = track;
    }

    /** The color of what the tracks throw up: turf, dust, snow, mud. */
    private Color kicked()
    {
        switch (this)
        {
            case DESERT: return new Color(236, 216, 172);
            case SNOW: return new Color(196, 208, 228);
            case MUD: return new Color(58, 42, 26);
            default: return new Color(92, 104, 58);
        }
    }

    /**
     * What one track throws up as it churns the ground at (x, y), flung at (vx, vy): a cloud of dust in the desert,
     * powder and lumps of snow, clods of mud, and now and then a bit of turf in the meadow.
     */
    public void kick(List<Spark> sparks, double x, double y, double vx, double vy, Random random)
    {
        Color color = kicked();
        switch (this)
        {
            case DESERT:
                Spark.dust(sparks, x, y, vx * 0.5, vy * 0.5, color, 30);
                break;
            case SNOW:
                Spark.clod(sparks, x, y, vx, vy, color);
                if (random.nextInt(2) == 0) Spark.dust(sparks, x, y, vx * 0.3, vy * 0.3, Color.WHITE, 16);
                break;
            case MUD:
                Spark.clod(sparks, x, y, vx, vy, color);
                Spark.clod(sparks, x, y, vx, vy, Tank.shade(color, 1.4));
                break;
            default:
                if (random.nextInt(3) == 0) Spark.clod(sparks, x, y, vx, vy, color);
                break;
        }
    }

    /**
     * A wall of this ground's material, filling exactly the wall's rectangle (what stops tanks and shells), with a drop
     * shadow, a lit top and left edge and a shaded bottom and right.
     */
    public void wall(Graphics2D g, Arena.Wall wall)
    {
        int l = (int) Math.round(wall.left);
        int t = (int) Math.round(wall.top);
        int w = (int) Math.round(wall.right - wall.left);
        int h = (int) Math.round(wall.bottom - wall.top);
        boolean across = w >= h;
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(l + 6, t + 8, w, h, 6, 6);
        Color body = this == DESERT ? new Color(186, 136, 86) : this == SNOW ? new Color(136, 142, 152)
                        : this == MUD ? new Color(150, 136, 96) : new Color(150, 146, 138);
        g.setColor(body);
        g.fillRect(l, t, w, h);
        Graphics2D c = (Graphics2D) g.create();
        c.clipRect(l, t, w, h);
        // Seeded by the wall's place, so its bags, lumps and moss stay put from frame to frame.
        Random random = new Random(l * 31 + t);
        switch (this)
        {
            case DESERT:
                // Courses of sandstone blocks, each course's joints halfway along the last's.
                c.setColor(Tank.shade(body, 0.74));
                for (int y = t + 12; y < t + h; y += 12) c.drawLine(l, y, l + w, y);
                for (int y = t, row = 0; y < t + h; y += 12, row++)
                    for (int x = l + (row % 2) * 13 + 26; x < l + w; x += 26) c.drawLine(x, y, x, y + 12);
                edges(c, l, t, w, h, body);
                break;
            case SNOW:
                edges(c, l, t, w, h, body);
                // A cap of snow over the top, leaving the shaded rim showing, with lumps along its edge.
                c.setColor(new Color(176, 192, 216));
                c.fillRoundRect(l + 2, t + 2, w - 4, h - 4, 10, 10);
                c.setColor(new Color(244, 247, 252));
                c.fillRoundRect(l + 1, t + 1, w - 5, h - 5, 10, 10);
                for (int i = 0; i < (across ? w : h) / 12; i++)
                {
                    int r = 4 + random.nextInt(5);
                    if (across) c.fillOval(l + random.nextInt(Math.max(1, w)) - r / 2, t + h - 6 - r / 2, r, r);
                    else c.fillOval(l + w - 6 - r / 2, t + random.nextInt(Math.max(1, h)) - r / 2, r, r);
                }
                break;
            case MUD:
                // Sandbags laid in rows along the wall, each row half a bag along from the last.
                int bag = 22;
                int thick = 11;
                for (int row = 0; row * thick < (across ? h : w); row++)
                    for (int along = -(row % 2) * bag / 2; along < (across ? w : h); along += bag)
                    {
                        int x = across ? l + along : l + row * thick;
                        int y = across ? t + row * thick : t + along;
                        int bw = across ? bag - 1 : thick - 1;
                        int bh = across ? thick - 1 : bag - 1;
                        c.setColor(Tank.shade(body, 0.6));
                        c.fillRoundRect(x + 1, y + 1, bw, bh, 8, 8);
                        c.setColor(Tank.shade(body, 0.92 + random.nextInt(3) * 0.08));
                        c.fillRoundRect(x, y, bw, bh, 8, 8);
                        c.setColor(Tank.shade(body, 1.25));
                        if (across) c.drawLine(x + 3, y + 2, x + bw - 4, y + 2);
                        else c.drawLine(x + 2, y + 3, x + 2, y + bh - 4);
                    }
                break;
            default:
                // Fieldstone: block seams, then moss along the shady foot of the wall.
                edges(c, l, t, w, h, body);
                c.setColor(Tank.shade(body, 0.78));
                if (across) for (int x = l + 27; x < l + w - 8; x += 27) c.drawLine(x, t + 4, x, t + h - 5);
                else for (int y = t + 27; y < t + h - 8; y += 27) c.drawLine(l + 4, y, l + w - 5, y);
                for (int i = 0; i < (w + h) / 5; i++)
                {
                    c.setColor(random.nextBoolean() ? new Color(84, 110, 50, 230) : new Color(120, 144, 70, 220));
                    int r = 5 + random.nextInt(8);
                    // Moss creeps in from the edges, thickest on the shady foot and right side.
                    boolean foot = random.nextInt(3) > 0;
                    int x = foot ? l + random.nextInt(Math.max(1, w)) : l + w - random.nextInt(10);
                    int y = foot ? t + h - random.nextInt(10) : t + random.nextInt(Math.max(1, h));
                    c.fillOval(x - r / 2, y - r / 2, r, r * 3 / 4);
                }
                break;
        }
        c.dispose();
        g.setColor(Tank.shade(body, 0.4));
        g.drawRect(l, t, w - 1, h - 1);
        if (this == DESERT)
        {
            // Sand blown up against the foot of the wall, on its shady sides.
            g.setColor(new Color(214, 186, 132));
            for (int x = l + 4; x < l + w - 4; x += 12) g.fillOval(x - 4, t + h - 4, 18, 9);
            for (int y = t + 4; y < t + h - 4; y += 12) g.fillOval(l + w - 4, y - 4, 9, 18);
        }
    }

    /** A block's lit top and left edge and shaded bottom and right. */
    private static void edges(Graphics2D g, int l, int t, int w, int h, Color body)
    {
        g.setColor(Tank.shade(body, 1.3));
        g.fillRect(l, t, w, 4);
        g.fillRect(l, t, 4, h);
        g.setColor(Tank.shade(body, 0.7));
        g.fillRect(l, t + h - 4, w, 4);
        g.fillRect(l + w - 4, t + 4, 4, h - 4);
    }

    /** The ground of a battlefield, by its layout's name: the four layouts in turn, anything else the meadow. */
    public static Terrain of(Arena arena)
    {
        for (int i = 0; i < Arena.LAYOUTS.size(); i++)
            if (Arena.LAYOUTS.get(i).name().equals(arena.name())) return values()[i % values().length];
        return MEADOW;
    }

    /** Lays this ground over a field of the given size: patches, then tufts (ripples in the desert), then stones. */
    public void lay(Graphics2D g, int width, int height, Random random)
    {
        g.setColor(base);
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < width * height / 330; i++)
        {
            int size = 8 + random.nextInt(34);
            g.setColor(patches[random.nextInt(patches.length)]);
            g.fillOval(random.nextInt(width + 20) - 20, random.nextInt(height + 20) - 20, size, size * 3 / 4);
        }
        for (int i = 0; i < width * height / tuftArea; i++)
        {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            g.setColor(tufts[random.nextInt(tufts.length)]);
            if (this == DESERT)
                // Wind ripples: a shallow arc, all lying the same way.
                g.drawArc(x - 9, y, 18 + random.nextInt(8), 5, 20, 140);
            else
                for (int b = -1; b <= 1; b++) g.drawLine(x, y, x + b * 2 + random.nextInt(2), y - 3 - random.nextInt(3));
        }
        for (int i = 0; i < width * height / 4500; i++)
        {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = 3 + random.nextInt(4);
            g.setColor(new Color(60, 56, 48, 120));
            g.fillOval(x + 1, y + 1, size, size - 1);
            g.setColor(stone);
            g.fillOval(x, y, size, size - 1);
        }
    }
}
