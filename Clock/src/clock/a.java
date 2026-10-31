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

public final class a extends JPanel implements MouseListener {
   private Main a;
   private boolean b = true;

   public a(Main var1) {
      this.a = var1;
      this.addMouseListener(this);
   }

   @Override
   public final void paint(Graphics var1) {
      Graphics2D var17;
      (var17 = (Graphics2D)var1).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      var17.setColor(new Color(0, 255, 255));
      var17.fillRect(0, 0, this.getWidth(), this.getHeight());
      int var2 = this.getWidth() > this.getHeight() ? this.getHeight() / 50 : this.getWidth() / 50;
      if (this.b) {
         var17.setStroke(new BasicStroke((float)var2));
         var2 = this.getHeight() / 50;
         int var20 = this.getHeight() / 50;
         int var21;
         int var22;
         int var6 = (var22 = (var21 = this.getWidth() > this.getHeight() ? this.getHeight() - (var2 << 1) : this.getWidth() - (var20 << 1)) / 2) / 2;
         int var7 = (var22 << 1) / 3;
         int var8 = (var22 << 1) / 3;
         double var11 = (Math.PI / 2) - (double)this.a.g * 0.10471975511965977;
         double var13 = (Math.PI / 2) - (double)this.a.f * 0.10471975511965977;
         double var15 = (Math.PI / 2) - (double)this.a.e * (Math.PI / 6);
         var17.setColor(new Color(255, 255, 255));
         var17.fillOval(var20 / 2, var2, var21, var21);
         var17.setColor(new Color(0, 0, 0));
         var17.drawOval(var20 / 2, var2, var21, var21);
         var17.drawLine(
            var20 + var22,
            var2 + var22,
            (int)((double)(var20 + var22) + (double)var6 * Math.cos(var15)),
            (int)((double)(var2 + var22) - (double)var6 * Math.sin(var15))
         );
         var17.drawLine(
            var20 + var22,
            var2 + var22,
            (int)((double)(var20 + var22) + (double)var7 * Math.cos(var13)),
            (int)((double)(var2 + var22) - (double)var7 * Math.sin(var13))
         );
         var17.setColor(new Color(255, 0, 0));
         var17.drawLine(
            var20 + var22,
            var2 + var22,
            (int)((double)(var20 + var22) + (double)var8 * Math.cos(var11)),
            (int)((double)(var2 + var22) - (double)var8 * Math.sin(var11))
         );
      } else {
         var17.setColor(new Color(0, 0, 0));
         Font var18 = new Font("Times New Roman", 1, 100);
         var17.setFont(var18);

         for (int var3 = var17.getFontMetrics().stringWidth(this.a.h) / 3; var3 > this.getWidth(); var3 = var17.getFontMetrics().stringWidth(this.a.h) / 3) {
            var18 = new Font("Times New Roman", 1, var18.getSize() - 1);
            var17.setFont(var18);
         }

         String[] var4 = this.a.h.split("\\n");

         for (int var5 = 0; var5 < var4.length; var5++) {
            var17.drawString(var4[var5], 10, var18.getSize() + var5 * var18.getSize());
         }
      }
   }

   @Override
   public final void mouseClicked(MouseEvent var1) {
   }

   @Override
   public final void mousePressed(MouseEvent var1) {
   }

   @Override
   public final void mouseReleased(MouseEvent var1) {
      this.b = !this.b;
   }

   @Override
   public final void mouseEntered(MouseEvent var1) {
   }

   @Override
   public final void mouseExited(MouseEvent var1) {
   }
}
