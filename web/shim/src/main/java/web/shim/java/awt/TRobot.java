package web.shim.java.awt;

import web.shim.java.awt.web.TDesktop;
import web.rt.Edt;
import web.rt.Js;
import web.rt.Sleep;
import web.shim.java.awt.event.TKeyEvent;

/**
 * A page-level Robot. Key presses become AWT key events for the focused component, queued like real OS input;
 * getPixelColor samples whatever canvas (or widget background) is drawn at that viewport point.
 * It cannot reach anything outside the page. The Windows key and the keys pressed while it is held are the
 * operating system's, not the app's: they go to the page's pretend Windows (Js.shellShortcut) as a shortcut,
 * the Windows key alone when it is released with nothing pressed in between, or Win+key when that key goes down.
 */
public class TRobot {
    private static final int VK_WINDOWS = 524;   // java.awt.event.KeyEvent.VK_WINDOWS
    private static boolean windowsDown;
    private static boolean windowsCombo;

    public TRobot() throws TAWTException {
    }

    public void keyPress(int keycode) {
        if (keycode == VK_WINDOWS) {
            windowsDown = true;
            windowsCombo = false;
        } else if (windowsDown) {
            windowsCombo = true;
            Js.shellShortcut(keycode);
        } else {
            post(TKeyEvent.KEY_PRESSED, keycode);
        }
    }

    public void keyRelease(int keycode) {
        if (keycode == VK_WINDOWS) {
            if (windowsDown && !windowsCombo) {
                Js.shellShortcut(keycode);
            }
            windowsDown = false;
        } else if (!windowsDown) {
            post(TKeyEvent.KEY_RELEASED, keycode);
        }
    }

    private static void post(int id, int keycode) {
        TComponent target = TDesktop.keyTarget();
        if (target == null) {
            return;
        }
        char c = keycode >= 65 && keycode <= 90 ? Character.toLowerCase((char) keycode)
                : keycode == 32 || keycode >= 48 && keycode <= 57 ? (char) keycode : '\uFFFF';
        TKeyEvent event = new TKeyEvent(target, id, System.currentTimeMillis(), 0, keycode, c);
        Edt.post(() -> TDesktop.dispatchKey(event));
        if (id == TKeyEvent.KEY_PRESSED && c != '\uFFFF') {
            TKeyEvent typed = new TKeyEvent(target, TKeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, 0, c);
            Edt.post(() -> TDesktop.dispatchKey(typed));
        }
    }

    public TColor getPixelColor(int x, int y) {
        int rgb = Js.pixelAt(x, y);
        return rgb < 0 ? TColor.white : new TColor(rgb);
    }

    public void delay(int ms) {
        try {
            Sleep.sleep(ms);
        } catch (InterruptedException e) {
            // ignored, as in java.awt.Robot
        }
    }

    public void mouseMove(int x, int y) {
    }
}
