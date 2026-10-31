package tankgame;

import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;

@SuppressWarnings("serial")
public class Main extends JFrame
{
    public Main(Boolean visibility)
    {
        this.setVisible(visibility);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(500, 500);
        this.setTitle("TankGame");
        GamePanel panel = new GamePanel(this);
        this.add(panel);
        Tank tank1 = new Tank(this, new Color(255, 0, 0), KeyEvent.VK_W, KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D,
                        KeyEvent.VK_Q);
        this.add(tank1);
        tank1.setBounds(0, 0, 1000, 500);
        Tank tank2 = new Tank(this, new Color(0, 255, 0), KeyEvent.VK_UP, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN,
                        KeyEvent.VK_RIGHT, KeyEvent.VK_M);
        this.add(tank2);
        tank2.setBounds(0, 0, 1000, 500);
        // Tank tank2 = new Tank(this, KeyEvent.VK_UP, KeyEvent.VK_LEFT,
        // KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT);
        // this.add(tank2);
        // tank1.setBounds(500, 0, 1000, 500);
        // this.addKeyListener(panel);
    }

    public static void safeSleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        new Main(true);
    }
}
