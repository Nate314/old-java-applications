package animate2;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;

public final class a extends Component {
   public int a = 1000;
   private int b = 20;
   private int c;
   private int d;
   private double[] e = new double[this.a];
   private double[] f = new double[this.a];
   private double[] g = new double[this.a];
   private double[] h = new double[this.a];
   private int[] i = new int[this.a * 3];
   private int j = 2;

   public a(int var1, Dimension var2, int var3, int var4) {
      this.j = var1;
      this.a = var3;
      this.b = var4;

      for (int var5 = 0; var5 < this.a; var5++) {
         if (this.j == 0 || this.j == 4) {
            this.e[var5] = Math.random() * var2.getWidth();
            this.f[var5] = Math.random() * var2.getHeight();
            this.g[var5] = Math.random();
            this.h[var5] = Math.random();
         } else if (this.j == 1 || this.j == 2) {
            this.e[var5] = var2.getWidth() / 2.0 - (double)(this.b / 2);
            this.f[var5] = var2.getHeight() / 2.0 - (double)(this.b / 2);
            this.g[var5] = 2.0 * (Math.random() - 0.5);
            this.h[var5] = 2.0 * (Math.random() - 0.5);
         }
      }

      for (int var6 = 0; var6 < this.a * 3; var6++) {
         this.i[var6] = (int)(Math.random() * 255.0);
      }
   }

   @Override
   public final void paint(Graphics var1) {
      super.paint(var1);
      this.c = this.getWidth();
      this.d = this.getHeight();
      var1.setColor(Color.blue);
      var1.fillRect(0, 0, this.c, this.d);
      if (this.j == 0) {
         Graphics var2 = var1;
         a var10 = this;

         for (int var3 = 0; var3 < var10.a; var3++) {
            var10.a(var2, var3);
            var10.e[var3] = var10.e[var3] - var10.g[var3];
            var10.f[var3] = var10.f[var3] + var10.h[var3];
            if (var10.e[var3] < (double)(0 - var10.b)) {
               var10.e[var3] = var10.e[var3] + (double)(var10.c + 2 * var10.b);
            } else if (var10.f[var3] > (double)(var10.d + var10.b)) {
               var10.f[var3] = var10.f[var3] - (double)(var10.d + 2 * var10.b);
            }

            var2.fillOval((int)var10.e[var3], (int)var10.f[var3], var10.b, var10.b);
         }
      } else if (this.j == 1) {
         this.a(var1);
      } else if (this.j == 2) {
         this.a(var1);
      } else if (this.j == 3) {
         Graphics var13 = var1;
         a var11 = this;

         for (int var15 = 0; var15 < var11.a; var15++) {
            var11.a(var13, var15);
            var11.e[var15] = var11.e[var15] - var11.g[var15];
            var11.f[var15] = var11.f[var15] - var11.h[var15];
            int var4 = (int)var11.e[var15];
            int var6 = (int)var11.f[var15];
            if (var4 < 0) {
               var11.g[var15] = -var11.g[var15];
            } else if (var4 > var11.c - var11.b) {
               var11.g[var15] = -var11.g[var15];
            } else if (var6 > var11.d - var11.b) {
               var11.h[var15] = -var11.h[var15];
            } else if (var6 < 0) {
               var11.h[var15] = -var11.h[var15];
            }

            var13.fillOval(var4, var6, var11.b, var11.b);
         }
      } else if (this.j == 4) {
         Graphics var14 = var1;
         a var12 = this;

         for (int var16 = 0; var16 < var12.a; var16++) {
            var12.a(var14, var16);

            for (int var17 = 0; var17 < var12.a; var17++) {
               if (var12.e[var16] < var12.e[var17]
                  && var12.e[var16] + (double)var12.b > var12.e[var17]
                  && var12.f[var16] < var12.f[var17]
                  && var12.f[var16] + (double)var12.b > var12.f[var17]) {
                  double var18 = (var12.g[var16] + var12.g[var17]) / 2.0;
                  double var8 = (var12.h[var16] + var12.h[var17]) / 2.0;
                  var12.g[var16] = var18;
                  var12.g[var17] = var18;
                  var12.h[var16] = var8;
                  var12.h[var17] = var8;
               }
            }

            var12.e[var16] = var12.e[var16] - var12.g[var16];
            var12.f[var16] = var12.f[var16] + var12.h[var16];
            if (var12.e[var16] < (double)(0 - var12.b)) {
               var12.e[var16] = var12.e[var16] + (double)(var12.c + 2 * var12.b);
            } else if (var12.f[var16] > (double)(var12.d + var12.b)) {
               var12.f[var16] = var12.f[var16] - (double)(var12.d + 2 * var12.b);
            }

            var14.fillOval((int)var12.e[var16], (int)var12.f[var16], var12.b, var12.b);
         }
      }

      this.repaint();
   }

   private void a(Graphics var1) {
      for (int var2 = 0; var2 < this.a; var2++) {
         this.a(var1, var2);
         this.e[var2] = this.e[var2] - this.g[var2];
         this.f[var2] = this.f[var2] - this.h[var2];
         int var3 = (int)this.e[var2];
         int var4 = (int)this.f[var2];
         if (var3 < 0) {
            this.g[var2] = -this.g[var2];
         } else if (var3 > this.c - this.b) {
            this.g[var2] = -this.g[var2];
         } else if (var4 > this.d - this.b) {
            this.h[var2] = -this.h[var2];
         } else if (var4 < 0) {
            this.h[var2] = -this.h[var2];
         }

         var1.fillOval(var3, var4, this.b, this.b);
      }
   }

   private void a(Graphics var1, int var2) {
      if (this.j == 0) {
         var1.setColor(Color.white);
      } else if (this.j == 1) {
         int var6 = (int)(this.e[var2] / (double)this.c * 250.0);
         var2 = (int)(this.f[var2] / (double)this.c * 250.0);
         var1.setColor(new Color(var6, var2, var2));
      } else {
         if (this.j == 2) {
            int var3 = (int)(this.g[var2] / Math.abs(this.g[var2]));
            var2 = (int)(this.h[var2] / Math.abs(this.h[var2]));
            if (var3 == 1 && var2 == 1) {
               var1.setColor(new Color(255, 0, 0));
               return;
            }

            if (var3 == 1 && var2 == -1) {
               var1.setColor(new Color(0, 255, 0));
               return;
            }

            if (var3 == -1 && var2 == -1) {
               var1.setColor(new Color(255, 0, 255));
               return;
            }

            if (var3 == -1 && var2 == 1) {
               var1.setColor(new Color(0, 255, 255));
               return;
            }
         } else if (this.j != 3 && this.j == 4) {
            var1.setColor(Color.white);
         }
      }
   }
}
