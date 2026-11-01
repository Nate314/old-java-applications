package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;

/** Draws an app's icon: its assets/<name>.png, or a colored tile with its initials when it has none. */
final class AppIcon {
   private static final Color[] TILE_COLORS = {
      new Color(0, 150, 255), new Color(232, 72, 85), new Color(46, 184, 114), new Color(245, 166, 35),
      new Color(142, 84, 233), new Color(0, 168, 168), new Color(230, 92, 160), new Color(96, 125, 139)
   };

   private AppIcon() {
   }

   static void paint(Graphics g, Image image, String name, int x, int y, int size) {
      smooth(g);
      int arc = size / 3;
      if (image != null) {
         // A light tile, so dark line-art icons (StockChecker, NumberGuess) show on the dark taskbar and menu.
         g.setColor(new Color(244, 247, 251));
         g.fillRoundRect(x, y, size, size, arc, arc);
         int inset = size / 8;
         g.drawImage(image, x + inset, y + inset, size - 2 * inset, size - 2 * inset, null);
         return;
      }
      g.setColor(TILE_COLORS[Math.floorMod(name.hashCode(), TILE_COLORS.length)]);
      g.fillRoundRect(x, y, size, size, arc, arc);
      g.setColor(new Color(255, 255, 255, 60));
      g.fillRoundRect(x, y, size, size / 2, arc, arc);
      String initials = initials(name);
      g.setColor(Color.WHITE);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, size * 2 / 5)));
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(initials, x + (size - metrics.stringWidth(initials)) / 2, y + (size + metrics.getAscent() - metrics.getDescent()) / 2);
   }

   /** The first two capitals of a CamelCase name ("FlappyBird" is "FB"), or its first letter. */
   static String initials(String name) {
      StringBuilder initials = new StringBuilder();
      for (int i = 0; i < name.length() && initials.length() < 2; i++) {
         if (Character.isUpperCase(name.charAt(i))) {
            initials.append(name.charAt(i));
         }
      }
      return initials.length() > 0 ? initials.toString() : name.substring(0, Math.min(1, name.length())).toUpperCase();
   }

   /** "UltimateTicTacToe" as "Ultimate Tic Tac Toe", for labels that wrap. */
   static String words(String name) {
      StringBuilder words = new StringBuilder();
      for (int i = 0; i < name.length(); i++) {
         char c = name.charAt(i);
         if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(name.charAt(i - 1))) {
            words.append(' ');
         }
         words.append(c);
      }
      return words.toString();
   }

   /** The text cut to fit the width, ending in "..." when it was cut. */
   static String fit(String text, FontMetrics metrics, int width) {
      if (metrics.stringWidth(text) <= width) {
         return text;
      }
      String cut = text;
      while (cut.length() > 1 && metrics.stringWidth(cut + "...") > width) {
         cut = cut.substring(0, cut.length() - 1);
      }
      return cut + "...";
   }

   static void smooth(Graphics g) {
      if (g instanceof Graphics2D) {
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      }
   }
}
