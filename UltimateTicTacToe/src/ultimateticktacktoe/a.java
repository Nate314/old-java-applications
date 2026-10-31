package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public final class a extends JButton implements ActionListener {
   private b a;
   private c b;
   private int c;
   private int d;
   private boolean e = false;
   private Color f = this.getBackground();
   private Color g = new Color(0, 0, 255);
   private Color h = new Color(255, 0, 0);

   public a(b var1, c var2, int var3, int var4) {
      this.a = var1;
      this.b = var2;
      this.d = var4;
      this.c = var3;
      this.setBackground(new Color(0, 255, 0));
      this.setFont(new Font("Times New Roman", 1, 55));
      this.setText("");
      this.setForeground(this.f);
      this.addActionListener(this);
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if ((this.b.a == this.c || this.b.b) && !this.e) {
         this.e = true;
         this.b.b = false;
         this.b.a = this.d;

         for (int var3 = 0; var3 < this.b.d.length; var3++) {
            for (int var2 = 0; var2 < this.a.c.length; var2++) {
               this.b.d[var3].c[var2].setBackground(this.b.d[var3].b);
            }
         }

         for (int var4 = 0; var4 < this.a.c.length; var4++) {
            this.b.a = this.d;
            this.b.d[this.b.a].c[var4].setBackground(new Color(255, 255, 0));
         }

         this.b.a();
         if (this.getForeground() == this.f) {
            if (this.b.c) {
               this.setText("X");
               this.setForeground(this.g);
               this.a.a();
            }

            if (!this.b.c) {
               this.setText("O");
               this.setForeground(this.h);
               this.a.a();
            }

            this.b.c = !this.b.c;
         }

         String var5;
         if ((var5 = this.a.b()) != "none") {
            if (var5 == "X" && this.a.b != new Color(0, 255, 0)) {
               for (int var7 = 0; var7 < this.a.c.length; var7++) {
                  this.a.b = new Color(0, 255, 255);
                  this.a.c[var7].setBackground(this.a.b);
               }

               return;
            }

            if (var5 == "O" && this.a.b != new Color(0, 255, 0)) {
               for (int var6 = 0; var6 < this.a.c.length; var6++) {
                  this.a.b = new Color(255, 175, 175);
                  this.a.c[var6].setBackground(this.a.b);
               }
            }
         }
      }
   }
}
