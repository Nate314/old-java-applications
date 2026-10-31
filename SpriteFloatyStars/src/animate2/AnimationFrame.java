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
      this.starField.setFocusable(false);
      this.addKeyListener(this);
   }

   @Override
   public final void keyTyped(KeyEvent e) {
   }

   @Override
   public final void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_UP) {
         this.starField.addStar();
      } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
         this.starField.removeStar();
      }
   }

   @Override
   public final void keyReleased(KeyEvent e) {
   }
}
