package web.shim.java.awt;

import web.shim.java.awt.web.TDesktop;
import web.rt.Edt;
import web.rt.Js;
import web.rt.Sleep;
import web.shim.java.awt.event.TKeyEvent;

/**
 * A page-level Robot. Key presses become AWT key events for the focused component, queued like real OS input;
 * getPixelColor samples whatever canvas (or widget background) is drawn at that viewport point.
 * It cannot reach anything outside the page.
 */
public class TRobot {
    public TRobot() throws TAWTException {
    }

    public void keyPress(int keycode) {
        post(TKeyEvent.KEY_PRESSED, keycode);
    }

    public void keyRelease(int keycode) {
        post(TKeyEvent.KEY_RELEASED, keycode);
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
