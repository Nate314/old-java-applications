package tankgame;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Runs the battle on a Swing timer and paints the field, the tanks, the shells, the score bar and the banners. */
@SuppressWarnings("serial")
public class GamePanel extends JPanel
{
    private static final int   TICK_MILLIS = 16;
    private static final Color FIELD       = new Color(100, 100, 255);
    private static final Color GRID        = new Color(88, 88, 235);
    private static final Color SHADE       = new Color(0, 0, 40, 215);
    private static final Color TEXT        = Color.WHITE;
    private static final Color DIM         = new Color(200, 205, 255);
    private static final Font  BIG         = new Font(Font.SANS_SERIF, Font.BOLD, 34);
    private static final Font  MEDIUM      = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font  SMALL       = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Battle       battle;
    private final Timer        timer       = new Timer(TICK_MILLIS, e -> tick());

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
        repaint();
    }

    @Override
    protected void paintComponent(Graphics gr)
    {
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(GRID);
        for (int x = 50; x < getWidth(); x += 50) g.drawLine(x, 0, x, getHeight());
        for (int y = 50; y < getHeight(); y += 50) g.drawLine(0, y, getWidth(), y);
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
                                new Line("Shells bounce off the walls once.", SMALL, DIM),
                                new Line("Red: W A S D to drive, Q to fire", SMALL, light(red)),
                                new Line("Green: arrow keys to drive, M to fire", SMALL, light(green)),
                                new Line("", SMALL, TEXT),
                                new Line("Press Q to play the computer", MEDIUM, light(red)),
                                new Line("Press M for two players", MEDIUM, light(green)),
                                recordLine() });
                break;
            case ROUND_START:
                boolean go = battle.phaseTicks() > Battle.ROUND_START_TICKS - 25;
                panel(g, new Line[] { new Line(go ? "FIGHT!" : "ROUND " + battle.round(), BIG, go ? Color.YELLOW : TEXT),
                                new Line(battle.redWins() + " - " + battle.greenWins(), MEDIUM, DIM) });
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
                String title = !battle.vsComputer() ? winner.name() + " wins the match"
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
