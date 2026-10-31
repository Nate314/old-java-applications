package tankgame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class GamePanel extends JPanel
{
    public GamePanel(Main frame)
    {
    }

//    private void frameRegulator()
//    {
//        long time = System.currentTimeMillis();
//        while (true)
//        {
//            if (time + 50 < System.currentTimeMillis())
//            {
//                time = System.currentTimeMillis();
//                repaint();
//                System.out.println(System.currentTimeMillis());
//            }
//        }
//    }

    private void safeSleep(long millis)
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

    @Override
    public void paint(Graphics gr)
    {
        Graphics2D g = (Graphics2D) gr;
        Color background = new Color(100, 100, 255);
        g.setColor(background);
        g.fillRect(0, 0, getWidth(), getHeight());
        repaint();
        safeSleep(10);
    }
}