package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class Tank
{
    /** Half the width of the square body. */
    public static final int  SIZE         = 50;
    /** Pixels moved, or degrees turned, per tick while a key is held. */
    public static final int  SPEED        = SIZE / 10;
    public static final int  MAX_HEALTH   = 3;
    /** Ticks between shots (about 0.4 s). */
    public static final int  RELOAD_TICKS = 25;
    private static final int FLASH_TICKS  = 10;
    private static final int KNOCKBACK    = 8;
    private static final Color WRECK      = new Color(60, 60, 70);
    private final Color      tankColor;
    private final int        startX;
    private final int        startY;
    private final int        startRotation;
    private String           name;
    private Controls         controls;
    private int              tankRotation;
    private double           tankX;
    private double           tankY;
    private double           velocityX;
    private double           velocityY;
    private int              health;
    private int              reload;
    private int              flash;

    /**
     * @param x
     *            starting center X
     * @param y
     *            starting center Y
     * @param rotation
     *            starting direction in degrees (0 is up, 90 is right)
     */
    public Tank(String name, Color tankColor, Controls controls, int x, int y, int rotation)
    {
        this.name = name;
        this.tankColor = tankColor;
        this.controls = controls;
        this.startX = x;
        this.startY = y;
        this.startRotation = rotation;
        reset();
    }

    /** Back to the starting spot with full health, for a new round. */
    public void reset()
    {
        tankX = startX;
        tankY = startY;
        tankRotation = startRotation;
        velocityX = velocityY = 0;
        health = MAX_HEALTH;
        reload = 0;
        flash = 0;
    }

    /**
     * @param forward
     *            should be true if the tank should be moved forward and false
     *            if the tank should be moved in the reverse direction.
     */
    public void moveTank(boolean forward)
    {
        int direction = forward ? 1 : -1;
        tankX += Math.sin(Math.toRadians(tankRotation)) * SPEED * direction;
        tankY -= Math.cos(Math.toRadians(tankRotation)) * SPEED * direction;
    }

    /**
     * @param counterClockwise
     *            should be true if the tank should be rotated counterclockwise
     *            and false if the tank should be rotated clockwise.
     */
    public void rotateTank(boolean counterClockwise)
    {
        tankRotation = Math.floorMod(tankRotation + (counterClockwise ? -SPEED : SPEED), 360);
    }

    /**
     * fires the cannon: a shell leaves the end of the barrel
     */
    public void fireTank(List<Shell> shells)
    {
        double dx = Math.sin(Math.toRadians(tankRotation));
        double dy = -Math.cos(Math.toRadians(tankRotation));
        int barrelEnd = SIZE * 3 / 2;
        shells.add(new Shell(this, tankX + dx * barrelEnd, tankY + dy * barrelEnd, dx, dy));
        reload = RELOAD_TICKS;
    }

    /**
     * One game tick: applies the controls, keeps the tank inside the field and off the other tank, and fires on a new
     * fire press once reloaded.
     */
    public void update(int width, int height, List<Shell> shells, Tank other)
    {
        double oldX = tankX;
        double oldY = tankY;
        if (controls.forward()) moveTank(true);
        if (controls.left()) rotateTank(true);
        if (controls.back()) moveTank(false);
        if (controls.right()) rotateTank(false);
        clamp(width, height);
        // Tanks are solid: a move that brings them closer while they touch is undone.
        if (distanceTo(other) < 2 * SIZE && distanceTo(other) < Math.hypot(other.tankX - oldX, other.tankY - oldY))
        {
            tankX = oldX;
            tankY = oldY;
        }
        velocityX = tankX - oldX;
        velocityY = tankY - oldY;
        if (reload > 0) reload--;
        if (controls.fire() && reload == 0) fireTank(shells);
    }

    /** Fades the hit flash; runs every tick, also between rounds. */
    public void fadeFlash()
    {
        if (flash > 0) flash--;
    }

    private void clamp(int width, int height)
    {
        tankX = Math.max(SIZE, Math.min(width - SIZE, tankX));
        tankY = Math.max(SIZE, Math.min(height - SIZE, tankY));
    }

    /** Takes one hit from a shell, which knocks the tank back along the shell's path. */
    public void hit(Shell shell, int width, int height)
    {
        health = Math.max(0, health - 1);
        flash = FLASH_TICKS;
        tankX += shell.dx() * KNOCKBACK;
        tankY += shell.dy() * KNOCKBACK;
        clamp(width, height);
    }

    /** Whether the point lies on the tank's (turned) square body. */
    public boolean contains(double x, double y)
    {
        double angle = Math.toRadians(tankRotation);
        double dx = x - tankX;
        double dy = y - tankY;
        double along = dx * Math.cos(angle) + dy * Math.sin(angle);
        double across = -dx * Math.sin(angle) + dy * Math.cos(angle);
        return Math.abs(along) <= SIZE && Math.abs(across) <= SIZE;
    }

    public double distanceTo(Tank other)
    {
        return Math.hypot(other.tankX - tankX, other.tankY - tankY);
    }

    public boolean isAlive()
    {
        return health > 0;
    }

    public void paint(Graphics2D gr)
    {
        Graphics2D g = (Graphics2D) gr.create();
        g.translate(tankX, tankY);
        // The barrel is drawn along +x, which is 90 degrees clockwise of "up" (rotation 0).
        g.rotate(Math.toRadians(tankRotation - 90));
        g.setColor(!isAlive() ? WRECK : flash % 4 >= 2 ? Color.WHITE : tankColor);
        g.fillRect(-SIZE, -SIZE, 2 * SIZE, 2 * SIZE);
        g.setColor(new Color(0, 0, 0));
        g.drawRect(-SIZE, -SIZE, 2 * SIZE, 2 * SIZE);
        g.fillRect(SIZE / 2, -SIZE / 2, SIZE, SIZE);
        g.dispose();
    }

    public String name()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Color color()
    {
        return tankColor;
    }

    public Controls controls()
    {
        return controls;
    }

    public void setControls(Controls controls)
    {
        this.controls = controls;
    }

    public double x()
    {
        return tankX;
    }

    public double y()
    {
        return tankY;
    }

    public int rotation()
    {
        return tankRotation;
    }

    public double velocityX()
    {
        return velocityX;
    }

    public double velocityY()
    {
        return velocityY;
    }

    public int health()
    {
        return health;
    }

    public int reload()
    {
        return reload;
    }
}
