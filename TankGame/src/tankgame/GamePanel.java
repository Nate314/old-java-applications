package tankgame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

@SuppressWarnings("serial")
public class GamePanel extends JPanel
{
    private static final int  TICK_MILLIS = 16;
    private final List<Tank>  tanks       = new ArrayList<>();
    private final List<Shell> shells      = new ArrayList<>();
    private final Timer       timer       = new Timer(TICK_MILLIS, e -> tick());

    public GamePanel()
    {
        this.setBackground(new Color(100, 100, 255));
    }

    public void addTank(Tank tank)
    {
        tanks.add(tank);
    }

    public void start()
    {
        timer.start();
    }

    /** Moves everything one step on the Swing event thread, so speed no longer depends on the paint rate. */
    private void tick()
    {
        for (Tank tank : tanks)
        {
            tank.update(getWidth(), getHeight(), shells);
        }
        shells.removeIf(shell -> !shell.move(getWidth(), getHeight()));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics gr)
    {
        super.paintComponent(gr);
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (int i = 0; i < tanks.size(); i++)
        {
            tanks.get(i).paint(g, 10 + i * getWidth() / 2);
        }
        for (Shell shell : shells)
        {
            shell.paint(g);
        }
    }
}
