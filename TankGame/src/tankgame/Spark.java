package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;

/**
 * One fading spark of a hit, an explosion or a muzzle flash, a puff of smoke that rises and spreads, the ring of a blast's
 * shock wave, the flash of light a blast throws on the ground, a tumbling scrap of a destroyed tank, or what a track
 * throws up from the ground (a puff of dust or a clod: see Terrain.kick).
 */
public class Spark
{
    private static final Random RANDOM = new Random();
    private static final Color  FLAME  = new Color(255, 220, 90);
    private static final Color  SMOKE  = new Color(70, 66, 62);
    private enum Kind { SPARK, SMOKE, RING, GLOW, SCRAP, DUST, CLOD }
    private double              x;
    private double              y;
    private double              vx;
    private double              vy;
    private final int           life;
    private int                 age;
    private final Color         color;
    private final Kind          kind;
    /** A ring's or a glow's full radius; a scrap's size. */
    private final double        size;
    private final double        spin;
    private double              turn;

    private Spark(double x, double y, double vx, double vy, int life, Color color, Kind kind, double size)
    {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.color = color;
        this.kind = kind;
        this.size = size;
        this.spin = RANDOM.nextDouble() * 0.6 - 0.3;
    }

    /** Adds {@code count} sparks flying out from (x, y) at up to {@code speed} px per tick. */
    public static void burst(List<Spark> sparks, double x, double y, int count, double speed, Color color)
    {
        for (int i = 0; i < count; i++)
        {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double v = speed * (0.3 + 0.7 * RANDOM.nextDouble());
            sparks.add(new Spark(x, y, Math.cos(angle) * v, Math.sin(angle) * v, 20 + RANDOM.nextInt(25),
                            RANDOM.nextBoolean() ? color : FLAME, Kind.SPARK, 0));
        }
    }

    /** A blast at (x, y): its shock wave spreading to {@code radius} and the light it throws on the ground. */
    public static void blast(List<Spark> sparks, double x, double y, double radius)
    {
        sparks.add(new Spark(x, y, 0, 0, 16 + (int) (radius / 6), FLAME, Kind.GLOW, radius * 0.8));
        sparks.add(new Spark(x, y, 0, 0, 12 + (int) (radius / 8), Color.WHITE, Kind.RING, radius));
    }

    /** Scraps of a destroyed tank thrown out from (x, y), tumbling and skidding to a stop. */
    public static void scraps(List<Spark> sparks, double x, double y, int count, Color color)
    {
        for (int i = 0; i < count; i++)
        {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double v = 3 + 6 * RANDOM.nextDouble();
            sparks.add(new Spark(x, y, Math.cos(angle) * v, Math.sin(angle) * v, 60 + RANDOM.nextInt(40),
                            RANDOM.nextBoolean() ? color : Tank.shade(color, 0.5), Kind.SCRAP, 6 + RANDOM.nextInt(7)));
        }
    }

    /** A puff of smoke drifting up from (x, y). */
    public static void smoke(List<Spark> sparks, double x, double y)
    {
        sparks.add(new Spark(x + RANDOM.nextInt(16) - 8, y + RANDOM.nextInt(16) - 8, RANDOM.nextDouble() - 0.5,
                        -0.4 - RANDOM.nextDouble() * 0.6, 40 + RANDOM.nextInt(30), SMOKE, Kind.SMOKE, 0));
    }

    /** A puff of the ground (dust, powder snow) thrown up from (x, y) at (vx, vy), spreading as it settles. */
    public static void dust(List<Spark> sparks, double x, double y, double vx, double vy, Color color, double size)
    {
        sparks.add(new Spark(x + RANDOM.nextInt(7) - 3, y + RANDOM.nextInt(7) - 3, vx + RANDOM.nextDouble() * 0.6 - 0.3,
                        vy + RANDOM.nextDouble() * 0.6 - 0.3, 30 + RANDOM.nextInt(24), color, Kind.DUST, size));
    }

    /** A clod of the ground (earth, snow, mud) flung from (x, y) at about (vx, vy), skidding to a stop. */
    public static void clod(List<Spark> sparks, double x, double y, double vx, double vy, Color color)
    {
        double spread = 0.6 + RANDOM.nextDouble() * 0.8;
        sparks.add(new Spark(x, y, vx * spread + RANDOM.nextDouble() * 1.6 - 0.8, vy * spread + RANDOM.nextDouble() * 1.6 - 0.8,
                        16 + RANDOM.nextInt(14), color, Kind.CLOD, 3 + RANDOM.nextInt(3)));
    }

    /** Moves one tick; returns false once the spark has faded out. */
    public boolean move()
    {
        x += vx;
        y += vy;
        if (kind == Kind.DUST || kind == Kind.CLOD)
        {
            vx *= kind == Kind.DUST ? 0.93 : 0.84;
            vy *= kind == Kind.DUST ? 0.93 : 0.84;
        }
        if (kind == Kind.SCRAP)
        {
            turn += spin * Math.hypot(vx, vy);
            vx *= 0.9;
            vy *= 0.9;
        }
        return ++age < life;
    }

    public void paint(Graphics2D g)
    {
        int left = life - age;
        switch (kind)
        {
            case RING:
                // Spreads fast, then slows, thinning out as it goes.
                int r = (int) (size * (1 - Math.pow((double) left / life, 3)));
                g.setStroke(new BasicStroke(1 + 6f * left / life));
                g.setColor(new Color(255, 250, 225, 200 * left / life));
                g.drawOval((int) x - r, (int) (y - r * 0.9), 2 * r, (int) (1.8 * r));
                g.setStroke(new BasicStroke(1));
                return;
            case GLOW:
                // Rings of warm light, brightest in the middle, that fade out.
                for (int i = 4; i >= 1; i--)
                {
                    int gr = (int) (size * i / 4);
                    g.setColor(new Color(255, 200, 110, 34 * left / life));
                    g.fillOval((int) x - gr, (int) (y - gr * 0.85), 2 * gr, (int) (1.7 * gr));
                }
                return;
            case SCRAP:
                Graphics2D t = (Graphics2D) g.create();
                t.translate(x, y);
                t.rotate(turn);
                int a = Math.min(255, 255 * left * 3 / life);
                int s = (int) size;
                t.setColor(new Color(0, 0, 0, a / 3));
                t.fillRect(-s / 2 + 2, -s / 3 + 3, s, s * 2 / 3);
                t.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), a));
                t.fillRect(-s / 2, -s / 3, s, s * 2 / 3);
                t.dispose();
                return;
            case DUST:
                int d = (int) (size * (0.5 + age / (double) life));
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 140 * left / life));
                g.fillOval((int) x - d / 2, (int) y - d / 2, d, d);
                return;
            case CLOD:
                int c = (int) size;
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, 400 * left / life)));
                g.fillOval((int) x - c / 2, (int) y - c / 2, c, c);
                return;
            default:
                break;
        }
        if (kind == Kind.SMOKE)
        {
            int size = 8 + 22 * age / life;
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 110 * left / life));
            g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
            return;
        }
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255 * left / life));
        int size = 3 + 5 * left / life;
        g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }
}
