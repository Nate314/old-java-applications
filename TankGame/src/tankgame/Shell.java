package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;

/** A fired shell: flies straight, bounces off the edge of the field once, and then flies out. */
public class Shell
{
    public static final int  SPEED       = 10;
    private static final int SIZE        = 10;
    private static final int MAX_BOUNCES = 1;
    private double           x;
    private double           y;
    private double           dx;
    private double           dy;
    private int              bounces;
    private final Tank       owner;

    public Shell(Tank owner, double x, double y, double dx, double dy)
    {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    /** Moves one tick; returns false once the shell has left the field for good. */
    public boolean move(int width, int height)
    {
        x += dx * SPEED;
        y += dy * SPEED;
        if (bounces < MAX_BOUNCES)
        {
            if (x < 0 || x > width)
            {
                x = x < 0 ? -x : 2 * width - x;
                dx = -dx;
                bounces++;
            }
            else if (y < 0 || y > height)
            {
                y = y < 0 ? -y : 2 * height - y;
                dy = -dy;
                bounces++;
            }
        }
        return x >= -SIZE && x <= width + SIZE && y >= -SIZE && y <= height + SIZE;
    }

    /** A shell never hurts the tank that fired it. */
    public boolean hits(Tank tank)
    {
        return tank != owner && tank.isAlive() && tank.contains(x, y);
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

    public void paint(Graphics2D g)
    {
        g.setColor(owner.color());
        g.fillOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        g.setColor(Color.BLACK);
        g.drawOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }
}
