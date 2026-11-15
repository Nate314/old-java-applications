package circlewindow;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

/**
 * The shape turning round its upright middle line like a coin on a table, seen in perspective: each window is
 * squeezed and moved to where its part of the flat shape would be, the near side drawn a little taller than
 * the far one, and lit brightest face on. Past a quarter turn the back shows, mirrored and darker.
 */
final class Spin {
   /** One whole turn, in milliseconds; by the clock, so a slow screen skips frames rather than turning slower. */
   static final long TURN_MILLIS = 2400;
   /** How far away the eye is, in half-widths of the shape: nearer makes the perspective stronger. */
   private static final double EYE = 3;
   /** How dark the back is, and the light edge-on, as a share of the front's color. */
   private static final double BACK = 0.7;
   private static final double EDGE_LIGHT = 0.45;

   private final List<Rectangle> home;
   private final List<Color> colors;
   private final double centerX;
   private final double centerY;
   private final double eye;

   /** Starts turning a shape whose windows lie flat at home, in these colors. */
   Spin(List<Rectangle> home, List<Color> colors) {
      this.home = home;
      this.colors = colors;
      Rectangle area = home.isEmpty() ? new Rectangle() : new Rectangle(home.get(0));
      for (Rectangle r : home) {
         area.add(r);
      }
      this.centerX = area.x + area.width / 2.0;
      this.centerY = area.y + area.height / 2.0;
      this.eye = EYE * Math.max(1, area.width / 2.0);
   }

   /** The angle, in radians, the shape has turned after spinning for the given time. */
   static double angle(long millis) {
      return 2 * Math.PI * (millis % TURN_MILLIS) / TURN_MILLIS;
   }

   /** Window i's bounds turned by angle (at least 1 pixel wide and tall). */
   Rectangle at(int i, double angle) {
      Rectangle r = this.home.get(i);
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      double left = this.screenX(r.x, cos, sin);
      double right = this.screenX(r.x + r.width, cos, sin);
      double scale = this.scale(r.x + r.width / 2.0, sin);
      double top = this.centerY + (r.y - this.centerY) * scale;
      double bottom = this.centerY + (r.y + r.height - this.centerY) * scale;
      int x = (int)Math.round(Math.min(left, right));
      int y = (int)Math.round(top);
      return new Rectangle(x, y, Math.max(1, (int)Math.round(Math.max(left, right)) - x),
         Math.max(1, (int)Math.round(bottom) - y));
   }

   /** Window i's color turned by angle: brightest face on, dimmer edge-on, darker from behind. */
   Color colorAt(int i, double angle) {
      double cos = Math.cos(angle);
      double light = (EDGE_LIGHT + (1 - EDGE_LIGHT) * Math.abs(cos)) * (cos < 0 ? BACK : 1);
      Color c = this.colors.get(i);
      return new Color((int)Math.round(c.getRed() * light), (int)Math.round(c.getGreen() * light),
         (int)Math.round(c.getBlue() * light));
   }

   /** Window i's flat bounds and color, to put it back when the spin stops. */
   Rectangle home(int i) {
      return this.home.get(i);
   }

   Color color(int i) {
      return this.colors.get(i);
   }

   private double screenX(double x, double cos, double sin) {
      return this.centerX + (x - this.centerX) * cos * this.scale(x, sin);
   }

   /** How much bigger a point x of the flat shape looks once turned: the side swinging nearer looks bigger. */
   private double scale(double x, double sin) {
      return this.eye / (this.eye - (x - this.centerX) * sin);
   }
}
