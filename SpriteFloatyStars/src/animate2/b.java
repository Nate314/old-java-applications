package animate2;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

public final class b extends JFrame implements KeyListener {
   private a a;

   public b(int var1, Dimension var2, int var3, int var4) {
      this.setTitle("Animation Test");
      this.setSize(var2);
      this.setDefaultCloseOperation(3);
      this.add(this.a = new a(var1, var2, var3, var4));
      this.addKeyListener(this);
   }

   @Override
   public final void keyTyped(KeyEvent var1) {
      if (var1.getKeyCode() == 38) {
         this.a.a++;
         System.out.println("test");
      } else {
         if (var1.getKeyCode() == 40) {
            this.a.a--;
         }
      }
   }

   @Override
   public final void keyPressed(KeyEvent var1) {
   }

   @Override
   public final void keyReleased(KeyEvent var1) {
   }
}
