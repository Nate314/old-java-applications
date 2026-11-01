package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;

/** A fired shell that flies in a straight line until it leaves the field. */
public class Shell
{
    private static final int SPEED = 10;
    private static final int SIZE  = 10;
    private double           x;
    private double           y;
    private final double     dx;
    private final double     dy;
    private final Color      color;

    public Shell(double x, double y, double dx, double dy, Color color)
    {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.color = color;
    }

    /** Moves one tick; returns false once the shell is outside the field. */
    public boolean move(int width, int height)
    {
        x += dx * SPEED;
        y += dy * SPEED;
        return x >= -SIZE && x <= width + SIZE && y >= -SIZE && y <= height + SIZE;
    }

    public void paint(Graphics2D g)
    {
        g.setColor(color);
        g.fillOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
        g.setColor(Color.BLACK);
        g.drawOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }
}
