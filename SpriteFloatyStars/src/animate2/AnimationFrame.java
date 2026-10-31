package animate2;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

public final class AnimationFrame extends JFrame implements KeyListener {
   private StarField starField;

   public AnimationFrame(int mode, Dimension size, int starCount, int starSize) {
      this.setTitle("Animation Test");
      this.setSize(size);
      this.setDefaultCloseOperation(3);
      this.add(this.starField = new StarField(mode, size, starCount, starSize));
      this.addKeyListener(this);
   }

   @Override
   public final void keyTyped(KeyEvent e) {
      if (e.getKeyCode() == 38) {
         this.starField.starCount++;
         System.out.println("test");
      } else {
         if (e.getKeyCode() == 40) {
            this.starField.starCount--;
         }
      }
   }

   @Override
   public final void keyPressed(KeyEvent e) {
   }

   @Override
   public final void keyReleased(KeyEvent e) {
   }
}
