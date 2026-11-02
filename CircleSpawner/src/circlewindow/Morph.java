package circlewindow;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Turns the windows of one shape into another: each window of the new shape grows out of the old window that sits
 * at the most similar place in its shape, and glides, stretches and changes color into place. When the new shape
 * needs more windows, the extra ones split off old windows; when it needs fewer, the old ones left over shrink into
 * the new window nearest them and close.
 */
final class Morph {
   private final List<CircleSliceWindow> windows = new ArrayList<>();
   private final List<Rectangle> from = new ArrayList<>();
   private final List<Rectangle> to = new ArrayList<>();
   private final List<Color> fromColors = new ArrayList<>();
   private final List<Color> toColors = new ArrayList<>();
   /** Windows that shrink away and close at the end. */
   private final List<CircleSliceWindow> leaving = new ArrayList<>();
   private final int frames;
   private int frame;

   /**
    * Starts moving the shape's windows (old) to the rectangles target with colors. The shape's list of windows
    * becomes the new shape's at once, in target order; newWindow makes a window that splits off at a rectangle,
    * in a color.
    */
   Morph(List<CircleSliceWindow> old, List<Rectangle> target, List<Color> colors, int frames,
         BiFunction<Rectangle, Color, CircleSliceWindow> newWindow) {
      this.frames = Math.max(1, frames);
      List<Rectangle> oldBounds = new ArrayList<>();
      for (CircleSliceWindow w : old) {
         oldBounds.add(w.getBounds());
      }
      int[] source = match(target, oldBounds);
      boolean[] used = new boolean[old.size()];
      List<CircleSliceWindow> next = new ArrayList<>();
      for (int i = 0; i < target.size(); i++) {
         int s = source[i];
         CircleSliceWindow w;
         if (s >= 0 && !used[s]) {
            used[s] = true;
            w = old.get(s);
         } else {
            // Splits off: a new window at the old one's place, or out of the middle of the new rectangle.
            w = s >= 0 ? newWindow.apply(oldBounds.get(s), old.get(s).color()) : newWindow.apply(middle(target.get(i)), colors.get(i));
         }
         this.add(w, target.get(i), colors.get(i));
         next.add(w);
      }
      int[] sink = match(oldBounds, target);
      for (int s = 0; s < old.size(); s++) {
         if (!used[s]) {
            CircleSliceWindow w = old.get(s);
            this.add(w, middle(sink[s] >= 0 ? target.get(sink[s]) : oldBounds.get(s)), w.color());
            this.leaving.add(w);
         }
      }
      old.clear();
      old.addAll(next);
   }

   private void add(CircleSliceWindow w, Rectangle end, Color endColor) {
      this.windows.add(w);
      this.from.add(w.getBounds());
      this.to.add(new Rectangle(end));
      this.fromColors.add(w.color());
      this.toColors.add(endColor);
   }

   /** Moves every window one frame on; true once the shape has arrived (and the leftover windows are closed). */
   boolean step() {
      this.frame = Math.min(this.frames, this.frame + 1);
      double t = this.frame / (double)this.frames;
      t = t * t * (3.0 - 2.0 * t);
      for (int i = 0; i < this.windows.size(); i++) {
         Rectangle a = this.from.get(i);
         Rectangle b = this.to.get(i);
         Rectangle r = new Rectangle(mix(a.x, b.x, t), mix(a.y, b.y, t), Math.max(1, mix(a.width, b.width, t)),
            Math.max(1, mix(a.height, b.height, t)));
         this.windows.get(i).place(r, blend(this.fromColors.get(i), this.toColors.get(i), t));
      }
      if (this.frame < this.frames) {
         return false;
      }
      this.finish();
      return true;
   }

   /** Closes the windows that were shrinking away, wherever they got to (when a newer change takes over). */
   void finish() {
      for (CircleSliceWindow w : this.leaving) {
         w.dispose();
      }
      this.leaving.clear();
   }

   /**
    * For each rectangle in wanted, the index of the rectangle in have at the most similar place within its own
    * shape (centers and sizes as fractions of each shape's bounds), each one used once while any are left and
    * then shared; -1 when have is empty. Nearest pairs are taken first.
    */
   static int[] match(List<Rectangle> wanted, List<Rectangle> have) {
      int[] result = new int[wanted.size()];
      Arrays.fill(result, -1);
      if (have.isEmpty() || wanted.isEmpty()) {
         return result;
      }
      double[][] a = normalized(wanted);
      double[][] b = normalized(have);
      int pairs = wanted.size() * have.size();
      double[] cost = new double[pairs];
      for (int i = 0; i < wanted.size(); i++) {
         for (int j = 0; j < have.size(); j++) {
            int k = i * have.size() + j;
            cost[k] = Math.abs(a[i][0] - b[j][0]) + Math.abs(a[i][1] - b[j][1])
               + 0.5 * (Math.abs(a[i][2] - b[j][2]) + Math.abs(a[i][3] - b[j][3]));
         }
      }
      // Cheapest pairs first.
      Integer[] index = new Integer[pairs];
      for (int k = 0; k < pairs; k++) {
         index[k] = k;
      }
      Arrays.sort(index, (x, y) -> Double.compare(cost[x], cost[y]));
      boolean[] taken = new boolean[have.size()];
      int left = wanted.size();
      int free = have.size();
      while (left > 0) {
         for (int n = 0; n < pairs && left > 0; n++) {
            int k = index[n];
            int i = k / have.size();
            int j = k % have.size();
            if (result[i] < 0 && (!taken[j] || free == 0)) {
               result[i] = j;
               if (!taken[j]) {
                  taken[j] = true;
                  free--;
               }
               left--;
            }
         }
      }
      return result;
   }

   /** Each rectangle's center and size as fractions of the bounds of them all. */
   private static double[][] normalized(List<Rectangle> rects) {
      Rectangle area = new Rectangle(rects.get(0));
      for (Rectangle r : rects) {
         area.add(r);
      }
      double w = Math.max(1, area.width);
      double h = Math.max(1, area.height);
      double[][] out = new double[rects.size()][];
      for (int i = 0; i < rects.size(); i++) {
         Rectangle r = rects.get(i);
         out[i] = new double[] {(r.x + r.width / 2.0 - area.x) / w, (r.y + r.height / 2.0 - area.y) / h, r.width / w, r.height / h};
      }
      return out;
   }

   private static Rectangle middle(Rectangle r) {
      return new Rectangle(r.x + r.width / 2, r.y + r.height / 2, 1, 1);
   }

   private static int mix(int a, int b, double t) {
      return (int)Math.round(a + (b - a) * t);
   }

   private static Color blend(Color a, Color b, double t) {
      return new Color(mix(a.getRed(), b.getRed(), t), mix(a.getGreen(), b.getGreen(), t), mix(a.getBlue(), b.getBlue(), t));
   }
}
