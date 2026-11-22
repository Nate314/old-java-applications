package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * The ground a battlefield is fought on: each of the four layouts has its own (a meadow, desert sand, snow, mud), so
 * every round looks like a new place. Your own battlefield and the title are on the meadow.
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
