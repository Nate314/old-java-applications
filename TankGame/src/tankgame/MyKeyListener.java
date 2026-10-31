package tankgame;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class MyKeyListener implements KeyListener
{
    private int    key;
    public boolean keyPressed = false;
    /** Set once when the key goes down (not on auto-repeat); the reader clears it. */
    public boolean newPress   = false;

    public MyKeyListener(int key)
    {
        this.key = key;
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == key)
        {
            if (!keyPressed) newPress = true;
            keyPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == key) keyPressed = false;
    }
}
