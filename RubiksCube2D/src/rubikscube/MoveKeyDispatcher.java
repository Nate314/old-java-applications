package rubikscube;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;
import javax.swing.JTextField;

/**
 * Global keys: W/A/S/D turn the selected square, Z/H/R/N/P are the level keys, T turns Teach me on or off, V
 * switches between the 3D cube and the flat net and the arrow keys turn the 3D cube to look around. Typing in the Mix fields is left alone.
 */
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
         } else if (keyCode == KeyEvent.VK_T) {
            this.frame.toggleCoach();
         } else if (keyCode == KeyEvent.VK_V) {
            this.frame.toggleView();
         } else if (keyCode == KeyEvent.VK_LEFT) {
            this.frame.orbit(-1, 0);
         } else if (keyCode == KeyEvent.VK_RIGHT) {
            this.frame.orbit(1, 0);
         } else if (keyCode == KeyEvent.VK_UP) {
            this.frame.orbit(0, -1);
         } else if (keyCode == KeyEvent.VK_DOWN) {
            this.frame.orbit(0, 1);
         }
      }

      return false;
   }
}
