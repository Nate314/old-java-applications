package animate2;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The mouse (or a finger) on an animation window's stars: rest to nudge them, hold to pull them in, let go to fling.
 * In Orbits a press on a sun drags the sun instead, and a right click on one takes it away.
 */
final class StarHand extends MouseAdapter {
   private final StarField field;

   StarHand(StarField field) {
      this.field = field;
      field.addMouseListener(this);
      field.addMouseMotionListener(this);
   }

   private Orbits orbits() {
      return this.field.mode() == StarField.ORBITS ? this.field.orbits() : null;
   }

   @Override
   public void mousePressed(MouseEvent e) {
      Orbits orbits = this.orbits();
      if (orbits != null && e.getButton() == MouseEvent.BUTTON3) {
         orbits.removeAt(e.getX(), e.getY());
         return;
      }
      this.field.pointer(e.getX(), e.getY(), true);
      if (orbits == null || !orbits.grab(e.getX(), e.getY())) {
         this.field.startPull(e.getX(), e.getY());
      }
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      Orbits orbits = this.orbits();
      if (orbits != null && orbits.holding()) {
         orbits.dragTo(e.getX(), e.getY());
         orbits.drop();
         return;
      }
      this.field.release(e.getX(), e.getY());
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      this.field.pointer(e.getX(), e.getY(), true);
      Orbits orbits = this.orbits();
      if (orbits != null) {
         orbits.dragTo(e.getX(), e.getY());
      }
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      this.field.pointer(e.getX(), e.getY(), true);
   }

   @Override
   public void mouseEntered(MouseEvent e) {
      this.field.pointer(e.getX(), e.getY(), true);
   }

   @Override
   public void mouseExited(MouseEvent e) {
      this.field.pointer(e.getX(), e.getY(), false);
   }
}
