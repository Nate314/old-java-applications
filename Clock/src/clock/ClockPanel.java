package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;

public final class ClockPanel extends JPanel implements MouseListener {
   private Main clock;
   private boolean analogMode = true;

   public ClockPanel(Main clock) {
      this.clock = clock;
      this.addMouseListener(this);
   }

   @Override
   public final void paint(Graphics graphics) {
      Graphics2D g2 = (Graphics2D)graphics;
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setColor(new Color(0, 255, 255));
      g2.fillRect(0, 0, this.getWidth(), this.getHeight());
      int margin = Math.min(this.getWidth(), this.getHeight()) / 50;
      if (this.analogMode) {
         g2.setStroke(new BasicStroke((float)margin));
         int diameter = Math.min(this.getWidth(), this.getHeight()) - (margin << 1);
         int radius = diameter / 2;
         int centerX = margin + radius;
         int centerY = margin + radius;
         double seconds = this.clock.seconds + this.clock.milliseconds / 1000.0;
         double minutes = this.clock.minutes + seconds / 60.0;
         double hours = this.clock.hours % 12 + minutes / 60.0;
         g2.setColor(new Color(255, 255, 255));
         g2.fillOval(margin, margin, diameter, diameter);
         g2.setColor(new Color(0, 0, 0));
         g2.drawOval(margin, margin, diameter, diameter);
         drawHand(g2, centerX, centerY, radius / 2, (Math.PI / 2) - hours * (Math.PI / 6));
         drawHand(g2, centerX, centerY, (radius << 1) / 3, (Math.PI / 2) - minutes * (Math.PI / 30));
         g2.setColor(new Color(255, 0, 0));
         drawHand(g2, centerX, centerY, radius * 9 / 10, (Math.PI / 2) - seconds * (Math.PI / 30));
      } else {
         g2.setColor(new Color(0, 0, 0));
         String[] lines = this.clock.timeText.split("\\n");
         Font font = new Font("Times New Roman", 1, 100);
         g2.setFont(font);

         // Shrink until the widest line fits across and all lines fit down.
         while (font.getSize() > 1 && (widestLine(g2, lines) > this.getWidth() - 20 || font.getSize() * lines.length > this.getHeight() - 10)) {
            font = new Font("Times New Roman", 1, font.getSize() - 1);
            g2.setFont(font);
         }

         for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            g2.drawString(lines[lineIndex], 10, font.getSize() + lineIndex * font.getSize());
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

   @Override
   public final void mouseClicked(MouseEvent event) {
   }

   @Override
   public final void mousePressed(MouseEvent event) {
   }

   @Override
   public final void mouseReleased(MouseEvent event) {
      if (event.getButton() == MouseEvent.BUTTON3) {
         this.clock.twentyFourHour = !this.clock.twentyFourHour;
      } else {
         this.analogMode = !this.analogMode;
      }
   }

   @Override
   public final void mouseEntered(MouseEvent event) {
   }

   @Override
   public final void mouseExited(MouseEvent event) {
   }
}
