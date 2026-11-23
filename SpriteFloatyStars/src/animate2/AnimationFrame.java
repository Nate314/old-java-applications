package animate2;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;

public final class AnimationFrame extends JFrame implements KeyListener {
   private StarField starField;

   public AnimationFrame(int mode, Dimension size, int starCount, int starSize) {
      this.setSize(size);
      // Closing a stars' window goes back to the settings window, to pick another sky.
      this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      this.add(this.starField = new StarField(mode, size, starCount, starSize));
      this.nameTitle();
      this.starField.onChange(this::nameTitle);
      this.starField.setFocusable(false);
      this.addKeyListener(this);
   }

   /** The title names the mode playing, as the settings window's tiles do. */
   private void nameTitle() {
      this.setTitle("Sprite Floaty Stars: " + StarField.MODE_NAMES[this.starField.mode()]);
   }

   @Override
   public void dispose() {
      this.starField.stop();
      super.dispose();
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

   /** Keys reach the stars the same way the buttons along the bottom do. */
   @Override
   public final void keyPressed(KeyEvent e) {
      this.starField.key(e.getKeyCode());
   }

   @Override
   public final void keyReleased(KeyEvent e) {
   }
}
