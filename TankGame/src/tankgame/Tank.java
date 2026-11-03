package tankgame;

import java.awt.BasicStroke;
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
    private static final Color WRECK      = new Color(58, 54, 50);
    private static final Color TREAD      = new Color(38, 36, 34);
    private static final Color CLEAT      = new Color(84, 80, 74);
    private static final Color STEEL      = new Color(62, 64, 66);
    /** How far the barrel kicks back when it fires, and for how many ticks it slides back out. */
    private static final int   RECOIL     = 10;
    private static final int   RECOIL_TICKS = 8;
    /** Supply crates: how long Ricochet and Turbo last (10 and 7 s), Turbo's speed, Triple shot's shots and spread. */
    public static final int    RICOCHET_TICKS = 600;
    public static final int    TURBO_TICKS  = 420;
    public static final int    TURBO_SPEED  = 8;
    public static final int    TRIPLE_SHOTS = 3;
    private static final int   SPREAD       = 12;
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
    /** How far each track has rolled, which moves the cleats drawn on it. */
    private double           leftTrack;
    private double           rightTrack;
    /** Powers from supply crates. */
    private boolean          shield;
    /** The Triple shot volley that last hit this tank. */
    private Object           lastVolley;
    private int              tripleShots;
    private int              ricochetTicks;
    private int              turboTicks;

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
        shield = false;
        tripleShots = ricochetTicks = turboTicks = 0;
    }

    /** Takes a supply crate's power. */
    public void give(Supply.Power power)
    {
        switch (power)
        {
            case REPAIR:
                health = Math.min(MAX_HEALTH, health + 1);
                break;
            case SHIELD:
                shield = true;
                break;
            case TRIPLE:
                tripleShots = TRIPLE_SHOTS;
                break;
            case RICOCHET:
                ricochetTicks = RICOCHET_TICKS;
                break;
            case TURBO:
                turboTicks = TURBO_TICKS;
                break;
        }
    }

    /**
     * @param forward
     *            should be true if the tank should be moved forward and false
     *            if the tank should be moved in the reverse direction.
     */
    public void moveTank(boolean forward)
    {
        int step = (forward ? 1 : -1) * (turboTicks > 0 ? TURBO_SPEED : SPEED);
        tankX += Math.sin(Math.toRadians(tankRotation)) * step;
        tankY -= Math.cos(Math.toRadians(tankRotation)) * step;
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
     * fires the cannon: a shell leaves the end of the barrel (with Triple shot, three in a fan, the middle one first)
     */
    public void fireTank(List<Shell> shells)
    {
        int[] angles = tripleShots > 0 ? new int[] { 0, -SPREAD, SPREAD } : new int[] { 0 };
        if (tripleShots > 0) tripleShots--;
        Object volley = angles.length > 1 ? new Object() : null;
        for (int angle : angles)
        {
            double dx = Math.sin(Math.toRadians(tankRotation + angle));
            double dy = -Math.cos(Math.toRadians(tankRotation + angle));
            int barrelEnd = SIZE * 3 / 2;
            Shell shell = new Shell(this, tankX + dx * barrelEnd, tankY + dy * barrelEnd, dx, dy);
            shell.startFrom(tankX, tankY);
            shell.setVolley(volley);
            if (ricochetTicks > 0) shell.setMaxBounces(Shell.RICOCHET_BOUNCES);
            shells.add(shell);
        }
        reload = RELOAD_TICKS;
    }

    /** One game tick on a field with no walls. */
    public void update(int width, int height, List<Shell> shells, Tank other)
    {
        update(width, height, shells, other, Arena.EMPTY);
    }

    /**
     * One game tick: applies the controls, keeps the tank inside the field and off the other tank and the walls, and
     * fires on a new fire press once reloaded.
     */
    public void update(int width, int height, List<Shell> shells, Tank other, Arena arena)
    {
        double oldX = tankX;
        double oldY = tankY;
        int oldRotation = tankRotation;
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
        keepOffWalls(arena, oldX, oldY);
        double heading = Math.toRadians(oldRotation);
        double rolled = (tankX - oldX) * Math.sin(heading) - (tankY - oldY) * Math.cos(heading);
        double turned = Math.floorMod(tankRotation - oldRotation + 180, 360) - 180;
        leftTrack += rolled + turned * 0.6;
        rightTrack += rolled - turned * 0.6;
        velocityX = tankX - oldX;
        velocityY = tankY - oldY;
        if (reload > 0) reload--;
        if (ricochetTicks > 0) ricochetTicks--;
        if (turboTicks > 0) turboTicks--;
        if (controls.fire() && reload == 0) fireTank(shells);
    }

    /** Fades the hit flash; runs every tick, also between rounds. */
    public void fadeFlash()
    {
        if (flash > 0) flash--;
    }

    /** Undoes a move into a wall, keeping the part along the wall so the tank slides past it. */
    private void keepOffWalls(Arena arena, double oldX, double oldY)
    {
        if (!arena.blocks(tankX, tankY, SIZE)) return;
        if (!arena.blocks(oldX, tankY, SIZE)) tankX = oldX;
        else if (!arena.blocks(tankX, oldY, SIZE)) tankY = oldY;
        else
        {
            tankX = oldX;
            tankY = oldY;
        }
    }

    private void clamp(int width, int height)
    {
        tankX = Math.max(SIZE, Math.min(width - SIZE, tankX));
        tankY = Math.max(SIZE, Math.min(height - SIZE, tankY));
    }

    /** Takes one hit from a shell, which knocks the tank back along the shell's path. */
    public boolean hit(Shell shell, int width, int height)
    {
        return hit(shell, width, height, Arena.EMPTY);
    }

    /**
     * Takes one hit; the knockback stops at a wall. A shield stops the hit instead and is used up, and a second shell of a
     * Triple shot volley that already hit does nothing; returns whether the tank was damaged.
     */
    public boolean hit(Shell shell, int width, int height, Arena arena)
    {
        // A Triple shot's fan does one hit at most: the other shells of a volley that already hit burst harmlessly.
        if (shell.volley() != null && shell.volley() == lastVolley) return false;
        lastVolley = shell.volley();
        if (shield)
        {
            shield = false;
            return false;
        }
        health = Math.max(0, health - 1);
        flash = FLASH_TICKS;
        double oldX = tankX;
        double oldY = tankY;
        tankX += shell.dx() * KNOCKBACK;
        tankY += shell.dy() * KNOCKBACK;
        clamp(width, height);
        keepOffWalls(arena, oldX, oldY);
        return true;
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

    /**
     * Draws the tank from above: its shadow, two tracks whose cleats roll as it drives and turns, the hull, and a
     * turret whose barrel kicks back when it fires. Damage shows as scorch marks; a destroyed tank is a charred wreck.
     */
    public void paint(Graphics2D gr)
    {
        Graphics2D g = (Graphics2D) gr.create();
        g.translate(tankX + 5, tankY + 7);
        // The barrel is drawn along +x, which is 90 degrees clockwise of "up" (rotation 0).
        g.rotate(Math.toRadians(tankRotation - 90));
        g.setColor(new Color(0, 0, 0, 70));
        g.fillRoundRect(-SIZE, -SIZE, 2 * SIZE, 2 * SIZE, 20, 20);
        g.dispose();
        g = (Graphics2D) gr.create();
        g.translate(tankX, tankY);
        g.rotate(Math.toRadians(tankRotation - 90));
        Color body = !isAlive() ? WRECK : tankColor;
        // Local +y is the tank's right side.
        track(g, -SIZE, leftTrack);
        track(g, SIZE - 22, rightTrack);
        g.setColor(shade(body, 0.62));
        g.fillRoundRect(-44, -31, 88, 62, 14, 14);
        g.setColor(body);
        g.fillRoundRect(-40, -27, 80, 54, 12, 12);
        g.setColor(shade(body, 1.25));
        g.fillRoundRect(-36, -24, 70, 10, 8, 8);
        // Engine grille at the back, a sloped plate at the front.
        g.setColor(shade(body, 0.7));
        for (int i = 0; i < 4; i++) g.fillRect(-36 + i * 6, -12, 3, 26);
        g.fillRect(30, -24, 6, 48);
        if (health < MAX_HEALTH)
        {
            g.setColor(new Color(20, 16, 12, isAlive() ? 120 : 170));
            g.fillOval(8, 6, 22, 14);
            if (health < MAX_HEALTH - 1) g.fillOval(-30, -22, 18, 16);
            if (!isAlive()) g.fillOval(-6, -26, 30, 20);
        }
        int recoil = isAlive() ? Math.max(0, reload - (RELOAD_TICKS - RECOIL_TICKS)) * RECOIL / RECOIL_TICKS : 0;
        Color steel = isAlive() ? STEEL : shade(WRECK, 0.8);
        g.setColor(steel);
        g.fillRect(12 - recoil, -6, 54, 12);
        g.setColor(shade(steel, 0.7));
        g.fillRect(64 - recoil, -8, 12, 16);
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(12 - recoil, -5, 52, 3);
        g.setColor(shade(body, 0.55));
        g.fillOval(-23, -23, 46, 46);
        g.setColor(shade(body, 1.08));
        g.fillOval(-20, -21, 40, 40);
        g.setColor(shade(body, 1.3));
        g.fillOval(-14, -17, 18, 12);
        g.setColor(shade(body, 0.5));
        g.fillOval(-12, 2, 13, 13);
        if (flash % 4 >= 2)
        {
            g.setColor(new Color(255, 255, 255, 190));
            g.fillRoundRect(-SIZE, -SIZE, 2 * SIZE, 2 * SIZE, 20, 20);
        }
        g.dispose();
        if (shield && isAlive())
        {
            // A see-through bubble round the tank, with a bright rim and a highlight.
            int r = SIZE + 22;
            Color c = Supply.Power.SHIELD.color;
            gr.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
            gr.fillOval((int) tankX - r, (int) tankY - r, 2 * r, 2 * r);
            gr.setStroke(new BasicStroke(3));
            gr.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
            gr.drawOval((int) tankX - r, (int) tankY - r, 2 * r, 2 * r);
            gr.setColor(new Color(255, 255, 255, 120));
            gr.drawArc((int) tankX - r + 10, (int) tankY - r + 10, 2 * r - 20, 2 * r - 20, 110, 50);
            gr.setStroke(new BasicStroke(1));
        }
    }

    /** One track along the side at {@code top}, with cleats placed by how far it has rolled. */
    private static void track(Graphics2D g, int top, double rolled)
    {
        g.setColor(TREAD);
        g.fillRoundRect(-SIZE, top, 2 * SIZE, 22, 14, 14);
        g.setColor(CLEAT);
        g.setStroke(new BasicStroke(3));
        int offset = Math.floorMod((int) Math.round(rolled), 10);
        for (int x = -SIZE + 4 + offset; x < SIZE - 3; x += 10) g.drawLine(x, top + 3, x, top + 19);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(0, 0, 0, 90));
        g.drawRoundRect(-SIZE, top, 2 * SIZE, 22, 14, 14);
    }

    /** The color made darker (factor below 1) or lighter (above 1, mixed toward white). */
    static Color shade(Color c, double factor)
    {
        if (factor <= 1)
            return new Color((int) (c.getRed() * factor), (int) (c.getGreen() * factor), (int) (c.getBlue() * factor));
        double f = factor - 1;
        return new Color((int) (c.getRed() + (255 - c.getRed()) * f), (int) (c.getGreen() + (255 - c.getGreen()) * f),
                        (int) (c.getBlue() + (255 - c.getBlue()) * f));
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

    public boolean shielded()
    {
        return shield;
    }

    public int tripleShots()
    {
        return tripleShots;
    }

    public int ricochetTicks()
    {
        return ricochetTicks;
    }

    public int turboTicks()
    {
        return turboTicks;
    }
}
