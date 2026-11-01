package tankgame;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Main extends JFrame
{
    private static final File RECORD_FILE = new File(System.getProperty("user.home", "."), ".tankgame-record.txt");

    public Main(Boolean visibility)
    {
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        // 500 px wide, or the screen's width if that is narrower (a phone held upright, in the browser).
        int width = Math.min(500, Toolkit.getDefaultToolkit().getScreenSize().width);
        this.setSize(width, 500);
        this.setTitle("TankGame");
        Tank red = new Tank("Red", new Color(255, 0, 0), new KeyControls(this, KeyEvent.VK_W, KeyEvent.VK_A,
                        KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_Q), 100, 250, 90);
        Tank green = new Tank("Green", new Color(0, 255, 0), new KeyControls(this, KeyEvent.VK_UP, KeyEvent.VK_LEFT,
                        KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_M), width - 120, 250, 270);
        // Read after the window exists: in the browser, saved files are restored once the first window is made.
        Battle battle = new Battle(red, green, new ComputerControls(new Random()), Record.load(RECORD_FILE));
        GamePanel panel = new GamePanel(battle);
        this.add(panel);
        this.setVisible(visibility);
        panel.start();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new Main(true));
    }
}
