package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Runs the battle on a Swing timer and paints the battlefield (grass and dirt that keeps the round's track marks and
 * blast scorches, and concrete walls), the tanks, the shells, the sparks and smoke, the score bar and the banners.
 */
@SuppressWarnings("serial")
public class GamePanel extends JPanel
{
    private static final int   TICK_MILLIS = 16;
    private static final Color FIELD       = new Color(106, 122, 72);
    /** The ground is drawn this many times finer than the field, so it stays sharp when the page zooms the window. */
    private static final int   GROUND_SCALE = 2;
    private static final Color CONCRETE    = new Color(150, 146, 138);
    private static final Color SHADE       = new Color(0, 0, 40, 215);
    private static final Color TEXT        = Color.WHITE;
    private static final Color DIM         = new Color(200, 205, 255);
    private static final Font  BIG         = new Font(Font.SANS_SERIF, Font.BOLD, 34);
    private static final Font  MEDIUM      = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font  SMALL       = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Battle       battle;
    private final Timer        timer       = new Timer(TICK_MILLIS, e -> tick());
    /** The round's ground, with every track mark and scorch so far; made again for each battlefield. */
    private BufferedImage      ground;
    private Arena              groundArena;
    /** Each tank's x, y and rotation when its tracks last marked the ground. */
    private final double[][]   lastPose    = new double[2][];
    private int                ticks;

    public GamePanel(Battle battle)
    {
        this.battle = battle;
        this.setBackground(FIELD);
    }

    public void start()
    {
        timer.start();
    }

    /** Moves everything one step on the Swing event thread, so speed does not depend on the paint rate. */
    private void tick()
    {
        battle.tick(getWidth(), getHeight());
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
        }
        repaint();
    }

    /** Lays fresh ground for the battlefield: grass with patches of dirt, tufts and pebbles. */
    private void makeGround()
    {
        groundArena = battle.arena();
        lastPose[0] = lastPose[1] = null;
        int width = getWidth();
        int height = getHeight();
        ground = new BufferedImage(width * GROUND_SCALE, height * GROUND_SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ground.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(GROUND_SCALE, GROUND_SCALE);
        g.setColor(FIELD);
        g.fillRect(0, 0, width, height);
        Random random = new Random(groundArena.name().hashCode());
        Color[] patches = { new Color(136, 150, 88, 34), new Color(84, 100, 56, 34), new Color(132, 112, 78, 40) };
        for (int i = 0; i < width * height / 330; i++)
        {
            int size = 8 + random.nextInt(34);
            g.setColor(patches[random.nextInt(patches.length)]);
            g.fillOval(random.nextInt(width + 20) - 20, random.nextInt(height + 20) - 20, size, size * 3 / 4);
        }
        Color[] blades = { new Color(64, 86, 40, 170), new Color(150, 168, 96, 150) };
        for (int i = 0; i < width * height / 900; i++)
        {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            g.setColor(blades[random.nextInt(blades.length)]);
            for (int b = -1; b <= 1; b++) g.drawLine(x, y, x + b * 2 + random.nextInt(2), y - 3 - random.nextInt(3));
        }
        for (int i = 0; i < width * height / 4500; i++)
        {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            int size = 3 + random.nextInt(4);
            g.setColor(new Color(60, 56, 48, 120));
            g.fillOval(x + 1, y + 1, size, size - 1);
            g.setColor(new Color(160, 154, 142));
            g.fillOval(x, y, size, size - 1);
        }
        g.dispose();
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
            t.setColor(new Color(52, 44, 30, 26));
            t.fillRect(-Tank.SIZE + 2, top, 6, 18);
            if (ticks % 2 == 0)
            {
                t.setColor(new Color(40, 34, 24, 36));
                t.fillRect(-Tank.SIZE + 3, top + 1, 2, 16);
            }
        }
        t.dispose();
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

    /** A concrete wall with a drop shadow, a lit top and left edge, a shaded bottom and right, and block seams. */
    private static void wall(Graphics2D g, Arena.Wall wall)
    {
        int l = (int) Math.round(wall.left);
        int t = (int) Math.round(wall.top);
        int w = (int) Math.round(wall.right - wall.left);
        int h = (int) Math.round(wall.bottom - wall.top);
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(l + 6, t + 8, w, h, 6, 6);
        g.setColor(CONCRETE);
        g.fillRect(l, t, w, h);
        g.setColor(Tank.shade(CONCRETE, 1.3));
        g.fillRect(l, t, w, 4);
        g.fillRect(l, t, 4, h);
        g.setColor(Tank.shade(CONCRETE, 0.7));
        g.fillRect(l, t + h - 4, w, 4);
        g.fillRect(l + w - 4, t + 4, 4, h - 4);
        g.setColor(Tank.shade(CONCRETE, 0.78));
        if (w >= h) for (int x = l + 27; x < l + w - 8; x += 27) g.drawLine(x, t + 4, x, t + h - 5);
        else for (int y = t + 27; y < t + h - 8; y += 27) g.drawLine(l + 4, y, l + w - 5, y);
        g.setColor(new Color(60, 56, 50));
        g.drawRect(l, t, w - 1, h - 1);
    }

    @Override
    protected void paintComponent(Graphics gr)
    {
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (ground != null) g.drawImage(ground, 0, 0, getWidth(), getHeight(), null);
        for (Arena.Wall wall : battle.arena().walls()) wall(g, wall);
        if (battle.phase() != Battle.Phase.TITLE)
        {
            battle.red().paint(g);
            battle.green().paint(g);
        }
        for (Shell shell : battle.shells()) shell.paint(g);
        for (Spark spark : battle.sparks()) spark.paint(g);
        if (battle.phase() != Battle.Phase.TITLE)
        {
            scoreBar(g, battle.red(), battle.redWins(), false);
            scoreBar(g, battle.green(), battle.greenWins(), true);
        }
        banner(g);
        if (battle.watching()) watchNote(g);
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
    }

    private void banner(Graphics2D g)
    {
        Tank red = battle.red();
        Tank green = battle.green();
        Tank winner = battle.winner();
        switch (battle.phase())
        {
            case TITLE:
                panel(g, new Line[] { new Line("TANK GAME", BIG, TEXT),
                                new Line("Three hits destroy a tank. First to " + Battle.ROUNDS_TO_WIN + " rounds wins.", SMALL, DIM),
                                new Line("Hide behind walls. Shells bounce once.", SMALL, DIM),
                                new Line("Each round is a new battlefield.", SMALL, DIM),
                                new Line("Red: W A S D to drive, Q to fire", SMALL, light(red)),
                                new Line("Green: arrow keys to drive, M to fire", SMALL, light(green)),
                                new Line("", SMALL, TEXT),
                                new Line("Press Q to play the computer", MEDIUM, light(red)),
                                new Line("Press M for two players", MEDIUM, light(green)),
                                new Line("Press C to watch the computer play itself", SMALL, DIM),
                                recordLine() });
                break;
            case ROUND_START:
                boolean go = battle.phaseTicks() > Battle.ROUND_START_TICKS - 25;
                panel(g, new Line[] { new Line(go ? "FIGHT!" : "ROUND " + battle.round(), BIG, go ? Color.YELLOW : TEXT),
                                new Line(battle.arena().name(), MEDIUM, TEXT),
                                new Line(battle.redWins() + " - " + battle.greenWins(), SMALL, DIM) });
                break;
            case ROUND_OVER:
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
                boolean ready = battle.phaseTicks() >= Battle.MATCH_OVER_TICKS;
                panel(g, new Line[] { new Line(title, MEDIUM, light(winner)), new Line(score, BIG, TEXT),
                                battle.vsComputer() ? recordLine() : new Line("", SMALL, TEXT),
                                new Line(ready ? "Press Q to play the computer" : "", SMALL, light(red)),
                                new Line(ready ? "Press M for two players" : "", SMALL, light(green)) });
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

    private Line recordLine()
    {
        Record record = battle.record();
        boolean any = record != null && record.wins() + record.losses() > 0;
        return new Line(any ? "Your record vs the computer: " + record : "", SMALL, Color.YELLOW);
    }

    /** Draws the lines centered on a shaded box in the middle of the field; empty lines leave a gap. */
    private void panel(Graphics2D g, Line[] lines)
    {
        int height = 24;
        int width = 0;
        for (Line line : lines)
        {
            height += line.font.getSize() + 10;
            g.setFont(line.font);
            width = Math.max(width, g.getFontMetrics().stringWidth(line.text));
        }
        width = Math.min(getWidth() - 20, width + 40);
        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;
        g.setColor(SHADE);
        g.fillRoundRect(x, y, width, height, 16, 16);
        int baseline = y + 12;
        for (Line line : lines)
        {
            baseline += line.font.getSize() + 10;
            g.setFont(line.font);
            g.setColor(line.color);
            g.drawString(line.text, (getWidth() - g.getFontMetrics().stringWidth(line.text)) / 2, baseline - 8);
        }
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
