package tankgame;

import java.awt.Component;

/** A player's five keys, each tracked by its own listener on the window. */
public class KeyControls implements Controls
{
    private final MyKeyListener forward;
    private final MyKeyListener left;
    private final MyKeyListener back;
    private final MyKeyListener right;
    private final MyKeyListener fire;

    /** Takes KeyEvent.VK_ codes, in the original order: up, left, down, right, fire. */
    public KeyControls(Component window, int up, int left, int down, int right, int fire)
    {
        this.forward = listen(window, up);
        this.left = listen(window, left);
        this.back = listen(window, down);
        this.right = listen(window, right);
        this.fire = listen(window, fire);
    }

    private static MyKeyListener listen(Component window, int key)
    {
        MyKeyListener listener = new MyKeyListener(key);
        window.addKeyListener(listener);
        return listener;
    }

    @Override
    public boolean forward()
    {
        return forward.keyPressed;
    }

    @Override
    public boolean back()
    {
        return back.keyPressed;
    }

    @Override
    public boolean left()
    {
        return left.keyPressed;
    }

    @Override
    public boolean right()
    {
        return right.keyPressed;
    }

    @Override
    public boolean fire()
    {
        boolean pressed = fire.newPress;
        fire.newPress = false;
        return pressed;
    }
}
