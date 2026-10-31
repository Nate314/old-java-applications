package rubikscube;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

public final class MoveKeyDispatcher implements KeyEventDispatcher {
   private String faceColor = "";
   private CubeFrame frame = null;

   public MoveKeyDispatcher(CubeFrame frame) {
      this.frame = frame;
   }

   @Override
   public final boolean dispatchKeyEvent(KeyEvent event) {
      if (event.getID() == 401) {
         int keyCode;
         if ((keyCode = event.getKeyCode()) == 87) {
            System.out.println("w");
            this.applyMove("w");
         } else if (keyCode == 65) {
            System.out.println("a");
            this.applyMove("a");
         } else if (keyCode == 83) {
            System.out.println("s");
            this.applyMove("s");
         } else if (keyCode == 68) {
            System.out.println("d");
            this.applyMove("d");
         }
      }

      return false;
   }

   private void applyMove(String key) {
      this.faceColor = this.frame.cube.selectedFaceColor;
      new Move(this.faceColor, key, this.frame.selectedCubie, this.frame.cube);
      this.frame.cube.refresh();
   }
}
