package tankgame;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Main extends JFrame
{
    public Main(Boolean visibility)
    {
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(500, 500);
        this.setTitle("TankGame");
        GamePanel panel = new GamePanel();
        this.add(panel);
        panel.addTank(new Tank(this, new Color(255, 0, 0), KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                        KeyEvent.VK_Q, 100, 250, 90));
        panel.addTank(new Tank(this, new Color(0, 255, 0), KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN,
                        KeyEvent.VK_RIGHT, KeyEvent.VK_M, 380, 250, 270));
        this.setVisible(visibility);
        panel.start();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new Main(true));
    }
}
