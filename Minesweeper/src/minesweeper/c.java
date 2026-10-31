package minesweeper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public final class c implements KeyListener {
   private e a;

   public c(e var1) {
      this.a = var1;
   }

   private void a(KeyEvent var1) {
      if (var1.getKeyCode() == 27) {
         System.exit(0);
      }

      if (var1.getKeyCode() == 38) {
         this.a.setLocation(this.a.getX(), this.a.getY() - 30);
      } else if (var1.getKeyCode() == 39) {
         this.a.setLocation(this.a.getX() + 30, this.a.getY());
      } else if (var1.getKeyCode() == 40) {
         this.a.setLocation(this.a.getX(), this.a.getY() + 30);
      } else {
         if (var1.getKeyCode() == 37) {
            this.a.setLocation(this.a.getX() - 30, this.a.getY());
         }
      }
   }

   @Override
   public final void keyPressed(KeyEvent var1) {
      this.a(var1);
   }

   @Override
   public final void keyReleased(KeyEvent var1) {
      this.a(var1);
   }

   @Override
   public final void keyTyped(KeyEvent var1) {
      this.a(var1);
   }
}
