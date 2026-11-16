package tankgame;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
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
        // Red also follows the mouse or a finger on the field (PointerControls).
        PointerControls pointer = new PointerControls(new KeyControls(this, KeyEvent.VK_W, KeyEvent.VK_A,
                        KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_Q));
        Tank red = new Tank("Red", new Color(214, 58, 46), pointer, 100, 250, 90);
        pointer.drive(red);
        Tank green = new Tank("Green", new Color(66, 170, 64), new KeyControls(this, KeyEvent.VK_UP, KeyEvent.VK_LEFT,
                        KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_M), width - 120, 250, 270);
        // Read after the window exists: in the browser, saved files are restored once the first window is made.
        Record record = Record.load(RECORD_FILE);
        Battle battle = new Battle(red, green, new ComputerControls(new Random()), record);
        battle.setSupplies(record.supplies());
        battle.setField(Arena.cells(record.field()));
        battle.setSounds(Sounds.open(record.sound()));
        GamePanel panel = new GamePanel(battle);
        // C: watch the computer play both tanks (Battle.toggleWatch). X: supply drops on / off. B: build a battlefield. V: sound.
        this.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_C) battle.toggleWatch();
                if (e.getKeyCode() == KeyEvent.VK_X) battle.toggleSupplies();
                if (e.getKeyCode() == KeyEvent.VK_B) battle.toggleBuild();
                if (e.getKeyCode() == KeyEvent.VK_V) battle.toggleSound();
            }
        });
        this.add(panel);
        this.setVisible(visibility);
        panel.start();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new Main(true));
    }
}
