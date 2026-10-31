package tankgame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class Tank extends JPanel
{
    private int           tankSize     = 50;
    private int           tankSpeed    = tankSize / 10;
    private int           tankRotation = 90;
    private int           tankX        = 0;
    private int           tankY        = 0;
    private Color         tankColor;
    private MyKeyListener wListener;
    private MyKeyListener aListener;
    private MyKeyListener sListener;
    private MyKeyListener dListener;
    private MyKeyListener qListener;

    /**
     * @param frame
     *            main JFrame to be passed so that key listeners can be added to
     *            the frame
     * @param tankColor
     *            color of the tank
     * @param up
     *            KeyEvent.VK_(keyname) used for moving the tank forwards
     * @param left
     *            KeyEvent.VK_(keyname) used for rotating the tank
     *            counterclockwise
     * @param down
     *            KeyEvent.VK_(keyname) used for moving the tank backwards
     * @param right
     *            KeyEvent.VK_(keyname) used for rotating the tank clockwise
     * @param fire
     *            KeyEvent.VK_(keyname) used for firing
     */
    public Tank(Main frame, Color tankColor, int up, int left, int down, int right, int fire)
    {
        this.setBackground(new Color(0, 0, 0, 0));
        this.tankColor = tankColor;
        wListener = new MyKeyListener(up);
        aListener = new MyKeyListener(left);
        sListener = new MyKeyListener(down);
        dListener = new MyKeyListener(right);
        qListener = new MyKeyListener(fire);
        frame.addKeyListener(wListener);
        frame.addKeyListener(aListener);
        frame.addKeyListener(sListener);
        frame.addKeyListener(dListener);
        frame.addKeyListener(qListener);
        this.setOpaque(true);
    }

    /**
     * @param forward
     *            should be true if the tank should be moved forward and false
     *            if the tank should be moved in the reverse direction.
     */
    public void moveTank(boolean forward)
    {
        System.out.println("moving" + (forward ? "forward" : "backward"));
        if (forward)
        {
            tankX += (Math.sin(Math.toRadians(tankRotation)) * tankSpeed);
            tankY -= (Math.cos(Math.toRadians(tankRotation)) * tankSpeed);
        }
        else
        {
            tankX -= (Math.sin(Math.toRadians(tankRotation)) * tankSpeed);
            tankY += (Math.cos(Math.toRadians(tankRotation)) * tankSpeed);
        }
    }

    /**
     * @param clockwise
     *            should be true if the tank should be rotated clockwise and
     *            false if the tank should be rotated in the counterclockwise.
     */
    public void rotateTank(boolean counterClockwise)
    {
        System.out.println("rotating" + (counterClockwise ? "counterclockwise" : "clockwise"));
        tankRotation += counterClockwise ? -tankSpeed : tankSpeed;
    }

    /**
     * fires the cannon
     */
    public void fireTank()
    {
        repaint();
    }

    @Override
    public void paint(Graphics gr)
    {
        if (wListener.keyPressed) moveTank(true);
        if (aListener.keyPressed) rotateTank(true);
        if (sListener.keyPressed) moveTank(false);
        if (dListener.keyPressed) rotateTank(false);
        if (qListener.keyTyped)
        {
            fireTank();
            qListener.keyTyped = false;
        }
        System.out.printf("d, %d @%ddegreed\n", tankX, tankY, tankRotation);
        Graphics2D g = (Graphics2D) gr;
        // g.clearRect(0, 0, getWidth(), getHeight());
        g.setColor(new Color(255, 255, 255));
        g.drawString(String.format("XPosition: %d\nYPosition: %d\nrotation: %d", tankX, tankY, tankRotation), tankSize,
                        tankSize);
        Rectangle tankBody = new Rectangle(-50, -50, 100, 100);
        Rectangle tankBarrel = new Rectangle(25, -25, 50, 50);
        g.setColor(tankColor);
        g.translate(tankX, tankY);
        g.rotate(Math.toRadians(tankRotation));
        g.draw(tankBody);
        g.fill(tankBody);
        g.setColor(new Color(0, 0, 0));
        g.draw(tankBarrel);
        g.fill(tankBarrel);
        Main.safeSleep(10);
        repaint();
    }
}
