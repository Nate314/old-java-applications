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
      Graphics2D g2;
      (g2 = (Graphics2D)graphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(new Color(0, 255, 255));
      g2.fillRect(0, 0, this.getWidth(), this.getHeight());
      int strokeWidthThenTopMargin = this.getWidth() > this.getHeight() ? this.getHeight() / 50 : this.getWidth() / 50;
      if (this.analogMode) {
         g2.setStroke(new BasicStroke((float)strokeWidthThenTopMargin));
         strokeWidthThenTopMargin = this.getHeight() / 50;
         int leftMargin = this.getHeight() / 50;
         int diameter;
         int radius;
         int hourHandLength = (radius = (diameter = this.getWidth() > this.getHeight() ? this.getHeight() - (strokeWidthThenTopMargin << 1) : this.getWidth() - (leftMargin << 1)) / 2) / 2;
         int minuteHandLength = (radius << 1) / 3;
         int secondHandLength = (radius << 1) / 3;
         double secondAngle = (Math.PI / 2) - (double)this.clock.seconds * 0.10471975511965977;
         double minuteAngle = (Math.PI / 2) - (double)this.clock.minutes * 0.10471975511965977;
         double hourAngle = (Math.PI / 2) - (double)this.clock.hours * (Math.PI / 6);
         g2.setColor(new Color(255, 255, 255));
         g2.fillOval(leftMargin / 2, strokeWidthThenTopMargin, diameter, diameter);
         g2.setColor(new Color(0, 0, 0));
         g2.drawOval(leftMargin / 2, strokeWidthThenTopMargin, diameter, diameter);
         g2.drawLine(
            leftMargin + radius,
            strokeWidthThenTopMargin + radius,
            (int)((double)(leftMargin + radius) + (double)hourHandLength * Math.cos(hourAngle)),
            (int)((double)(strokeWidthThenTopMargin + radius) - (double)hourHandLength * Math.sin(hourAngle))
         );
         g2.drawLine(
            leftMargin + radius,
            strokeWidthThenTopMargin + radius,
            (int)((double)(leftMargin + radius) + (double)minuteHandLength * Math.cos(minuteAngle)),
            (int)((double)(strokeWidthThenTopMargin + radius) - (double)minuteHandLength * Math.sin(minuteAngle))
         );
         g2.setColor(new Color(255, 0, 0));
         g2.drawLine(
            leftMargin + radius,
            strokeWidthThenTopMargin + radius,
            (int)((double)(leftMargin + radius) + (double)secondHandLength * Math.cos(secondAngle)),
            (int)((double)(strokeWidthThenTopMargin + radius) - (double)secondHandLength * Math.sin(secondAngle))
         );
      } else {
         g2.setColor(new Color(0, 0, 0));
         Font font = new Font("Times New Roman", 1, 100);
         g2.setFont(font);

         for (int textWidth = g2.getFontMetrics().stringWidth(this.clock.timeText) / 3; textWidth > this.getWidth(); textWidth = g2.getFontMetrics().stringWidth(this.clock.timeText) / 3) {
            font = new Font("Times New Roman", 1, font.getSize() - 1);
            g2.setFont(font);
         }

         String[] lines = this.clock.timeText.split("\\n");

         for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            g2.drawString(lines[lineIndex], 10, font.getSize() + lineIndex * font.getSize());
         }
      }
   }

   @Override
   public final void mouseClicked(MouseEvent event) {
   }

   @Override
   public final void mousePressed(MouseEvent event) {
   }

   @Override
   public final void mouseReleased(MouseEvent event) {
      this.analogMode = !this.analogMode;
   }

   @Override
   public final void mouseEntered(MouseEvent event) {
   }

   @Override
   public final void mouseExited(MouseEvent event) {
   }
}
