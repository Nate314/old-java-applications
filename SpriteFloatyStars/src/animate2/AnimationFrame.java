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
      if (this.writing()) {
         this.starField.skyWriting().type(e.getKeyChar());
      }
   }

   /** Sky writing with the keyboard writing the message: every key types, and Enter or Esc keeps it. */
   private boolean writing() {
      return this.starField.mode() == StarField.SKY_WRITING && this.starField.skyWriting().writing();
   }

   @Override
   public final void keyPressed(KeyEvent e) {
      int key = e.getKeyCode();
      if (this.writing()) {
         if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_ESCAPE) {
            this.starField.skyWriting().stopWriting();
         }
         return;
      }
      if (key == KeyEvent.VK_ENTER && this.starField.mode() == StarField.SKY_WRITING) {
         this.starField.skyWriting().startWriting();
      } else if (key == KeyEvent.VK_UP) {
         this.starField.addStar();
      } else if (key == KeyEvent.VK_DOWN) {
         this.starField.removeStar();
      } else if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_7) {
         this.starField.setMode(key - KeyEvent.VK_1);
      } else if (key >= KeyEvent.VK_NUMPAD1 && key <= KeyEvent.VK_NUMPAD7) {
         this.starField.setMode(key - KeyEvent.VK_NUMPAD1);
      } else if (key == KeyEvent.VK_N) {
         this.starField.setMode(this.starField.mode() + 1);
      } else if (key == KeyEvent.VK_SPACE) {
         this.starField.burstCenter();
      } else if (key == KeyEvent.VK_H) {
         this.starField.toggleHint();
      } else if (key == KeyEvent.VK_T) {
         this.starField.toggleTrails();
      }
   }

   @Override
   public final void keyReleased(KeyEvent e) {
   }
}
