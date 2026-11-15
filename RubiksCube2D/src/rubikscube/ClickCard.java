package rubikscube;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.JPanel;

/**
 * A hand-painted card with places to click (the Patterns and My cube cards): each paint lays out its targets afresh
 * with {@link #target}, the one under the mouse is {@link #hover}, and a press hands its action to the listener.
 */
abstract class ClickCard extends JPanel {
   static final int NO_ACTION = Integer.MIN_VALUE;
   private final List<Rectangle> targets = new ArrayList<>();
   private final List<Integer> actions = new ArrayList<>();
   int hover = NO_ACTION;

   ClickCard(IntConsumer onClick) {
      this.setLayout(null);
      this.setOpaque(false);
      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            int action = ClickCard.this.actionAt(event.getX(), event.getY());
            if (action != NO_ACTION) {
               onClick.accept(action);
            }
         }

         @Override
         public void mouseMoved(MouseEvent event) {
            int action = ClickCard.this.actionAt(event.getX(), event.getY());
            if (action != ClickCard.this.hover) {
               ClickCard.this.hover = action;
               ClickCard.this.repaint();
            }
         }

         @Override
         public void mouseExited(MouseEvent event) {
            ClickCard.this.hover = NO_ACTION;
            ClickCard.this.repaint();
         }
      };
      this.addMouseListener(mouse);
      this.addMouseMotionListener(mouse);
   }

   /** Forgets the targets of the last paint. */
   final void clearTargets() {
      this.targets.clear();
      this.actions.clear();
   }

   final void target(int x, int y, int width, int height, int action) {
      this.targets.add(new Rectangle(x, y, width, height));
      this.actions.add(action);
   }

   private int actionAt(int x, int y) {
      for (int i = 0; i < this.targets.size(); i++) {
         if (this.targets.get(i).contains(x, y)) {
            return this.actions.get(i);
         }
      }
      return NO_ACTION;
   }

   /** A button (see {@link CardPaint#button}) that clicks as {@code action}. */
   final void button(Graphics2D g, int x, int y, int width, String label, int action, boolean primary) {
      CardPaint.button(g, x, y, width, label, primary, this.hover == action);
      this.target(x, y, width, 26, action);
   }
}
