package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * A supply crate dropped onto the battlefield by parachute. It can be taken once it has landed: the first tank to
 * drive over it gets its power, and a shell that hits it blows it up, so a player can deny the other one a crate.
 */
public class Supply
{
    /** What a crate gives the tank that takes it. */
    public enum Power
    {
        REPAIR("Repair", "+", "+1 health", new Color(90, 200, 90)),
        SHIELD("Shield", "S", "stops the next hit", new Color(90, 170, 255)),
        TRIPLE("Triple shot", "3", "next 3 shots spread", new Color(255, 170, 60)),
        RICOCHET("Ricochet", "R", "shells bounce 3 times", new Color(200, 120, 255)),
        TURBO("Turbo", ">", "faster for a while", new Color(255, 230, 70));

        public final String label;
        /** The sign painted on the crate. */
        public final String mark;
        public final String what;
        public final Color  color;

        Power(String label, String mark, String what, Color color)
        {
            this.label = label;
            this.mark = mark;
            this.what = what;
            this.color = color;
        }
    }

    /** Ticks a crate takes to float down (about a second). */
    public static final int  FALL_TICKS = 60;
    /** Half the crate's width; a tank takes it when the crate's middle is on its body or this close to it. */
    public static final int  HALF       = 14;
    private static final Font LETTER    = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private final Power      power;
    private final double     x;
    private final double     y;
    private int              age;

    public Supply(Power power, double x, double y)
    {
        this.power = power;
        this.x = x;
        this.y = y;
    }

    /** One tick of the fall. */
    public void tick()
    {
        age++;
    }

    public boolean landed()
    {
        return age >= FALL_TICKS;
    }

    /** Whether a landed crate is under the tank. */
    public boolean takenBy(Tank tank)
    {
        if (!landed() || !tank.isAlive()) return false;
        return tank.contains(x, y) || Math.hypot(tank.x() - x, tank.y() - y) < Tank.SIZE + HALF;
    }

    /** Whether the shell hits the landed crate. */
    public boolean hitBy(Shell shell)
    {
        return landed() && Math.abs(shell.x() - x) <= HALF + Shell.RADIUS && Math.abs(shell.y() - y) <= HALF + Shell.RADIUS;
    }

    public Power power()
    {
        return power;
    }

    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    /**
     * A wooden crate with its power's sign in its color. While falling it hangs under a striped parachute above a
     * shadow that grows and darkens as it nears the ground; once down it bobs a glow ring so it is easy to spot.
     */
    public void paint(Graphics2D gr, int ticks)
    {
        Graphics2D g = (Graphics2D) gr.create();
        double fall = Math.min(1, age / (double) FALL_TICKS);
        double height = (1 - fall) * 120;
        int shadow = (int) (HALF * (1.2 + fall * 0.6));
        g.setColor(new Color(0, 0, 0, (int) (40 + 60 * fall)));
        g.fillOval((int) (x - shadow + 4), (int) (y - shadow / 2 + 8), shadow * 2, shadow);
        if (landed())
        {
            double pulse = (Math.sin(ticks * 0.12) + 1) / 2;
            int ring = (int) (HALF + 8 + pulse * 6);
            Color c = power.color;
            g.setStroke(new BasicStroke(3));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (90 + 100 * (1 - pulse))));
            g.drawOval((int) x - ring, (int) y - ring, ring * 2, ring * 2);
        }
        double sway = landed() ? 0 : Math.sin(age * 0.15) * 6 * (1 - fall);
        g.translate(x + sway, y - height);
        if (!landed())
        {
            // Parachute: a dome of stripes with lines down to the crate's corners.
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(60, 50, 40));
            g.drawLine(-HALF, -HALF, -26, -38);
            g.drawLine(HALF, -HALF, 26, -38);
            g.drawLine(0, -HALF, 0, -40);
            // Six panels of a half-ellipse, alternately in the power's color and white (polygons only, for the web shim).
            for (int panel = 0; panel < 6; panel++)
            {
                g.setColor(panel % 2 == 0 ? power.color : Color.WHITE);
                int[][] p = canopy(-30 + panel * 10, -20 + panel * 10);
                g.fillPolygon(p[0], p[1], p[0].length);
            }
            int[][] outline = canopy(-30, 30);
            g.setColor(new Color(60, 50, 40));
            g.drawPolygon(outline[0], outline[1], outline[0].length);
        }
        g.setColor(new Color(120, 80, 40));
        g.fillRect(-HALF, -HALF, HALF * 2, HALF * 2);
        g.setColor(new Color(170, 120, 66));
        g.fillRect(-HALF + 3, -HALF + 3, HALF * 2 - 6, HALF * 2 - 6);
        g.setColor(new Color(110, 72, 36));
        g.setStroke(new BasicStroke(2));
        g.drawRect(-HALF, -HALF, HALF * 2, HALF * 2);
        g.setColor(power.color);
        g.fillOval(-9, -9, 18, 18);
        g.setColor(new Color(30, 24, 18));
        g.setFont(LETTER);
        g.drawString(power.mark, -g.getFontMetrics().stringWidth(power.mark) / 2, 6);
        g.dispose();
    }

    /** The parachute's canopy between x = from and x = to: points along its curved top, then back along its rim. */
    private static int[][] canopy(int from, int to)
    {
        int steps = Math.max(2, (to - from) / 2);
        int[] xs = new int[steps + 3];
        int[] ys = new int[steps + 3];
        for (int i = 0; i <= steps; i++)
        {
            double x = from + (to - from) * i / (double) steps;
            xs[i] = (int) Math.round(x);
            ys[i] = (int) Math.round(-38 - 30 * Math.sqrt(Math.max(0, 1 - (x / 30) * (x / 30))));
        }
        xs[steps + 1] = to;
        ys[steps + 1] = -38;
        xs[steps + 2] = from;
        ys[steps + 2] = -38;
        return new int[][] { xs, ys };
    }
}
