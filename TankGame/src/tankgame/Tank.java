package tankgame;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class Tank
{
    private int           tankSize     = 50;
    private int           tankSpeed    = tankSize / 10;
    private int           tankRotation;
    private double        tankX;
    private double        tankY;
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
     * @param x
     *            starting center X
     * @param y
     *            starting center Y
     * @param rotation
     *            starting direction in degrees (0 is up, 90 is right)
     */
    public Tank(Main frame, Color tankColor, int up, int left, int down, int right, int fire, int x, int y,
                    int rotation)
    {
        this.tankColor = tankColor;
        this.tankX = x;
        this.tankY = y;
        this.tankRotation = rotation;
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
    }

    /**
     * @param forward
     *            should be true if the tank should be moved forward and false
     *            if the tank should be moved in the reverse direction.
     */
    public void moveTank(boolean forward)
    {
        int direction = forward ? 1 : -1;
        tankX += Math.sin(Math.toRadians(tankRotation)) * tankSpeed * direction;
        tankY -= Math.cos(Math.toRadians(tankRotation)) * tankSpeed * direction;
    }

    /**
     * @param counterClockwise
     *            should be true if the tank should be rotated counterclockwise
     *            and false if the tank should be rotated clockwise.
     */
    public void rotateTank(boolean counterClockwise)
    {
        tankRotation = Math.floorMod(tankRotation + (counterClockwise ? -tankSpeed : tankSpeed), 360);
    }

    /**
     * fires the cannon: a shell leaves the end of the barrel
     */
    public void fireTank(List<Shell> shells)
    {
        double dx = Math.sin(Math.toRadians(tankRotation));
        double dy = -Math.cos(Math.toRadians(tankRotation));
        int barrelEnd = tankSize * 3 / 2;
        shells.add(new Shell(tankX + dx * barrelEnd, tankY + dy * barrelEnd, dx, dy, tankColor));
    }

    /** One game tick: applies the held keys, keeps the tank inside the field, and fires on a new Q/M press. */
    public void update(int width, int height, List<Shell> shells)
    {
        if (wListener.keyPressed) moveTank(true);
        if (aListener.keyPressed) rotateTank(true);
        if (sListener.keyPressed) moveTank(false);
        if (dListener.keyPressed) rotateTank(false);
        tankX = Math.max(tankSize, Math.min(width - tankSize, tankX));
        tankY = Math.max(tankSize, Math.min(height - tankSize, tankY));
        if (qListener.newPress)
        {
            fireTank(shells);
            qListener.newPress = false;
        }
    }

    public void paint(Graphics2D gr, int statusX)
    {
        gr.setColor(new Color(255, 255, 255));
        String[] status = { "XPosition: " + Math.round(tankX), "YPosition: " + Math.round(tankY), "rotation: " + tankRotation };
        for (int i = 0; i < status.length; i++)
        {
            gr.drawString(status[i], statusX, 20 + 15 * i);
        }
        Graphics2D g = (Graphics2D) gr.create();
        g.setColor(tankColor);
        g.translate(tankX, tankY);
        // The barrel is drawn along +x, which is 90 degrees clockwise of "up" (rotation 0).
        g.rotate(Math.toRadians(tankRotation - 90));
        g.fillRect(-tankSize, -tankSize, 2 * tankSize, 2 * tankSize);
        g.setColor(new Color(0, 0, 0));
        g.fillRect(tankSize / 2, -tankSize / 2, tankSize, tankSize);
        g.dispose();
    }
}
