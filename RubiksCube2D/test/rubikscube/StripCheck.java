package rubikscube;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Self-check for the round strip under the 3D cube: it fills a pip per mixing move, says Your turn with three stars
 * in reach, keeps them to par and drops one past it, turns into Next once the level is solved (a press on it starts
 * the next level), and is gone after Solve and in free play. It stays clear of the cube on every layout. Needs a
 * display (it makes the window without showing it) and a scratch user.home (it saves level records). Run with java -ea.
 */
public class StripCheck {
   private static CubeFrame frame;

   public static void main(String[] args) throws Exception {
      for (int[] screen : new int[][]{{1920, 1080}, {360, 640}, {412, 800}, {900, 560}, {1366, 650}}) {
         SwingUtilities.invokeAndWait(() -> frame = new CubeFrame(new Dimension(screen[0], screen[1])));
         String name = screen[0] + "x" + screen[1];
         CubeView3D view = viewOf();
         SwingUtilities.invokeAndWait(() -> frame.startLevel(3));
         waitFor(() -> "Your turn".equals(view.stripLabel()), name + ": the mix ends on Your turn");
         int[] box = paint(view);
         check(box != null, name + ": the strip shows");
         check(box[0] >= 0 && box[0] + box[2] <= view.getWidth() && box[1] + box[3] <= view.getHeight(), name + ": the strip is inside the view");
         for (int[] hit : hits(view)) {
            for (int k = 1; k + 1 < hit.length; k += 2) {
               check(hit[k + 1] < box[1] || hit[k] < box[0] || hit[k] > box[0] + box[2], name + ": the strip is clear of the cube");
            }
         }
         frame.dispose();
      }

      SwingUtilities.invokeAndWait(() -> frame = new CubeFrame(new Dimension(1920, 1080)));
      CubeView3D view = viewOf();
      SwingUtilities.invokeAndWait(() -> frame.startLevel(1));
      waitFor(() -> "Your turn".equals(view.stripLabel()), "level 1's mix ends on Your turn");
      check((int) get(view, "stripPar") == 1 && (int) get(view, "stripDone") == 0 && (int) get(view, "stripStars") == 3,
            "a pip for par 1, none filled, three stars in reach");

      // A move off the way back fills a pip; Undo empties it.
      SwingUtilities.invokeAndWait(() -> {
         String[] hint = hint();
         frame.select(hint[0], Integer.parseInt(hint[2]));
         frame.applyUserMove(hint[1]);
         frame.applyUserMove(hint[1]);
      });
      check((int) get(view, "stripDone") == 2 && (int) get(view, "stripStars") == 2, "two moves on par 1: a pip and +1, two stars");
      SwingUtilities.invokeAndWait(() -> {
         frame.undo();
         frame.undo();
      });
      check((int) get(view, "stripDone") == 0 && (int) get(view, "stripStars") == 3, "Undo takes them off again");

      solve();
      check("Next: level 2".equals(view.stripLabel()) && (boolean) get(view, "stripNext"), "a solve turns the strip into Next: " + view.stripLabel());
      check((int) get(view, "stripStars") == 3, "solved at par: three stars");
      int[] button = paintButton(view);
      check(button != null, "Next is drawn as a button");
      SwingUtilities.invokeAndWait(() -> click(view, button[0] + button[2] / 2, button[1] + button[3] / 2));
      check((int) get(frame, "level") == 2, "Next on the strip starts level 2");
      check("Watch the mix".equals(view.stripLabel()) && (int) get(view, "stripStars") == -1, "its mix shows on the strip, no stars yet");
      waitFor(() -> "Watch the mix".equals(view.stripLabel()) && (int) get(view, "stripDone") >= 1, "the mix fills the pips");
      waitFor(() -> "Your turn".equals(view.stripLabel()), "level 2's mix ends");

      // A turn and its turn back, three times: the cube as the mix left it, 6 moves on par 2 (two stars up to 2 x 2 + 2).
      for (int i = 0; i < 3; i++) {
         SwingUtilities.invokeAndWait(() -> {
            frame.select("red", 1);
            frame.applyUserMove("w");
            frame.applyUserMove(MoveHistory.inverse("w"));
         });
      }
      check((int) get(view, "stripDone") == 6 && (int) get(view, "stripStars") == 2, "6 moves on par 2: two stars in reach");
      SwingUtilities.invokeAndWait(() -> {
         frame.select("red", 1);
         frame.applyUserMove("w");
      });
      check((int) get(view, "stripStars") == 1, "7 moves on par 2: one star");
      paint(view);

      SwingUtilities.invokeAndWait(() -> invoke(frame, "solveForPlayer"));
      check(view.stripLabel() == null, "Solve takes the strip away (the level is not counted)");
      frame.dispose();
      System.out.println("StripCheck passed.");
      System.exit(0);
   }

   /** Makes the hinted moves back to solved, the way a player would. */
   private static void solve() throws Exception {
      for (int i = 0; i < 40; i++) {
         boolean[] more = {false};
         SwingUtilities.invokeAndWait(() -> {
            String[] move = hint();
            if (move != null) {
               // select() marks the square; the hint itself would cap the stars, so it is read, not pressed.
               frame.select(move[0], Integer.parseInt(move[2]));
               frame.applyUserMove(move[1]);
               more[0] = true;
            }
         });
         if (!more[0]) {
            return;
         }
      }
   }

   private static String[] hint() {
      return ((MoveHistory) get(frame, "history")).hint();
   }

   private static CubeView3D viewOf() {
      for (Component c : ((JPanel) frame.getContentPane().getComponent(0)).getComponents()) {
         if (c instanceof CubeView3D) {
            return (CubeView3D) c;
         }
      }
      throw new AssertionError("no cube");
   }

   private static int[] paint(CubeView3D view) throws Exception {
      int[][] box = {null};
      SwingUtilities.invokeAndWait(() -> {
         BufferedImage image = new BufferedImage(view.getWidth(), view.getHeight(), BufferedImage.TYPE_INT_RGB);
         view.paint(image.getGraphics());
         box[0] = view.stripBox();
      });
      return box[0];
   }

   private static int[] paintButton(CubeView3D view) throws Exception {
      paint(view);
      return view.stripButton();
   }

   @SuppressWarnings("unchecked")
   private static List<int[]> hits(CubeView3D view) {
      return (List<int[]>) get(view, "hits");
   }

   private static void click(CubeView3D view, int x, int y) {
      invoke(view, "pressed", x, y, System.currentTimeMillis());
      invoke(view, "released", System.currentTimeMillis());
   }

   private static void invoke(Object target, String name, Object... args) {
      try {
         for (Method m : target.getClass().getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
               m.setAccessible(true);
               m.invoke(target, args);
               return;
            }
         }
      } catch (ReflectiveOperationException e) {
         throw new AssertionError(e);
      }
      throw new AssertionError("no " + name);
   }

   private static Object get(Object target, String name) {
      try {
         Field field = target.getClass().getDeclaredField(name);
         field.setAccessible(true);
         return field.get(target);
      } catch (ReflectiveOperationException e) {
         throw new AssertionError(e);
      }
   }

   private static void waitFor(java.util.function.BooleanSupplier done, String what) throws Exception {
      long end = System.currentTimeMillis() + 15000;
      while (System.currentTimeMillis() < end) {
         boolean[] ok = {false};
         SwingUtilities.invokeAndWait(() -> ok[0] = done.getAsBoolean());
         if (ok[0]) {
            return;
         }
         Thread.sleep(50);
      }
      throw new AssertionError("timed out: " + what);
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
