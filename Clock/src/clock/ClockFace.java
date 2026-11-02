package clock;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** The original clock: an analog face with a red second hand, or the day, date and time as text. */
final class ClockFace implements Tab {
   private final Main clock;

   ClockFace(Main clock) {
      this.clock = clock;
   }

   @Override
   public String label(long now) {
      return "Clock";
   }

   @Override
   public String hint() {
      return (this.clock.state.analog ? "Click: digital" : "Click: analog") + "   Right click: 12/24 h   1 2 3: tabs";
   }

   @Override
   public void paint(Graphics2D g2, Rectangle area, long now) {
      int margin = Math.min(area.width, area.height) / 50;
      if (this.clock.state.analog) {
         g2.setStroke(new BasicStroke((float)margin));
         int diameter = Math.min(area.width, area.height) - (margin << 1);
         int radius = diameter / 2;
         int left = area.x + (area.width - diameter) / 2;
         int top = area.y + (area.height - diameter) / 2;
         int centerX = left + radius;
         int centerY = top + radius;
         double seconds = this.clock.seconds + this.clock.milliseconds / 1000.0;
         double minutes = this.clock.minutes + seconds / 60.0;
         double hours = this.clock.hours % 12 + minutes / 60.0;
         g2.setColor(Ui.FACE);
         g2.fillOval(left, top, diameter, diameter);
         g2.setColor(Ui.INK);
         g2.drawOval(left, top, diameter, diameter);
         drawHand(g2, centerX, centerY, radius / 2, (Math.PI / 2) - hours * (Math.PI / 6));
         drawHand(g2, centerX, centerY, (radius << 1) / 3, (Math.PI / 2) - minutes * (Math.PI / 30));
         g2.setColor(Ui.HAND);
         drawHand(g2, centerX, centerY, radius * 9 / 10, (Math.PI / 2) - seconds * (Math.PI / 30));
      } else {
         g2.setColor(Ui.INK);
         String[] lines = this.clock.timeText.split("\\n");
         Font font = new Font(Ui.DIGITS, Font.BOLD, 100);
         g2.setFont(font);

         // Shrink until the widest line fits across and all lines fit down.
         while (font.getSize() > 1 && (widestLine(g2, lines) > area.width - 20 || font.getSize() * lines.length > area.height - 10)) {
            font = new Font(Ui.DIGITS, Font.BOLD, font.getSize() - 1);
            g2.setFont(font);
         }

         int top = area.y + (area.height - font.getSize() * lines.length) / 2;
         for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            g2.drawString(lines[lineIndex], area.x + 10, top + font.getSize() + lineIndex * font.getSize());
         }
      }
   }

   private static void drawHand(Graphics2D g2, int centerX, int centerY, int length, double angle) {
      g2.drawLine(centerX, centerY, (int)(centerX + length * Math.cos(angle)), (int)(centerY - length * Math.sin(angle)));
   }

   private static int widestLine(Graphics2D g2, String[] lines) {
      int widest = 0;
      for (String line : lines) {
         widest = Math.max(widest, g2.getFontMetrics().stringWidth(line));
      }
      return widest;
   }

   private boolean rightPressed;

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      this.rightPressed = rightButton;
   }

   @Override
   public void drag(int x, int y, long now) {
   }

   /** As in the original, on release: left click switches analog and digital, right click 12 and 24 hours. */
   @Override
   public void release(int x, int y, long now) {
      if (this.rightPressed) {
         this.clock.state.twentyFour = !this.clock.state.twentyFour;
      } else {
         this.clock.state.analog = !this.clock.state.analog;
      }
   }

   @Override
   public boolean key(int keyCode, long now) {
      return false;
   }
}
