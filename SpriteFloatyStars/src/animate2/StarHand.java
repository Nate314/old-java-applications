package animate2;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** The mouse (or a finger) on an animation window's stars: rest to nudge them, hold to pull them in, let go to fling. */
final class StarHand extends MouseAdapter {
   private final StarField field;

   StarHand(StarField field) {
      this.field = field;
      field.addMouseListener(this);
      field.addMouseMotionListener(this);
   }

   @Override
   public void mousePressed(MouseEvent e) {
      this.field.startPull(e.getX(), e.getY());
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      this.field.release(e.getX(), e.getY());
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      this.field.pointer(e.getX(), e.getY(), true);
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
