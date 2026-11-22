package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Runs the battle on a Swing timer and paints the battlefield (its ground, see Terrain, which keeps the round's track marks and
 * blast scorches, and walls of the ground's material), what the tracks throw up, the supply crates, the tanks, the shells,
 * the sparks and smoke, the ground's weather, the score bar with each tank's powers, and the banners.
 */
@SuppressWarnings("serial")
public class GamePanel extends JPanel
{
    private static final int   TICK_MILLIS = 16;
    private static final int   SLOW_FRAMES = 66;
    /** The ground is drawn this many times finer than the field, so it stays sharp when the page zooms the window. */
    private static final int   GROUND_SCALE = 2;
    private static final Color SHADE       = new Color(0, 0, 40, 215);
    private static final Color TEXT        = Color.WHITE;
    private static final Color DIM         = new Color(200, 205, 255);
    private static final Font  BIG         = new Font(Font.SANS_SERIF, Font.BOLD, 34);
    private static final Font  MEDIUM      = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font  SMALL       = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private static final Font  CHIP        = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static final Color CRATE       = new Color(240, 200, 140);
    private static final Color CONCRETE_TEXT = new Color(215, 212, 205);
    private final Battle       battle;
    private final Timer        timer       = new Timer(TICK_MILLIS, e -> tick());
    /** The round's ground, with every track mark and scorch so far; made again for each battlefield. */
    private BufferedImage      ground;
    private Arena              groundArena;
    private Terrain            terrain     = Terrain.MEADOW;
    /** Each tank's x, y and rotation when its tracks last marked the ground. */
    private final double[][]   lastPose    = new double[2][];
    /** Dust, snow and clods the tracks have thrown up (only drawn: the battle never sees them). */
    private final List<Spark>  kicks       = new ArrayList<>();
    private final Random       kickRandom  = new Random();
    private Weather            weather;
    private int                ticks;
    /** Frames left of the slow motion that plays a destroyed tank's blast, and the blasts seen so far. */
    private int                slowFrames;
    private int                blastsSeen;
    private final Random       shakeRandom = new Random();
    /** The lines and buttons a click or tap acts on, where they were last drawn, and what each does. */
    private final Map<Rectangle, Runnable> taps = new LinkedHashMap<>();
    /** Building: whether the press that started this drag lays walls (or lifts them). */
    private boolean            laying;
    /** Building: where the pointer was at the last press or drag event. */
    private int                lastX;
    private int                lastY;

    public GamePanel(Battle battle)
    {
        this.battle = battle;
        this.setBackground(Terrain.MEADOW.base);
        // A click (or a tap, on a touch screen) on a button does what it says: 1 player or 2 players, or a setting
        // (crates, sound, battlefield, watch) like its key. While building it lays or lifts walls. Anywhere else it plays red with
        // the pointer (PointerControls), or starts a match against the computer, like Q.
        MouseAdapter mouse = new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                for (Map.Entry<Rectangle, Runnable> tap : taps.entrySet())
                    if (tap.getKey().contains(e.getX(), e.getY()))
                    {
                        tap.getValue().run();
                        repaint();
                        return;
                    }
                if (building())
                {
                    int[] cell = cellAt(e.getX(), e.getY());
                    laying = cell != null && !battle.cell(cell[0], cell[1]);
                    battle.newStroke();
                    lastX = e.getX();
                    lastY = e.getY();
                    build(e.getX(), e.getY());
                    return;
                }
                PointerControls pointer = pointer();
                if (pointer == null) return;
                if (battle.phase() == Battle.Phase.TITLE || battle.phase() == Battle.Phase.MATCH_OVER) pointer.tapToStart();
                else pointer.press(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                if (building())
                {
                    build(e.getX(), e.getY());
                    return;
                }
                PointerControls pointer = pointer();
                if (pointer != null) pointer.drag(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                if (building()) return;
                PointerControls pointer = pointer();
                if (pointer != null) pointer.release(e.getX(), e.getY());
            }
        };
        this.addMouseListener(mouse);
        this.addMouseMotionListener(mouse);
    }

    private boolean building()
    {
        return battle.phase() == Battle.Phase.BUILD;
    }

    /** The grid cell (column, row) under a point of the field, or null outside it. */
    private int[] cellAt(int x, int y)
    {
        if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) return null;
        return new int[] { x * Arena.GRID / getWidth(), y * Arena.GRID / getHeight() };
    }

    /**
     * Lays (or lifts, if the drag began on a wall) the walls in the cells from the last pointer spot to this one, so a
     * quick drag (whose events come far apart) leaves no gaps.
     */
    private void build(int x, int y)
    {
        int steps = Math.max(1, (int) Math.hypot(x - lastX, y - lastY) / 4);
        for (int i = 1; i <= steps; i++)
        {
            int[] cell = cellAt(lastX + (x - lastX) * i / steps, lastY + (y - lastY) * i / steps);
            if (cell != null) battle.setCell(cell[0], cell[1], laying, getWidth(), getHeight());
        }
        lastX = x;
        lastY = y;
        repaint();
    }

    /**
     * The builder: the grid over the field, the cells beside a tank shaded red (no wall goes there), a strip along the
     * top saying what to do and buttons along the bottom. Both strips let the walls under them show through.
     */
    private void builder(Graphics2D g)
    {
        int w = getWidth();
        int h = getHeight();
        for (int row = 0; row < Arena.GRID; row++)
            for (int col = 0; col < Arena.GRID; col++)
            {
                int l = col * w / Arena.GRID;
                int t = row * h / Arena.GRID;
                int cw = (col + 1) * w / Arena.GRID - l;
                int ch = (row + 1) * h / Arena.GRID - t;
                if (!Arena.cellFree(col, row, w, h, battle.red(), battle.green()))
                {
                    g.setColor(new Color(200, 40, 30, 55));
                    g.fillRect(l, t, cw, ch);
                }
                g.setColor(new Color(255, 255, 255, 45));
                g.drawRect(l, t, cw, ch);
            }
        g.setColor(new Color(0, 0, 40, 160));
        g.fillRect(0, 0, w, 46);
        g.fillRect(0, h - 46, w, 46);
        g.setColor(TEXT);
        g.setFont(MEDIUM);
        String title = "BUILD YOUR BATTLEFIELD";
        g.drawString(title, (w - g.getFontMetrics().stringWidth(title)) / 2, 20);
        g.setColor(battle.refused() ? Color.YELLOW : DIM);
        g.setFont(SMALL);
        String help = battle.refused() ? "No: then the tanks could not reach each other"
                        : "Tap or drag to lay walls, again to lift them";
        g.drawString(help, (w - g.getFontMetrics().stringWidth(help)) / 2, 39);
        int gap = 8;
        int bw = (w - gap * 4) / 3;
        button(g, new Rectangle(gap, h - 39, bw, 32), "Clear", battle::clearField);
        button(g, new Rectangle(gap * 2 + bw, h - 39, bw, 32), "Fight here (Q)", () -> battle.fightHere(true));
        button(g, new Rectangle(gap * 3 + bw * 2, h - 39, bw, 32), "Done (B)", battle::toggleBuild);
    }

    private void button(Graphics2D g, Rectangle box, String label, Runnable action)
    {
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRoundRect(box.x, box.y, box.width, box.height, 10, 10);
        g.setColor(DIM);
        g.drawRoundRect(box.x, box.y, box.width, box.height, 10, 10);
        g.setColor(TEXT);
        g.setFont(SMALL);
        g.drawString(label, box.x + (box.width - g.getFontMetrics().stringWidth(label)) / 2, box.y + 21);
        taps.put(box, action);
    }

    /** Red's pointer controls, or null while the computer drives red (watch). */
    private PointerControls pointer()
    {
        Controls controls = battle.red().controls();
        return controls instanceof PointerControls ? (PointerControls) controls : null;
    }

    /** Where the pointer sends red: a ring it drives to while held, or cross hairs it is turning to fire at. */
    private void pointerMark(Graphics2D g)
    {
        PointerControls pointer = pointer();
        double[] goal = pointer == null || battle.phase() != Battle.Phase.FIGHT ? null : pointer.goal();
        if (goal == null || !battle.red().isAlive()) return;
        int x = (int) Math.round(goal[0]);
        int y = (int) Math.round(goal[1]);
        int r = pointer.holding() ? 12 + (ticks / 3) % 6 : 10;
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(0, 0, 0, 90));
        g.drawOval(x - r + 1, y - r + 2, r * 2, r * 2);
        g.setColor(light(battle.red()));
        g.drawOval(x - r, y - r, r * 2, r * 2);
        if (!pointer.holding())
        {
            g.drawLine(x - r - 5, y, x - r + 5, y);
            g.drawLine(x + r - 5, y, x + r + 5, y);
            g.drawLine(x, y - r - 5, x, y - r + 5);
            g.drawLine(x, y + r - 5, x, y + r + 5);
        }
        g.setStroke(new BasicStroke(1f));
    }

    /** A player's aiming line while they fight; none for a tank the computer drives. */
    private Tank.Sight sight(Tank tank, Tank other)
    {
        if (battle.phase() != Battle.Phase.FIGHT || !tank.isAlive() || !other.isAlive()
                        || tank.controls() instanceof ComputerControls) return null;
        return tank.sight(getWidth(), getHeight(), battle.arena(), other, battle.supply());
    }

    /** Dots along the shell's path that fade with distance, a ring at each bounce; fainter while reloading. */
    private void sightLine(Graphics2D g, Tank tank, Tank.Sight sight)
    {
        if (sight == null) return;
        Color c = light(tank);
        double strength = tank.reload() > 0 ? 0.45 : 1;
        double travelled = 0;
        double next = 0;
        for (int i = 1; i < sight.points.size(); i++)
        {
            double[] a = sight.points.get(i - 1);
            double[] b = sight.points.get(i);
            double length = Math.hypot(b[0] - a[0], b[1] - a[1]);
            for (; next <= travelled + length; next += 13)
            {
                double t = length == 0 ? 0 : (next - travelled) / length;
                int x = (int) Math.round(a[0] + (b[0] - a[0]) * t);
                int y = (int) Math.round(a[1] + (b[1] - a[1]) * t);
                int alpha = (int) (strength * Math.max(70, 235 - next * 0.12));
                g.setColor(new Color(0, 0, 0, alpha / 2));
                g.fillOval(x - 3, y - 2, 7, 7);
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                g.fillOval(x - 3, y - 3, 6, 6);
            }
            travelled += length;
            if (i < sight.points.size() - 1)
            {
                // A bounce: a small ring where the shell glances off.
                int alpha = (int) (strength * Math.max(90, 235 - travelled * 0.12));
                g.setStroke(new BasicStroke(2f));
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                g.drawOval((int) Math.round(b[0]) - 6, (int) Math.round(b[1]) - 6, 12, 12);
                g.setStroke(new BasicStroke(1f));
            }
        }
    }

    /** Corner brackets round the tank a shell fired now would hit, pulsing once the gun is loaded. */
    private void onTarget(Graphics2D g, Tank tank, Tank.Sight sight)
    {
        if (sight == null || sight.target == null) return;
        boolean loaded = tank.reload() == 0;
        int r = Tank.SIZE + 10 + (loaded ? (ticks / 4) % 4 : 0);
        int x = (int) Math.round(sight.target.x());
        int y = (int) Math.round(sight.target.y());
        Color c = light(tank);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int pass = 0; pass < 2; pass++)
        {
            g.setColor(pass == 0 ? new Color(0, 0, 0, 110)
                            : new Color(c.getRed(), c.getGreen(), c.getBlue(), loaded ? 230 : 120));
            int o = pass == 0 ? 1 : 0;
            for (int sx = -1; sx <= 1; sx += 2)
                for (int sy = -1; sy <= 1; sy += 2)
                {
                    int cx = x + sx * r + o;
                    int cy = y + sy * r + o;
                    g.drawLine(cx, cy, cx - sx * 14, cy);
                    g.drawLine(cx, cy, cx, cy - sy * 14);
                }
        }
        g.setStroke(new BasicStroke(1f));
    }

    public void start()
    {
        timer.start();
    }

    /** True while the browser page asks for less motion (the web shim's property; null, so false, on the desktop). */
    private static boolean calm()
    {
        return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
    }

    /** Moves everything one step on the Swing event thread, so speed does not depend on the paint rate. */
    private void tick()
    {
        if (slowFrames > 0)
        {
            // The blast that destroys a tank plays at a third of the speed, then a half, then full speed again.
            slowFrames--;
            // With reduced motion the blast plays at full speed (the banner still waits for it).
            int every = calm() ? 1 : slowFrames > 30 ? 3 : slowFrames > 12 ? 2 : 1;
            if (slowFrames % every != 0)
            {
                repaint();
                return;
            }
        }
        battle.tick(getWidth(), getHeight());
        if (battle.blasts() != blastsSeen)
        {
            blastsSeen = battle.blasts();
            slowFrames = SLOW_FRAMES;
        }
        ticks++;
        if (getWidth() > 0 && getHeight() > 0)
        {
            if (battle.arena() != groundArena) makeGround();
            Graphics2D g = ground.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(GROUND_SCALE, GROUND_SCALE);
            markTracks(g, battle.red(), 0);
            markTracks(g, battle.green(), 1);
            for (double[] scorch : battle.scorches()) scorch(g, scorch[0], scorch[1], scorch[2]);
            g.dispose();
            kicks.removeIf(kick -> !kick.move());
            weather.tick(getWidth(), getHeight());
        }
        repaint();
    }

    /** Lays fresh ground for the battlefield, in its own terrain (Terrain). */
    private void makeGround()
    {
        groundArena = battle.arena();
        terrain = Terrain.of(groundArena);
        lastPose[0] = lastPose[1] = null;
        int width = getWidth();
        int height = getHeight();
        ground = new BufferedImage(width * GROUND_SCALE, height * GROUND_SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ground.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(GROUND_SCALE, GROUND_SCALE);
        terrain.lay(g, width, height, new Random(groundArena.name().hashCode()));
        g.dispose();
        kicks.clear();
        weather = new Weather(terrain, width, height);
    }

    /** Presses the back of each track into the ground when the tank has moved or turned since last time. */
    private void markTracks(Graphics2D g, Tank tank, int index)
    {
        double[] pose = { tank.x(), tank.y(), tank.rotation() };
        double[] last = lastPose[index];
        lastPose[index] = pose;
        if (last == null || battle.phase() == Battle.Phase.TITLE || !tank.isAlive()
                        || (last[0] == pose[0] && last[1] == pose[1] && last[2] == pose[2]))
            return;
        Graphics2D t = (Graphics2D) g.create();
        t.translate(tank.x(), tank.y());
        t.rotate(Math.toRadians(tank.rotation() - 90));
        for (int top : new int[] { -Tank.SIZE + 2, Tank.SIZE - 20 })
        {
            Color c = terrain.track;
            t.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 26));
            t.fillRect(-Tank.SIZE + 2, top, 6, 18);
            if (ticks % 2 == 0)
            {
                t.setColor(new Color(c.getRed() * 3 / 4, c.getGreen() * 3 / 4, c.getBlue() * 3 / 4, 36));
                t.fillRect(-Tank.SIZE + 3, top + 1, 2, 16);
            }
        }
        t.dispose();
        if (ticks % 2 == 0) kick(pose, last);
    }

    /** Each track throws up a little of the ground (Terrain.kick) behind it, or ahead of it when backing. */
    private void kick(double[] pose, double[] last)
    {
        double fx = Math.sin(Math.toRadians(pose[2]));
        double fy = -Math.cos(Math.toRadians(pose[2]));
        double back = (pose[0] - last[0]) * fx + (pose[1] - last[1]) * fy < 0 ? 1 : -1;
        for (int side = -1; side <= 1; side += 2)
        {
            double x = pose[0] + fx * back * (Tank.SIZE + 2) - fy * side * (Tank.SIZE - 11);
            double y = pose[1] + fy * back * (Tank.SIZE + 2) + fx * side * (Tank.SIZE - 11);
            terrain.kick(kicks, x, y, fx * back * 2.5, fy * back * 2.5, kickRandom);
        }
    }

    /** A blackened ring of soot where something blew up, darkest in the middle. */
    private static void scorch(Graphics2D g, double x, double y, double radius)
    {
        for (int i = 0; i < 4; i++)
        {
            double r = radius * (1 - i * 0.22);
            g.setColor(new Color(30, 24, 16, 40 + i * 12));
            g.fillOval((int) (x - r), (int) (y - r * 0.85), (int) (2 * r), (int) (1.7 * r));
        }
    }

    @Override
    protected void paintComponent(Graphics gr)
    {
        super.paintComponent(gr);
        taps.clear();
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // Hits and blasts shake the field and everything on it; the score bar and banners hold still.
        double shake = calm() ? 0 : battle.shake();
        int shakeX = (int) Math.round((shakeRandom.nextDouble() * 2 - 1) * shake);
        int shakeY = (int) Math.round((shakeRandom.nextDouble() * 2 - 1) * shake);
        if (shake > 0)
        {
            // The strip the shaken ground uncovers is the same ground, also in pictures of the canvas (replay, photo).
            g.setColor(terrain.base);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.translate(shakeX, shakeY);
        if (ground != null) g.drawImage(ground, 0, 0, getWidth(), getHeight(), null);
        for (Arena.Wall wall : battle.arena().walls()) terrain.wall(g, wall);
        for (Spark kick : kicks) kick.paint(g);
        // A landed crate lies under the tanks; a falling one is above everything on the ground.
        Supply supply = battle.supply();
        if (supply != null && supply.landed()) supply.paint(g, ticks);
        pointerMark(g);
        Tank.Sight redSight = sight(battle.red(), battle.green());
        Tank.Sight greenSight = sight(battle.green(), battle.red());
        sightLine(g, battle.red(), redSight);
        sightLine(g, battle.green(), greenSight);
        battle.red().paint(g);
        battle.green().paint(g);
        onTarget(g, battle.red(), redSight);
        onTarget(g, battle.green(), greenSight);
        if (supply != null && !supply.landed()) supply.paint(g, ticks);
        for (Shell shell : battle.shells()) shell.paint(g);
        for (Spark spark : battle.sparks()) spark.paint(g);
        g.translate(-shakeX, -shakeY);
        // The air holds still while the field shakes, and stays clear when the browser asks for less motion.
        if (weather != null && !calm()) weather.paint(g);
        // The match-over box shows the score where the score bars would be.
        if (battle.phase() != Battle.Phase.TITLE && battle.phase() != Battle.Phase.MATCH_OVER && !building())
        {
            scoreBar(g, battle.red(), battle.redWins(), false);
            scoreBar(g, battle.green(), battle.greenWins(), true);
            notice(g);
        }
        banner(g);
        if (building()) builder(g);
        // On the title card and match-over screen the Watch button says so instead.
        if (battle.watching() && battle.phase() != Battle.Phase.TITLE && battle.phase() != Battle.Phase.MATCH_OVER)
            watchNote(g);
    }

    /** A tank's name, its health and the rounds it has won, in the top left (red) or top right (green) corner. */
    private void scoreBar(Graphics2D g, Tank tank, int wins, boolean rightAligned)
    {
        g.setFont(MEDIUM);
        String name = tank.name().toUpperCase();
        int nameWidth = g.getFontMetrics().stringWidth(name);
        int width = Math.max(nameWidth, Tank.MAX_HEALTH * 18 + 8 + Battle.ROUNDS_TO_WIN * 16);
        int x = rightAligned ? getWidth() - 12 - width : 12;
        g.setColor(SHADE);
        g.fillRoundRect(x - 6, 6, width + 12, 50, 10, 10);
        g.setColor(light(tank));
        g.drawString(name, rightAligned ? x + width - nameWidth : x, 26);
        int px = rightAligned ? x + width - (Tank.MAX_HEALTH * 18 + 8 + Battle.ROUNDS_TO_WIN * 16) : x;
        for (int i = 0; i < Tank.MAX_HEALTH; i++)
        {
            g.setColor(i < tank.health() ? tank.color() : new Color(255, 255, 255, 60));
            g.fillRect(px + i * 18, 34, 14, 14);
        }
        px += Tank.MAX_HEALTH * 18 + 8;
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < Battle.ROUNDS_TO_WIN; i++)
        {
            g.setColor(Color.YELLOW);
            if (i < wins) g.fillOval(px + i * 16, 35, 12, 12);
            else g.drawOval(px + i * 16, 35, 12, 12);
        }
        g.setStroke(new BasicStroke(1));
        powers(g, tank, rightAligned);
    }

    /** Chips under the score bar for the tank's powers from crates, with shots or seconds left. */
    private void powers(Graphics2D g, Tank tank, boolean rightAligned)
    {
        if (!tank.isAlive()) return;
        List<Supply.Power> powers = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        if (tank.shielded()) chip(powers, texts, Supply.Power.SHIELD, "SHIELD");
        if (tank.tripleShots() > 0) chip(powers, texts, Supply.Power.TRIPLE, "TRIPLE x" + tank.tripleShots());
        if (tank.ricochetTicks() > 0) chip(powers, texts, Supply.Power.RICOCHET, "RICOCHET " + seconds(tank.ricochetTicks()));
        if (tank.turboTicks() > 0) chip(powers, texts, Supply.Power.TURBO, "TURBO " + seconds(tank.turboTicks()));
        g.setFont(CHIP);
        // In a row on a wide field; stacked in the corner on a narrow one, so the two sides never meet.
        boolean stack = getWidth() < 440;
        int x = rightAligned ? getWidth() - 6 : 6;
        int y = 61;
        for (int i = 0; i < powers.size(); i++)
        {
            String text = texts.get(i);
            int w = g.getFontMetrics().stringWidth(text) + 12;
            int left = rightAligned ? x - w : x;
            g.setColor(powers.get(i).color);
            g.fillRoundRect(left, y, w, 17, 9, 9);
            g.setColor(new Color(25, 20, 15));
            g.drawString(text, left + 6, y + 12);
            if (stack) y += 20;
            else x = rightAligned ? left - 4 : left + w + 4;
        }
    }

    private static void chip(List<Supply.Power> powers, List<String> texts, Supply.Power power,
                    String text)
    {
        powers.add(power);
        texts.add(text);
    }

    private static String seconds(int ticks)
    {
        return (ticks + 59) / 60 + "s";
    }

    /** The power a tank just took, rising and fading over it: its name, and what it does. */
    private void notice(Graphics2D g)
    {
        Supply.Power power = battle.noticePower();
        int age = battle.noticeTicks();
        if (power == null || age >= Battle.NOTICE_TICKS) return;
        Tank tank = battle.noticeTank();
        // Full strength for the first two thirds, then fading out.
        int alpha = Math.min(255, 255 * (Battle.NOTICE_TICKS - age) * 3 / Battle.NOTICE_TICKS);
        int y = (int) (tank.y() - Tank.SIZE - 18 - age * 0.4);
        y = Math.max(96, y);
        outlined(g, power.label.toUpperCase(), MEDIUM, power.color, tank.x(), y, alpha);
        outlined(g, power.what, SMALL, Color.WHITE, tank.x(), y + 17, alpha);
    }

    /** Text centered on x with a dark outline, so it reads on grass and walls, kept inside the field. */
    private void outlined(Graphics2D g, String text, Font font, Color color, double centerX, int baseline, int alpha)
    {
        g.setFont(font);
        int w = g.getFontMetrics().stringWidth(text);
        int x = (int) Math.max(4, Math.min(getWidth() - w - 4, centerX - w / 2.0));
        g.setColor(new Color(0, 0, 0, alpha * 3 / 4));
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                if (dx != 0 || dy != 0) g.drawString(text, x + dx * 2, baseline + dy * 2);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g.drawString(text, x, baseline);
    }

    private void banner(Graphics2D g)
    {
        Tank red = battle.red();
        Tank green = battle.green();
        Tank winner = battle.winner();
        switch (battle.phase())
        {
            case TITLE:
                menu(g, new Line[] { new Line("TANK GAME", BIG, TEXT),
                                new Line("Three hits destroy a tank. First to " + Battle.ROUNDS_TO_WIN + " rounds wins.", SMALL, DIM),
                                new Line(battle.fieldBlocked() ? BLOCKED_NOTE : battle.hasField()
                                                ? "Every round is on your battlefield." : "Each round is a new battlefield.",
                                                SMALL, battle.fieldBlocked() ? Color.YELLOW : DIM), recordLine() }, true);
                controls(g);
                break;
            case ROUND_START:
                boolean go = battle.phaseTicks() > Battle.ROUND_START_TICKS - 25;
                panel(g, new Line[] { new Line(go ? "FIGHT!" : "ROUND " + battle.round(), BIG, go ? Color.YELLOW : TEXT),
                                new Line(battle.arena().name(), MEDIUM, TEXT),
                                new Line(Terrain.of(battle.arena()).where, SMALL, DIM),
                                new Line(battle.redWins() + " - " + battle.greenWins(), SMALL, DIM),
                                new Line(battle.fieldBlocked() ? BLOCKED_NOTE : "", SMALL, Color.YELLOW) });
                break;
            case ROUND_OVER:
                // The blast plays out in slow motion before the banner covers it.
                if (slowFrames > 0) break;
                panel(g, new Line[] { winner == null ? new Line("Both tanks destroyed", MEDIUM, TEXT)
                                : new Line(winner.name() + " wins the round", MEDIUM, light(winner)),
                                new Line(winner == null ? "No point: play the round again"
                                                : battle.redWins() + " - " + battle.greenWins(), SMALL, DIM) });
                break;
            case MATCH_OVER:
                String score = Math.max(battle.redWins(), battle.greenWins()) + " - "
                                + Math.min(battle.redWins(), battle.greenWins());
                String title = !battle.vsComputer() || battle.watching() ? winner.name() + " wins the match"
                                : winner == red ? "You beat the computer!" : "The computer wins";
                menu(g, new Line[] { new Line(title, MEDIUM, light(winner)), new Line(score, BIG, TEXT), recordLine() },
                                battle.phaseTicks() >= Battle.MATCH_OVER_TICKS);
                break;
            default:
                break;
        }
    }

    /** Watch: a line along the bottom of the field saying who drives and how to take over. */
    private void watchNote(Graphics2D g)
    {
        String text = "Computer vs computer. Press C to take red";
        g.setFont(SMALL);
        int w = g.getFontMetrics().stringWidth(text) + 20;
        int x = (getWidth() - w) / 2;
        int y = getHeight() - 34;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(x, y, w, 24, 12, 12);
        g.setColor(Color.YELLOW);
        g.drawString(text, x + 10, y + 17);
    }

    /** The tank's color mixed with white, so text in it reads well on the dark banners. */
    private static Color light(Tank tank)
    {
        Color c = tank.color();
        return new Color((c.getRed() + 255) / 2, (c.getGreen() + 255) / 2, (c.getBlue() + 255) / 2);
    }

    /** Said when the player's battlefield is set aside because this window is too narrow for it (Battle#fieldBlocked). */
    private static final String BLOCKED_NOTE = "Your battlefield has no way through at this size";

    /**
     * The title card and the match-over screen: the lines in a box along the top with the two ways to play under them,
     * and the settings along the bottom (each a key and a tap). Until the match-over pause ends only the lines show.
     */
    private void menu(Graphics2D g, Line[] lines, boolean ready)
    {
        // Top and bottom margins kept tight so a box with the record line still clears the tanks' top tracks.
        Rectangle box = panel(g, lines, 2, 380, ready ? 52 : 0);
        if (!ready) return;
        int gap = 10;
        int bw = (box.width - gap * 3) / 2;
        int by = box.y + box.height - 50;
        play(g, new Rectangle(box.x + gap, by, bw, 44), "1 player", "vs the computer: Q", battle.red(),
                        () -> battle.startMatch(true));
        play(g, new Rectangle(box.x + gap * 2 + bw, by, bw, 44), "2 players", "one keyboard: M", battle.green(),
                        () -> battle.startMatch(false));
        settings(g);
    }

    /** A big button in a tank's color: what it starts, and its key. */
    private void play(Graphics2D g, Rectangle b, String label, String sub, Tank tank, Runnable action)
    {
        Color c = tank.color();
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        g.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
        g.setStroke(new BasicStroke(2));
        g.setColor(light(tank));
        g.drawRoundRect(b.x, b.y, b.width, b.height, 12, 12);
        g.setStroke(new BasicStroke(1));
        centered(g, label, MEDIUM, TEXT, b, b.y + 20);
        centered(g, sub, SMALL, TEXT, b, b.y + 37);
        taps.put(b, action);
    }

    private static void centered(Graphics2D g, String text, Font font, Color color, Rectangle b, int baseline)
    {
        g.setFont(font);
        g.setColor(color);
        g.drawString(text, b.x + (b.width - g.getFontMetrics().stringWidth(text)) / 2, baseline);
    }

    /** Crates, sound, your battlefield and watch along the bottom: four in a row, or two by two when narrow. */
    private void settings(Graphics2D g)
    {
        String[] keys = { "X", "V", "B", "C" };
        String[] labels = { battle.supplies() ? "Crates on" : "Crates off", battle.sound() ? "Sound on" : "Sound off",
                        battle.hasField() ? "Your field" : "Build a field", battle.watching() ? "Watching" : "Watch" };
        Color[] colors = { CRATE, DIM, CONCRETE_TEXT, battle.watching() ? Color.YELLOW : DIM };
        Runnable[] actions = { battle::toggleSupplies, battle::toggleSound, battle::toggleBuild, battle::toggleWatch };
        int cols = getWidth() >= 440 ? 4 : 2;
        int gap = 8;
        int cw = (getWidth() - gap * (cols + 1)) / cols;
        int top = getHeight() - keys.length / cols * 36 - 4;
        for (int i = 0; i < keys.length; i++)
        {
            Rectangle b = new Rectangle(gap + i % cols * (cw + gap), top + i / cols * 36, cw, 30);
            g.setColor(SHADE);
            g.fillRoundRect(b.x, b.y, b.width, b.height, 10, 10);
            g.setColor(colors[i]);
            g.drawRoundRect(b.x, b.y, b.width, b.height, 10, 10);
            keycap(g, b.x + 5, b.y + 4, 22, keys[i], -1);
            g.setFont(SMALL);
            g.setColor(colors[i]);
            g.drawString(labels[i], b.x + 34, b.y + 20);
            taps.put(b, actions[i]);
        }
    }

    /** The record against the computer, or nothing (no line) before the first match against it. */
    private Line recordLine()
    {
        Record record = battle.record();
        return record != null && record.wins() + record.losses() > 0
                        ? new Line("Your record vs the computer: " + record, SMALL, Color.YELLOW) : null;
    }

    /** On the title card, each tank's keys under it: red's W A S D and Q (or the pointer), green's arrows and M. */
    private void controls(Graphics2D g)
    {
        keys(g, battle.red(), new String[] { "W", "A", "S", "D" }, "Q",
                        getWidth() >= 440 ? "or hold to drive, tap to fire" : "or touch the field");
        keys(g, battle.green(), null, "M", "for 2 players");
    }

    /** Four driving keys (letters, or arrows when null) in a T, the fire key beside them and a note under them. */
    private void keys(Graphics2D g, Tank tank, String[] letters, String fire, String note)
    {
        int s = 24;
        int step = s + 3;
        int top = (int) tank.y() + Tank.SIZE + 10;
        int left = (int) tank.x() - (step * 4 + 8) / 2;
        int[][] at = { { left + step, top }, { left, top + step }, { left + step, top + step },
                        { left + step * 2, top + step } };
        for (int i = 0; i < at.length; i++) keycap(g, at[i][0], at[i][1], s, letters == null ? null : letters[i], i);
        int fx = left + step * 3 + 8;
        keycap(g, fx, top + step, s, fire, -1);
        outlined(g, "fire", CHIP, TEXT, fx + s / 2.0, top + s - 4, 255);
        outlined(g, note, SMALL, light(tank), tank.x(), top + step * 2 + 16, 255);
    }

    private static final Font KEY = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    /** A light key with a letter, or an arrow pointing up, left, down or right (arrow 0 to 3) when the text is null. */
    private static void keycap(Graphics2D g, int x, int y, int s, String text, int arrow)
    {
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(x + 1, y + 3, s, s, 7, 7);
        g.setColor(new Color(236, 232, 222));
        g.fillRoundRect(x, y, s, s, 7, 7);
        g.setColor(new Color(40, 40, 52));
        if (text != null)
        {
            g.setFont(KEY);
            g.drawString(text, x + (s - g.getFontMetrics().stringWidth(text)) / 2, y + s / 2 + 5);
            return;
        }
        int cx = x + s / 2;
        int cy = y + s / 2;
        double angle = Math.toRadians(-90 - arrow * 90);
        int[] xs = new int[3];
        int[] ys = new int[3];
        for (int i = 0; i < 3; i++)
        {
            double a = angle + i * Math.PI * 2 / 3;
            xs[i] = cx + (int) Math.round(Math.cos(a) * 6);
            ys[i] = cy + (int) Math.round(Math.sin(a) * 6);
        }
        g.fillPolygon(xs, ys, 3);
    }

    /** Draws the lines centered on a shaded box in the middle of the field; empty lines leave a gap, null ones none. */
    private void panel(Graphics2D g, Line[] lines)
    {
        panel(g, lines, -1, 0, 0);
    }

    /**
     * Draws the lines centered on a shaded box at least minWidth wide (when the field allows), its top at top or in the
     * middle of the field when top is negative, with extra room under the lines; returns the box.
     */
    private Rectangle panel(Graphics2D g, Line[] lines, int top, int minWidth, int extra)
    {
        int height = 24 + extra;
        int width = 0;
        for (Line line : lines)
        {
            if (line == null) continue;
            height += line.font.getSize() + 10;
            g.setFont(line.font);
            width = Math.max(width, g.getFontMetrics().stringWidth(line.text));
        }
        width = Math.min(getWidth() - 20, Math.max(minWidth, width + 40));
        int x = (getWidth() - width) / 2;
        int y = top < 0 ? (getHeight() - height) / 2 : top;
        g.setColor(SHADE);
        g.fillRoundRect(x, y, width, height, 16, 16);
        int baseline = y + 12;
        for (Line line : lines)
        {
            if (line == null) continue;
            baseline += line.font.getSize() + 10;
            g.setFont(line.font);
            g.setColor(line.color);
            g.drawString(line.text, (getWidth() - g.getFontMetrics().stringWidth(line.text)) / 2, baseline - 8);
        }
        return new Rectangle(x, y, width, height);
    }

    private static final class Line
    {
        final String text;
        final Font   font;
        final Color  color;

        Line(String text, Font font, Color color)
        {
            this.text = text;
            this.font = font;
            this.color = color;
        }
    }
}
