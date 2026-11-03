package circlewindow;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Self-check for morph matching, drawn shapes and the drawing pad. Run with java -ea (headless is fine). */
public class MorphCheck {
   public static void main(String[] args) {
      // Matching: every wanted rectangle gets one, each used once while any are left, the nearest first.
      List<Rectangle> have = Arrays.asList(new Rectangle(0, 0, 10, 10), new Rectangle(90, 0, 10, 10), new Rectangle(0, 90, 10, 10));
      List<Rectangle> mirrored = Arrays.asList(new Rectangle(500, 590, 10, 10), new Rectangle(590, 500, 10, 10), new Rectangle(500, 500, 10, 10));
      int[] m = Morph.match(mirrored, have, false);
      check(Arrays.equals(m, new int[] {2, 1, 0}), "same places in their shapes pair up, wherever the shapes are: " + Arrays.toString(m));

      List<Rectangle> more = Arrays.asList(new Rectangle(0, 0, 5, 5), new Rectangle(5, 0, 5, 5), new Rectangle(95, 0, 5, 5),
         new Rectangle(0, 95, 5, 5), new Rectangle(50, 50, 5, 5));
      m = Morph.match(more, have, false);
      Set<Integer> used = new HashSet<>();
      for (int s : m) {
         check(s >= 0 && s < have.size(), "every new window has a source");
         used.add(s);
      }
      check(used.size() == have.size(), "with more windows wanted, every old one is reused");

      m = Morph.match(have, more, false);
      check(new HashSet<>(Arrays.asList(m[0], m[1], m[2])).size() == 3, "with fewer wanted, no source is shared");
      check(Morph.match(have, Arrays.asList(), false).length == 3 && Morph.match(have, Arrays.asList(), false)[0] == -1, "nothing to match gives -1");
      check(Morph.match(Arrays.asList(), have, false).length == 0, "nothing wanted gives nothing");

      // Drawing: the smile builds a few dozen windows inside the size asked for; an empty pad builds none.
      DrawPad pad = new DrawPad(() -> { });
      pad.smile();
      List<Rectangle> smile = Shapes.build(Shapes.DRAW, 200, "", pad.cells(), 500, 400, 900);
      check(smile.size() > 5 && smile.size() < 80, "smile is " + smile.size() + " windows");
      Rectangle area = new Rectangle(smile.get(0));
      for (Rectangle r : smile) {
         area.add(r);
      }
      check(area.width <= 200 && area.height <= 200 && area.contains(500, 400), "smile fits and is centered: " + area);
      check(Shapes.build(Shapes.DRAW, 200, "", new boolean[DrawPad.CELLS][DrawPad.CELLS], 500, 400, 900).isEmpty(), "empty pad, no windows");

      // The pad's saved text round-trips, and damaged text reads as far as it goes.
      String text = pad.text();
      DrawPad copy = new DrawPad(() -> { });
      copy.setText(text);
      check(copy.text().equals(text), "drawing round-trips");
      copy.setText("##/x");
      check(copy.cells()[0][0] && copy.cells()[0][1] && !copy.cells()[0][2] && !copy.cells()[1][0], "short rows read what is there");
      int[] changes = {0};
      DrawPad counted = new DrawPad(() -> changes[0]++);
      counted.clear();
      check(changes[0] == 1, "clear tells the window");
      // Clock: a desk clock's time, and from one second to the next the digits that did not change keep their windows.
      check("9:05:07".equals(Shapes.clockText(new Date(2026 - 1900, 0, 1, 21, 5, 7))), "clock text at 21:05:07");
      check("12:00:00".equals(Shapes.clockText(new Date(2026 - 1900, 0, 1, 0, 0, 0))), "clock text at midnight");
      check(PixelFont.has(':') && PixelFont.lit(':', 2, 1) && !PixelFont.lit(':', 2, 3), "the font has a colon");
      List<Rectangle> before = Shapes.build(Shapes.CLOCK, 200, "12:34:56", null, 500, 400, 900);
      List<Rectangle> after = Shapes.build(Shapes.CLOCK, 200, "12:34:51", null, 500, 400, 900);
      int[] tick = Morph.match(after, before, true);
      int stayed = 0;
      int same = 0;
      for (int i = 0; i < after.size(); i++) {
         if (before.contains(after.get(i))) {
            same++;
            stayed += before.get(tick[i]).equals(after.get(i)) ? 1 : 0;
         }
      }
      check(same > 20 && stayed == same, "a tick keeps " + stayed + " of the " + same + " windows that did not change");
      System.out.println("MorphCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
