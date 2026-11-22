package stockchecker;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Self-check for the desk's two layouts (wide, and narrow for a phone held upright): every button showing lies inside
 * the desk and no two overlap, with the market open and at the close, and the desk paints in both. Run with java -ea.
 */
public class LayoutCheck {
   public static void main(String[] args) throws Exception {
      for (boolean narrow : new boolean[] {false, true}) {
         File file = File.createTempFile("stockchecker-layout", ".txt");
         file.deleteOnExit();
         Desk desk = Desk.load(file, Arrays.asList("CRM", "LMT", "TSN", "NFLX", "SWK"), new HashMap<>());
         desk.setSpeed(Desk.PAUSED);
         DeskPanel panel = new DeskPanel(desk, narrow);
         String name = narrow ? "narrow" : "wide";
         int width = panel.getPreferredSize().width;
         int height = panel.getPreferredSize().height;
         check(width == (narrow ? 360 : 720) && height == (narrow ? 680 : 440), name + " desk size");
         desk.buy("CRM", 10);
         desk.market.minute = 200;
         paintAndCheck(panel, name + ", open", width, height, 14);
         desk.market.minute = Market.CLOSE;
         paintAndCheck(panel, name + ", closed", width, height, 8);
      }
      System.out.println("LayoutCheck passed.");
      System.exit(0);
   }

   private static void paintAndCheck(DeskPanel panel, String what, int width, int height, int expected) {
      List<Rectangle> shown = panel.shownButtons();
      check(shown.size() == expected, what + ": " + expected + " buttons show, not " + shown.size());
      Rectangle desk = new Rectangle(0, 0, width, height);
      for (int i = 0; i < shown.size(); i++) {
         check(desk.contains(shown.get(i)), what + ": button " + shown.get(i) + " inside the desk");
         for (int j = i + 1; j < shown.size(); j++) {
            check(!shown.get(i).intersects(shown.get(j)), what + ": buttons " + shown.get(i) + " and " + shown.get(j) + " apart");
         }
      }
      panel.setSize(width, height);
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      panel.paint(image.createGraphics());
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
