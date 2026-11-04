package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * A fired shell: flies straight, bounces once (three times after a Ricochet crate) off the edge of the field or a wall,
 * and then flies out or bursts on the next wall it meets.
 */
public class Shell
{
    public static final int  SPEED       = 10;
    /** How far from a wall the shell's center is when it touches it. */
    public static final int  RADIUS      = 4;
    private static final int SIZE        = 10;
    /** Bounces a shell makes before it flies out or bursts; a Ricochet crate's shells make more. */
    public static final int  RICOCHET_BOUNCES = 3;
    private int              maxBounces  = 1;
    private double           x;
    private double           y;
    /** Where the shell was before its last move (the tank's center for a new shell), to find the wall it crossed. */
    private double           lastX;
    private double           lastY;
    private double           dx;
    private double           dy;
    private int              bounces;
    private boolean          bounced;
    private boolean          burst;
    private final Tank       owner;
    /** The shells of one Triple shot share a volley, which can hit a tank only once; null for a single shell. */
    private Object           volley;

    public Shell(Tank owner, double x, double y, double dx, double dy)
    {
        this.owner = owner;
        this.x = lastX = x;
        this.y = lastY = y;
        this.dx = dx;
        this.dy = dy;
    }

    /** Starts the first move's path at (x, y), so a shell fired with the barrel through a wall hits that wall. */
    void startFrom(double x, double y)
    {
        lastX = x;
        lastY = y;
    }

    /** Lets the shell bounce {@code bounces} times instead of once (Ricochet). */
    void setMaxBounces(int bounces)
    {
        maxBounces = bounces;
    }

    void setVolley(Object volley)
    {
        this.volley = volley;
    }

    public Object volley()
    {
        return volley;
    }

    /** Whether this shell bounces more than once. */
    public boolean ricochet()
    {
        return maxBounces > 1;
    }

    /** Moves one tick on a field with no walls. */
    public boolean move(int width, int height)
    {
        return move(width, height, Arena.EMPTY);
    }

    /** Moves one tick; returns false once the shell has left the field for good or burst on a wall. */
    public boolean move(int width, int height, Arena arena)
    {
        bounced = false;
        double fromX = lastX;
        double fromY = lastY;
        x += dx * SPEED;
        y += dy * SPEED;
        double[] hit = arena.firstHit(fromX, fromY, x, y, RADIUS);
        if (hit != null)
        {
            // Back to where it touched the wall, a hair outside, then bounce off that side or burst.
            double t = Math.max(0, hit[0] - 0.01);
            x = fromX + (x - fromX) * t;
            y = fromY + (y - fromY) * t;
            if (bounces >= maxBounces || hit[1] < 0)
            {
                burst = true;
                return false;
            }
            if (hit[1] == 0) dx = -dx;
            else dy = -dy;
            bounces++;
            bounced = true;
        }
        else if (bounces < maxBounces)
        {
            if (x < 0 || x > width)
            {
                x = x < 0 ? -x : 2 * width - x;
                dx = -dx;
                bounces++;
                bounced = true;
            }
            else if (y < 0 || y > height)
            {
                y = y < 0 ? -y : 2 * height - y;
                dy = -dy;
                bounces++;
                bounced = true;
            }
        }
        lastX = x;
        lastY = y;
        return x >= -SIZE && x <= width + SIZE && y >= -SIZE && y <= height + SIZE;
    }

    /** A shell never hurts the tank that fired it. */
    public boolean hits(Tank tank)
    {
        return tank != owner && tank.isAlive() && tank.contains(x, y);
    }

    /** Whether the last move bounced the shell off the edge or a wall. */
    public boolean bounced()
    {
        return bounced;
    }

    /** Whether the shell ended on a wall (rather than flying out). */
    public boolean burst()
    {
        return burst;
    }

    public Tank owner()
    {
        return owner;
    }

    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    public double dx()
    {
        return dx;
    }

    public double dy()
    {
        return dy;
    }

    /** A glowing round with a short tracer behind it in its tank's color. */
    public void paint(Graphics2D g)
    {
        Color c = owner.color();
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
        g.drawLine((int) (x - dx * 22), (int) (y - dy * 22), (int) x, (int) y);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 236, 170, 150));
        g.drawLine((int) (x - dx * 10), (int) (y - dy * 10), (int) x, (int) y);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(40, 30, 20));
        g.fillOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        g.setColor(new Color(255, 230, 150));
        g.fillOval((int) x - SIZE / 2 + 2, (int) y - SIZE / 2 + 2, SIZE - 4, SIZE - 4);
        if (ricochet())
        {
            Color r = Supply.Power.RICOCHET.color;
            g.setColor(new Color(r.getRed(), r.getGreen(), r.getBlue(), 180));
            g.setStroke(new BasicStroke(2));
            g.drawOval((int) x - SIZE / 2 - 3, (int) y - SIZE / 2 - 3, SIZE + 6, SIZE + 6);
            g.setStroke(new BasicStroke(1));
        }
    }
}
