package tankgame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MyKeyListener implements KeyListener
{
    private int    key;
    public boolean keyPressed = false;
    public boolean keyTyped = false;

    public MyKeyListener(int key)
    {
        this.key = key;
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
        // System.out.printf("%dor%d typed\n", key1, key2);
        if (e.getKeyCode() == key) keyTyped = true;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        System.out.printf("%d pressed\n", key);
        if (e.getKeyCode() == key) keyPressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        System.out.printf("%d released\n", key);
        if (e.getKeyCode() == key) keyPressed = false;
    }
}
