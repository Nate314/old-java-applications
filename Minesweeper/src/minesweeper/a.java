package minesweeper;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;
import javax.swing.JButton;

public final class a extends JButton implements MouseListener {
   private boolean a = false;
   private e b;
   private Color c = new Color(0, 255, 0);
   private Color d = new Color(0, 0, 255);
   private Color e = new Color(255, 0, 0);
   private Color f = new Color(0, 200, 255);
   private Color g = new Color(255, 0, 255);
   private String h;
   private int i;
   private int j;
   private final int[] k = new int[3];
   private final int[] l = new int[3];
   private final int[] m = new int[8];
   private final int[] n = new int[8];

   public a(e var1, String var2, int var3, int var4) {
      this.b = var1;
      this.h = var2;
      this.i = var3;
      this.j = var4;
      this.setBackground(this.c);
      this.setText("");
      this.setForeground(new Color(255, 255, 255));
      this.addMouseListener(this);
      this.addKeyListener(new c(var1));
      this.k[0] = var3 - 1 < 0 ? -1 : var3 - 1;
      this.k[1] = var3;
      this.k[2] = var3 + 1 > var1.b - 1 ? -1 : var3 + 1;
      this.l[0] = var4 - 1 < 0 ? -1 : var4 - 1;
      this.l[1] = var4;
      this.l[2] = var4 + 1 > var1.c - 1 ? -1 : var4 + 1;

      for (int var5 = 0; var5 < 3; var5++) {
         if (this.k[var5] == -1 || this.l[var5] == -1) {
            this.k[var5] = -1;
            this.l[var5] = -1;
         }
      }

      for (int var6 = 0; var6 < 8; var6++) {
         this.m[var6] = this.k[var6 / 3];
         this.n[var6] = this.l[var6 % 3];
      }
   }

   private void a(MouseEvent var1) {
      if (this.h == " ") {
         for (int var2 = 0; var2 < 8; var2++) {
            if (this.m[var2] != -1
               && this.n[var2] != -1
               && this.b.g[this.m[var2]][this.n[var2]].getBackground() != this.d
               && !this.b.h[this.m[var2]][this.n[var2]]) {
               this.b.g[this.m[var2]][this.n[var2]].mouseClicked(var1);
               this.b.h[this.m[var2]][this.n[var2]] = true;
            }
         }
      }

      this.a();
   }

   private void a() {
      if (this.h != "b") {
         this.b.d.a(this.b.e = this.b.e + this.b.f * 6, 20, 2, 0, 0);

         try {
            Thread.sleep(2L);
         } catch (InterruptedException var2) {
            var2.printStackTrace();
         }

         if (this.b.e > 100) {
            this.b.f = -1;
         } else if (this.b.e < 25) {
            this.b.f = 1;
         }

         this.setBackground(this.d);
         this.setText(this.h);
      } else {
         this.setBackground(this.e);
         this.setText(this.h);

         for (int var1 = 0; var1 < this.b.b * this.b.c - 1; var1++) {
            if (this.b.g[var1 / this.b.b][var1 % this.b.b].h == "b") {
               this.b.g[var1 / this.b.b][var1 % this.b.b].setBackground(this.e);
               this.b.g[var1 / this.b.b][var1 % this.b.b].setText(this.b.g[var1 / this.b.b][var1 % this.b.b].h);
            }
         }

         int[][] var3 = new int[][]{{49, 67, 71, 75, 78}, {48, 67, 70, 73, 76}};
         this.b.d.a(var3[new Random().nextInt(2)], 750, 10, 127, 4096);
         this.b.a = true;
      }
   }

   @Override
   public final void mouseClicked(MouseEvent var1) {
      this.setFont(new Font("Times New Roman", 1, this.getHeight() / 2));
      if (!this.b.a) {
         this.b.h[this.i][this.j] = true;
         if (var1.getButton() == 1) {
            if (this.h == " ") {
               this.a(var1);
            } else {
               this.a();
            }
         }

         if (var1.getButton() == 3) {
            this.setBackground(this.getBackground() == this.c ? this.f : (this.getBackground() == this.f ? this.g : this.c));
            this.setText(this.getText() != "f" && this.getText() != "?" ? "f" : (this.getText() == "f" ? "?" : " "));
         }
      } else {
         this.b.dispose();
         new e();
      }

      String var5 = "";
      String var2 = "";

      for (int var3 = 0; var3 < this.b.b; var3++) {
         for (int var4 = 0; var4 < this.b.c; var4++) {
            var5 = var5 + (this.b.g[var3][var4].getText() == "" ? "0" : "1");
            var2 = var2 + "1";
         }
      }

      if (var5.equals(var2)) {
         this.b.a = true;
      }
   }

   @Override
   public final void mousePressed(MouseEvent var1) {
   }

   @Override
   public final void mouseReleased(MouseEvent var1) {
   }

   @Override
   public final void mouseEntered(MouseEvent var1) {
   }

   @Override
   public final void mouseExited(MouseEvent var1) {
   }
}
