package launcher;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * App pictures: the web gallery's thumbnails (web/public/shots, screenshots with a transparent desk), scaled once per
 * size. An app without a screenshot gets a drawn card with its initials. Each picture also lights its own box: a
 * blurred, dimmed copy blown up behind it (its glow), and its average color as a deep tint (its tone).
 */
final class Thumbs {
   private static final Color WELL = new Color(0x171a21);   // under the transparent desk, as the wells were before
   private final Map<String, BufferedImage> originals = new HashMap<>();
   private final Map<String, Image> scaled = new HashMap<>();
   private final Map<String, BufferedImage> blurs = new HashMap<>();
   private final Map<String, BufferedImage> glows = new HashMap<>();
   private final Map<String, Color> tones = new HashMap<>();

   /** The full-size picture: the screenshot, or the drawn card. */
   BufferedImage original(Catalog.App app) {
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

   /** Fills a rounded box with the picture's glow: the picture covering the box, blurred and dimmed. */
   void drawGlow(Graphics2D g, Catalog.App app, int x, int y, int w, int h, int arc) {
      if (w <= 0 || h <= 0) return;
      // One glow per app and box kind (arc): a resize replaces it rather than keeping one per width dragged through.
      BufferedImage glow = glows.get(app.id + "r" + arc);
      if (glow == null || glow.getWidth() != w || glow.getHeight() != h) {
         glow = glow(blurs.computeIfAbsent(app.id, id -> blur(original(app))), tone(app), w, h, arc);
         glows.put(app.id + "r" + arc, glow);
      }
      g.drawImage(glow, x, y, null);
   }

   /** The picture's color as a deep tint: its pixels averaged, the colorful ones counting most (a gray one stays gray). */
   Color tone(Catalog.App app) {
      return tones.computeIfAbsent(app.id, id -> tone(opaque(original(app))));
   }

   static Color tone(BufferedImage img) {
      double r = 0;
      double g = 0;
      double b = 0;
      double sum = 0;
      float[] hsb = new float[3];
      for (int y = 0; y < img.getHeight(); y += 2) {
         for (int x = 0; x < img.getWidth(); x += 2) {
            int p = img.getRGB(x, y);
            Color.RGBtoHSB(p >> 16 & 255, p >> 8 & 255, p & 255, hsb);
            double weight = 0.02 + hsb[1] * hsb[2];
            r += weight * (p >> 16 & 255);
            g += weight * (p >> 8 & 255);
            b += weight * (p & 255);
            sum += weight;
         }
      }
      Color.RGBtoHSB((int) (r / sum), (int) (g / sum), (int) (b / sum), hsb);
      return new Color(Color.HSBtoRGB(hsb[0], Math.min(1f, hsb[1] * 1.3f), 0.34f));
   }

   /** The picture made opaque, shrunk to 24 px wide and blown up four times again: a soft blur, made once per app. */
   static BufferedImage blur(BufferedImage img) {
      int sw = 24;
      int sh = Math.max(1, Math.round(24f * img.getHeight() / img.getWidth()));
      BufferedImage small = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = small.createGraphics();
      g.drawImage(opaque(img).getScaledInstance(sw, sh, Image.SCALE_AREA_AVERAGING), 0, 0, null);
      g.dispose();
      BufferedImage out = new BufferedImage(sw * 4, sh * 4, BufferedImage.TYPE_INT_RGB);
      g = out.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(small, 0, 0, sw * 4, sh * 4, null);
      g.dispose();
      return out;
   }

   /** The blurred picture covering a w x h box (its middle at the box's shape), rounded and dimmed toward its tone. */
   static BufferedImage glow(BufferedImage blur, Color tone, int w, int h, int arc) {
      double s = Math.max((double) w / blur.getWidth(), (double) h / blur.getHeight());
      int cw = Math.max(1, Math.min(blur.getWidth(), (int) Math.round(w / s)));
      int ch = Math.max(1, Math.min(blur.getHeight(), (int) Math.round(h / s)));
      int cx = (blur.getWidth() - cw) / 2;
      int cy = (blur.getHeight() - ch) / 2;
      BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = Ui.smooth(out.getGraphics());
      g.setColor(Color.WHITE);
      g.fillRoundRect(0, 0, w, h, arc, arc);
      g.setComposite(AlphaComposite.SrcIn);   // the rounded box's antialiased edge, kept as the glow's alpha
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(blur, 0, 0, w, h, cx, cy, cx + cw, cy + ch, null);
      g.setComposite(AlphaComposite.SrcAtop);
      // Dimmed toward its tone, so the picture in front stays the brightest thing and a dark one still shows its color.
      g.setColor(new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 130));
      g.fillRect(0, 0, w, h);
      g.dispose();
      return out;
   }

   static BufferedImage opaque(BufferedImage img) {
      BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = out.createGraphics();
      g.setColor(WELL);
      g.fillRect(0, 0, img.getWidth(), img.getHeight());
      g.drawImage(img, 0, 0, null);
      g.dispose();
      return out;
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
