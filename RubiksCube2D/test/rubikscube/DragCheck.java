package rubikscube;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;

/**
 * Checks that a drag on the 3D cube takes the layer by hand: it follows the pointer, becomes the move when let go
 * past half way (or flicked), the other way when dragged back past half way, springs back when let go short, and the
 * move's own turn goes on from where the drag left it.
 */
public final class DragCheck {
   private static final List<String> taken = new ArrayList<>();
   private static boolean refuse = false;
   private static CubeView3D view;
   private static String face;
   private static int square;

   public static void main(String[] args) throws Exception {
      view = new CubeView3D(new Cube(null, new JButton[6][9], 50, 50), (f, s) -> {
         face = f;
         square = s;
      }, key -> {
         taken.add(key);
         if (!refuse) {
            view.turned(CubeGeometry.turnOf(face, key, square));
         }
      });
      view.setSize(550, 352);
      paint();
      // The top-left square of the side most toward the viewer.
      int best = 0;
      for (int f = 1; f < 6; f++) {
         best = view.facingOf(f * 9) > view.facingOf(best * 9) ? f : best;
      }
      int sticker = best * 9;
      int[] at = centerOf(sticker);
      double[] along = view.screenAlong(sticker, "d", 1.6);

      drag(at, along, 0.3, 1000);
      assert taken.isEmpty() : "a slow drag let go short of half way makes no move";
      assert live() != null : "the layer is still out of place, springing back";
      ticks(30);
      assert live() == null : "it springs back";

      drag(at, along, 0.7, 1000);
      assert taken.equals(List.of("d")) : "past half way makes the move: " + taken;
      assert live() == null;
      ticks(1);
      assert (double) get("turnAngle") > 0.55 * Math.PI / 2 : "the turn goes on from where the drag left it";
      ticks(40);

      taken.clear();
      drag(at, along, -0.7, 1000);
      assert taken.equals(List.of("a")) : "dragged back past half way turns the other way: " + taken;
      ticks(40);

      taken.clear();
      drag(at, along, 0.25, 120);
      assert taken.equals(List.of("d")) : "a quick flick makes the move: " + taken;
      ticks(40);

      taken.clear();
      refuse = true;
      drag(at, along, 0.8, 1000);
      assert taken.size() == 1 && live() != null : "a move the frame does not take springs back";
      ticks(30);
      assert live() == null && (double) get("resumeFrom") == 0.0;
      refuse = false;

      taken.clear();
      view.canTurn = () -> false;
      press(at, 0);
      move(at, along, 0.3, 500);
      assert taken.size() == 1 && live() == null : "while the frame takes no moves, the drag asks at once as before";
      release(at, along, 0.3, 500);
      System.out.println("DragCheck passed");
   }

   private static void drag(int[] at, double[] along, double fraction, long millis) throws Exception {
      press(at, 0);
      for (int step = 1; step <= 10; step++) {
         move(at, along, (fraction > 0 ? 1 : -1) * Math.abs(fraction) * step / 10.0 + (fraction < 0 && step < 4 ? 0.4 : 0), millis * step / 10);
      }
      assert live() != null : "the layer follows the pointer";
      release(at, along, fraction, millis);
   }

   private static void press(int[] at, long when) {
      view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, when, 0, at[0], at[1], 1, false, MouseEvent.BUTTON1));
   }

   private static void move(int[] at, double[] along, double f, long when) {
      int x = (int) Math.round(at[0] + along[0] * f);
      int y = (int) Math.round(at[1] + along[1] * f);
      view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_DRAGGED, when, 0, x, y, 0, false, MouseEvent.BUTTON1));
   }

   private static void release(int[] at, double[] along, double f, long when) {
      int x = (int) Math.round(at[0] + along[0] * f);
      int y = (int) Math.round(at[1] + along[1] * f);
      view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, when, 0, x, y, 1, false, MouseEvent.BUTTON1));
   }

   private static int[] centerOf(int sticker) {
      long sx = 0;
      long sy = 0;
      int n = 0;
      for (int x = 0; x < 550; x += 2) {
         for (int y = 0; y < 352; y += 2) {
            if (view.stickerAt(x, y) == sticker) {
               sx += x;
               sy += y;
               n++;
            }
         }
      }
      assert n > 0 : "the square is in view";
      return new int[]{(int) (sx / n), (int) (sy / n)};
   }

   private static Object live() throws Exception {
      return get("liveTurn");
   }

   private static Object get(String name) throws Exception {
      Field field = CubeView3D.class.getDeclaredField(name);
      field.setAccessible(true);
      return field.get(view);
   }

   private static void ticks(int n) throws Exception {
      Method tick = CubeView3D.class.getDeclaredMethod("tick");
      tick.setAccessible(true);
      for (int i = 0; i < n; i++) {
         if (i % 10 == 9) {
            Thread.sleep(100);
         }
         tick.invoke(view);
         paint();
      }
   }

   private static void paint() {
      view.paint(new BufferedImage(550, 352, BufferedImage.TYPE_INT_RGB).createGraphics());
   }
}
