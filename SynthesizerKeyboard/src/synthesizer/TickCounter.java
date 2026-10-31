package synthesizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class TickCounter implements ActionListener {
   private final KeyboardWindow window;

   TickCounter(KeyboardWindow window) {
      this.window = window;
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      this.window.tickCount++;
   }
}
