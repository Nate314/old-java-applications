package launcher;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * App pictures: the web gallery's thumbnails (web/site/shots, screenshots with a transparent desk), scaled once per
 * size. The desktop-only apps have no screenshot, so they get a drawn card with their initials.
 */
final class Thumbs {
   private final Map<String, BufferedImage> originals = new HashMap<>();
   private final Map<String, Image> scaled = new HashMap<>();

   private BufferedImage original(Catalog.App app) {
      return originals.computeIfAbsent(app.id, id -> {
         if (app.thumbnail != null) {
            try {
               BufferedImage img = ImageIO.read(app.thumbnail.toFile());
               if (img != null) return img;
            } catch (IOException ignored) {
               // fall through to the drawn card
            }
         }
         return drawn(app);
      });
   }

   /** Draws the app's picture centered in the box, as large as fits, never above its own size. */
   void draw(Graphics2D g, Catalog.App app, int x, int y, int w, int h) {
      BufferedImage img = original(app);
      double s = Math.min(1.0, Math.min((double) w / img.getWidth(), (double) h / img.getHeight()));
      int sw = Math.max(1, (int) Math.round(img.getWidth() * s));
      int sh = Math.max(1, (int) Math.round(img.getHeight() * s));
      Image pic = scaled.computeIfAbsent(app.id + "@" + sw + "x" + sh,
         k -> sw == img.getWidth() ? img : img.getScaledInstance(sw, sh, Image.SCALE_SMOOTH));
      g.drawImage(pic, x + (w - sw) / 2, y + (h - sh) / 2, null);
   }

   private static BufferedImage drawn(Catalog.App app) {
      BufferedImage img = new BufferedImage(480, 300, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = Ui.smooth(img.getGraphics());
      Color c = Ui.category(app.category);
      g.setPaint(new GradientPaint(0, 0, c.darker().darker(), 480, 300, new Color(0x15171d)));
      g.fillRoundRect(40, 20, 400, 260, 28, 28);
      g.setColor(new Color(255, 255, 255, 40));
      g.drawRoundRect(40, 20, 399, 259, 28, 28);
      StringBuilder initials = new StringBuilder();
      for (String word : app.name.split("[ +]")) {
         if (!word.isEmpty() && Character.isUpperCase(word.charAt(0))) initials.append(word.charAt(0));
      }
      if (app.name.contains("+")) initials.insert(1, '+');
      g.setFont(Ui.font(Font.BOLD, 110f));
      FontMetrics fm = g.getFontMetrics();
      String text = initials.toString();
      g.setColor(c);
      g.drawString(text, 240 - fm.stringWidth(text) / 2, 175);
      g.setFont(Ui.font(Font.BOLD, 22f));
      fm = g.getFontMetrics();
      g.setColor(new Color(255, 255, 255, 170));
      g.drawString("DESKTOP ONLY", 240 - fm.stringWidth("DESKTOP ONLY") / 2, 240);
      g.dispose();
      return img;
   }
}
