package minesweeper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public final class WindowMoveKeyListener implements KeyListener {
   private GameFrame frame;

   public WindowMoveKeyListener(GameFrame frame) {
      this.frame = frame;
   }

   private void handleKey(KeyEvent event) {
      if (event.getKeyCode() == 27) {
         System.exit(0);
      }

      if (event.getKeyCode() == 38) {
         this.frame.setLocation(this.frame.getX(), this.frame.getY() - 30);
      } else if (event.getKeyCode() == 39) {
         this.frame.setLocation(this.frame.getX() + 30, this.frame.getY());
      } else if (event.getKeyCode() == 40) {
         this.frame.setLocation(this.frame.getX(), this.frame.getY() + 30);
      } else {
         if (event.getKeyCode() == 37) {
            this.frame.setLocation(this.frame.getX() - 30, this.frame.getY());
         }
      }
   }

   @Override
   public final void keyPressed(KeyEvent event) {
      this.handleKey(event);
   }

   @Override
   public final void keyReleased(KeyEvent event) {
      this.handleKey(event);
   }

   @Override
   public final void keyTyped(KeyEvent event) {
      this.handleKey(event);
   }
}
