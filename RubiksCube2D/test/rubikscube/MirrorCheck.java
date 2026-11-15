package rubikscube;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import javax.swing.JButton;

/**
 * Checks the 3D cube's mirrors: from 48 points of view every hidden side has one, inside the view and under the row
 * of buttons, apart from the others and clear of the cube's middle; a click on one turns its side to the front; X's
 * setting is saved.
 */
public final class MirrorCheck {
   public static void main(String[] args) throws Exception {
      CubeView3D view = new CubeView3D(new Cube(null, new JButton[6][9], 50, 50), (face, square) -> { }, key -> { });
      view.setSize(550, 352);
      int checked = 0;
      for (int y = 0; y < 8; y++) {
         for (double pitch : new double[]{-1.2, -0.48, 0.3, 0.48, 0.9, 1.3}) {
            look(view, -0.62 + y * Math.PI / 4, pitch);
            int hidden = 0;
            for (int face = 0; face < 6; face++) {
               if (view.facingOf(face * 9) < -0.05) {
                  hidden++;
                  assert mirrorOf(view, face) != null : "no mirror for hidden side " + face + " at " + y + ", " + pitch;
               }
            }
            assert view.mirrorHits.size() >= hidden && view.mirrorHits.size() <= 3;
            for (int[] mirror : view.mirrorHits) {
               Rectangle box = box(mirror);
               assert box.x >= 0 && box.x + box.width <= 550 && box.y >= 44 && box.y + box.height <= 352
                     : "mirror " + mirror[0] + " outside the view: " + box + " at " + y + ", " + pitch;
               assert box.x + box.width < 275 - 100 || box.x > 275 + 100 : "mirror over the cube: " + box;
               for (int[] other : view.mirrorHits) {
                  assert other == mirror || !box.intersects(box(other)) : "mirrors overlap at " + y + ", " + pitch;
               }
               checked++;
            }
         }
      }
      assert checked > 100 : checked;

      // A click on the mirror of the side underneath turns that side to the front.
      look(view, -0.62, 0.48);
      int bottom = CubeGeometry.faceIndex("green");
      int[] mirror = mirrorOf(view, bottom);
      assert mirror != null && view.facingOf(bottom * 9) < 0;
      Rectangle box = box(mirror);
      int x = (int) box.getCenterX();
      int y = (int) box.getCenterY();
      view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
      view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1));
      assert view.facingOf(bottom * 9) > 0.3 : "the click turns the side underneath to the front";

      view.setMirrors(false);
      paint(view);
      assert view.mirrorHits.isEmpty() : "X hides the mirrors";

      File file = File.createTempFile("rubikscube-mirrors", ".txt");
      file.deleteOnExit();
      LevelRecords records = LevelRecords.load(file);
      assert records.mirrors() : "mirrors are on at first";
      records.record(2, 3, 2);
      records.setMirrors(false);
      LevelRecords again = LevelRecords.load(file);
      assert !again.mirrors() && again.stars(2) == 3 : "mirrors=off is saved beside the levels";
      again.setMirrors(true);
      assert LevelRecords.load(file).mirrors();
      System.out.println("MirrorCheck passed (" + checked + " mirrors)");
   }

   /** Puts the view at that yaw and pitch and paints until the mirrors have glided into place. */
   private static void look(CubeView3D view, double yaw, double pitch) throws Exception {
      for (String name : new String[]{"yaw", "targetYaw", "pitch", "targetPitch"}) {
         Field field = CubeView3D.class.getDeclaredField(name);
         field.setAccessible(true);
         field.set(view, name.toLowerCase().contains("yaw") ? yaw : pitch);
      }
      for (int i = 0; i < 80; i++) {
         paint(view);
      }
   }

   private static void paint(CubeView3D view) {
      view.paint(new BufferedImage(550, 352, BufferedImage.TYPE_INT_RGB).createGraphics());
   }

   private static int[] mirrorOf(CubeView3D view, int face) {
      for (int[] mirror : view.mirrorHits) {
         if (mirror[0] == face) {
            return mirror;
         }
      }
      return null;
   }

   private static Rectangle box(int[] poly) {
      Rectangle box = new Rectangle(poly[1], poly[2], 0, 0);
      for (int i = 1; i < poly.length; i += 2) {
         box.add(poly[i], poly[i + 1]);
      }
      return box;
   }
}
