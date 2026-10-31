package rubikscube;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

public final class MoveKeyDispatcher implements KeyEventDispatcher {
   private CubeFrame frame = null;

   public MoveKeyDispatcher(CubeFrame frame) {
      this.frame = frame;
   }

   @Override
   public final boolean dispatchKeyEvent(KeyEvent event) {
      if (event.getID() == KeyEvent.KEY_PRESSED) {
         int keyCode = event.getKeyCode();
         if (keyCode == KeyEvent.VK_W) {
            this.frame.applyUserMove("w");
         } else if (keyCode == KeyEvent.VK_A) {
            this.frame.applyUserMove("a");
         } else if (keyCode == KeyEvent.VK_S) {
            this.frame.applyUserMove("s");
         } else if (keyCode == KeyEvent.VK_D) {
            this.frame.applyUserMove("d");
         }
      }

      return false;
   }
}
