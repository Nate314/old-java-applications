package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Random;

/** One fading spark of a hit or an explosion. */
public class Spark
{
    private static final Random RANDOM = new Random();
    private static final Color  FLAME  = new Color(255, 220, 90);
    private double              x;
    private double              y;
    private final double        vx;
    private final double        vy;
    private final int           life;
    private int                 age;
    private final Color         color;

    private Spark(double x, double y, double vx, double vy, int life, Color color)
    {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.color = color;
    }

    /** Adds {@code count} sparks flying out from (x, y) at up to {@code speed} px per tick. */
    public static void burst(List<Spark> sparks, double x, double y, int count, double speed, Color color)
    {
        for (int i = 0; i < count; i++)
        {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double v = speed * (0.3 + 0.7 * RANDOM.nextDouble());
            sparks.add(new Spark(x, y, Math.cos(angle) * v, Math.sin(angle) * v, 20 + RANDOM.nextInt(25),
                            RANDOM.nextBoolean() ? color : FLAME));
        }
    }

    /** Moves one tick; returns false once the spark has faded out. */
    public boolean move()
    {
        x += vx;
        y += vy;
        return ++age < life;
    }

    public void paint(Graphics2D g)
    {
        int left = life - age;
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255 * left / life));
        int size = 3 + 5 * left / life;
        g.fillOval((int) x - size / 2, (int) y - size / 2, size, size);
    }
}
