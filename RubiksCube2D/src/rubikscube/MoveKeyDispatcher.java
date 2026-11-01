package rubikscube;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;

/** Global keys: W/A/S/D turn the selected square, Z/H/R/N/P are the level keys. Typing in the Mix fields is left alone. */
public final class MoveKeyDispatcher implements KeyEventDispatcher {
   private CubeFrame frame = null;

   public MoveKeyDispatcher(CubeFrame frame) {
      this.frame = frame;
   }

   @Override
   public final boolean dispatchKeyEvent(KeyEvent event) {
      if (event.getID() == KeyEvent.KEY_PRESSED && !(event.getComponent() instanceof JTextField)) {
         int keyCode = event.getKeyCode();
         if (keyCode == KeyEvent.VK_W) {
            this.frame.applyUserMove("w");
         } else if (keyCode == KeyEvent.VK_A) {
            this.frame.applyUserMove("a");
         } else if (keyCode == KeyEvent.VK_S) {
            this.frame.applyUserMove("s");
         } else if (keyCode == KeyEvent.VK_D) {
            this.frame.applyUserMove("d");
         } else if (keyCode == KeyEvent.VK_Z) {
            this.frame.levelKey('z');
         } else if (keyCode == KeyEvent.VK_H) {
            this.frame.levelKey('h');
         } else if (keyCode == KeyEvent.VK_R) {
            this.frame.levelKey('r');
         } else if (keyCode == KeyEvent.VK_N) {
            this.frame.levelKey('n');
         } else if (keyCode == KeyEvent.VK_P) {
            this.frame.levelKey('p');
         }
      }

      return false;
   }
}
