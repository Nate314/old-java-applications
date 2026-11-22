package groupcreator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/** A person's badge: their initials in a circle of their own color, the same on every card, tile and view. */
final class Badge {
   private Badge() {
   }

   /** "A" for Alice, "MJ" for Mary Jane or Mary-Jane, "" for a blank name. */
   static String initials(String name) {
      String out = "";
      for (String word : name.trim().split("[\\s\\-]+")) {
         if (!word.isEmpty() && out.length() < 2) {
            out += Character.toUpperCase(word.charAt(0));
         }
      }
      return out;
   }

   /**
    * The hue of the name's place in the class, stepped by the golden angle so neighbours in the names field never look
    * alike; a name not in the class (an earlier round's) falls back to its hash.
    */
   static float hue(String name, List<String> people) {
      int i = people.indexOf(name);
      int step = i >= 0 ? i : Math.floorMod(name.hashCode(), 97);
      return (step * 0.618034f + 0.58f) % 1f;
   }

   /** A light fill, so the initials read in the board's ink on every hue. */
   static Color fill(float hue) {
      return Color.getHSBColor(hue, 0.34f, 0.98f);
   }

   static Color ring(float hue) {
      return Color.getHSBColor(hue, 0.62f, 0.72f);
   }

   /** The badge with its top-left corner at x, y, d across. */
   static void paint(Graphics2D g, String name, List<String> people, int x, int y, int d, boolean dim) {
      float hue = hue(name, people);
      g.setColor(dim ? new Color(232, 228, 220) : fill(hue));
      g.fillOval(x, y, d, d);
      g.setColor(dim ? new Color(190, 184, 172) : ring(hue));
      g.setStroke(new BasicStroke(d >= 24 ? 2f : 1.2f));
      g.drawOval(x, y, d, d);
      g.setStroke(new BasicStroke(1f));
      String letters = initials(name);
      Font font = new Font("SansSerif", Font.BOLD, Math.max(8, (int) Math.round(d * (letters.length() > 1 ? 0.42 : 0.52))));
      GroupBoard.centered(g, letters, font, dim ? GroupBoard.MUTED : GroupBoard.INK, x + d / 2,
         y + d / 2 + Math.round(font.getSize() * 0.37f));
   }
}
