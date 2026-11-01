package minesweeper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;

public final class WindowMoveKeyListener implements KeyListener {
   private GameFrame frame;

   public WindowMoveKeyListener(GameFrame frame) {
      this.frame = frame;
   }

   private void handleKey(KeyEvent event) {
      if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
         // Same as the close button, so inside NathaNOS it closes only this window.
         this.frame.dispatchEvent(new WindowEvent(this.frame, WindowEvent.WINDOW_CLOSING));
         return;
      }

      if (event.getKeyCode() == KeyEvent.VK_UP) {
         this.frame.setLocation(this.frame.getX(), this.frame.getY() - 30);
      } else if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
         this.frame.setLocation(this.frame.getX() + 30, this.frame.getY());
      } else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
         this.frame.setLocation(this.frame.getX(), this.frame.getY() + 30);
      } else {
         if (event.getKeyCode() == KeyEvent.VK_LEFT) {
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
   }

   @Override
   public final void keyTyped(KeyEvent event) {
   }
}
