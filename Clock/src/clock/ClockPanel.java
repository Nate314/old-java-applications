package clock;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JPanel;

/**
 * The window's content: a strip of three tabs (Clock, Timer, Stopwatch) over the chosen tab and a line of help.
 * It routes the mouse and keys to that tab and saves the state after each change.
 */
public final class ClockPanel extends JPanel implements MouseListener, MouseMotionListener {
   private static final String[] MODES = {ClockState.CLOCK, ClockState.TIMER, ClockState.STOPWATCH};

   private final Main clock;
   private final Tab[] tabs;
   private Rectangle[] tabBoxes;
   private Tab pressed;
   private final KeyListener keys = new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent event) {
         ClockPanel.this.keyReleased(event.getKeyCode());
      }
   };

   public ClockPanel(Main clock) {
      this.clock = clock;
      this.tabs = new Tab[]{new ClockFace(clock), new TimerView(clock), new StopwatchView(clock)};
      this.addMouseListener(this);
      this.addMouseMotionListener(this);
      this.setFocusable(true);
   }

   KeyListener keys() {
      return this.keys;
   }

   private int current() {
      for (int i = 0; i < MODES.length; i++) {
         if (MODES[i].equals(this.clock.state.mode)) {
            return i;
         }
      }
      return 0;
   }

   void show(String mode) {
      this.clock.state.mode = mode;
   }

   /** The tab strip, the tab and the help line, laid out for the panel's size. */
   @Override
   public final void paint(Graphics graphics) {
      Graphics2D g2 = (Graphics2D)graphics;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setColor(Ui.BACKGROUND);
      g2.fillRect(0, 0, this.getWidth(), this.getHeight());
      long now = System.currentTimeMillis();
      int pad = Math.max(4, Math.min(this.getWidth(), this.getHeight()) / 40);
      int stripHeight = Math.max(24, Math.min(40, this.getHeight() / 11));
      int hintHeight = Math.max(12, Math.min(18, this.getHeight() / 26));

      this.tabBoxes = Ui.row(new Rectangle(pad, pad, this.getWidth() - 2 * pad, stripHeight), MODES.length, pad / 2);
      int current = this.current();
      for (int i = 0; i < this.tabs.length; i++) {
         Ui.button(g2, this.tabBoxes[i], this.tabs[i].label(now), i == current);
      }

      Tab tab = this.tabs[current];
      int top = pad * 2 + stripHeight;
      Rectangle area = new Rectangle(pad, top, this.getWidth() - 2 * pad, this.getHeight() - top - hintHeight - pad * 2);
      tab.paint(g2, area, now);

      g2.setColor(Ui.FAINT);
      String hint = tab.hint();
      Ui.center(g2, hint, Ui.fit(g2, Ui.LABELS, hint, this.getWidth() - 2 * pad, hintHeight, hintHeight),
            new Rectangle(pad, this.getHeight() - pad - hintHeight, this.getWidth() - 2 * pad, hintHeight));
   }

   private void changed() {
      this.clock.state.save();
      this.clock.refresh();
   }

   @Override
   public final void mousePressed(MouseEvent event) {
      this.requestFocusInWindow();
      int x = event.getX();
      int y = event.getY();
      Rectangle box = Ui.hit(this.tabBoxes, x, y);
      if (box != null) {
         this.show(MODES[Ui.indexOf(this.tabBoxes, box)]);
         this.pressed = null;
         this.changed();
         return;
      }
      this.pressed = this.tabs[this.current()];
      this.pressed.press(x, y, event.getButton() == MouseEvent.BUTTON3, System.currentTimeMillis());
      this.clock.refresh();
   }

   @Override
   public final void mouseDragged(MouseEvent event) {
      if (this.pressed != null) {
         this.pressed.drag(event.getX(), event.getY(), System.currentTimeMillis());
         this.clock.refresh();
      }
   }

   @Override
   public final void mouseReleased(MouseEvent event) {
      if (this.pressed != null) {
         this.pressed.release(event.getX(), event.getY(), System.currentTimeMillis());
         this.pressed = null;
         this.changed();
      }
   }

   @Override
   public final void mouseClicked(MouseEvent event) {
   }

   @Override
   public final void mouseMoved(MouseEvent event) {
   }

   @Override
   public final void mouseEntered(MouseEvent event) {
   }

   @Override
   public final void mouseExited(MouseEvent event) {
   }

   /** 1 2 3 (or C T S) pick a tab; other keys go to the tab. */
   private void keyReleased(int keyCode) {
      int pick = keyCode == KeyEvent.VK_1 || keyCode == KeyEvent.VK_C ? 0
            : keyCode == KeyEvent.VK_2 || keyCode == KeyEvent.VK_T ? 1
            : keyCode == KeyEvent.VK_3 || keyCode == KeyEvent.VK_S ? 2 : -1;
      if (pick >= 0) {
         this.show(MODES[pick]);
         this.changed();
      } else if (this.tabs[this.current()].key(keyCode, System.currentTimeMillis())) {
         this.changed();
      }
   }
}
