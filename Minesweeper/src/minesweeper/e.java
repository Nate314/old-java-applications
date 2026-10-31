package minesweeper;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;

public final class e extends JFrame {
   public boolean a = false;
   private int i = 15;
   public int b = this.i;
   public int c = this.i;
   public d d = new d();
   public int e = 60;
   public int f = 1;
   public a[][] g = new a[this.b][this.c];
   private JPanel j = new JPanel();
   private int k;
   private int l;
   public boolean[][] h = new boolean[this.b][this.c];

   public e() {
      int var1 = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() * 3.0 / 4.0);
      this.setSize(var1, var1);
      this.setDefaultCloseOperation(3);
      this.setTitle("AWT Minesweeper");
      this.setVisible(true);
      this.addKeyListener(new c(this));
      this.add(this.j);
      this.k = this.getWidth() / this.b;
      this.l = this.getHeight() / this.b;
      this.j.setBackground(new Color(0, 0, 0, 0));
      this.j.setLayout(null);
      this.j.setBounds(0, 0, this.getWidth(), this.getHeight());
      this.j.setVisible(true);

      try {
         String[][] var5 = new b(this.b, this.c).a();

         for (int var2 = 0; var2 < this.b; var2++) {
            for (int var3 = 0; var3 < this.c; var3++) {
               this.g[var2][var3] = new a(this, var5[var2][var3], var2, var3);
               this.j.add(this.g[var2][var3]);
               this.g[var2][var3].setVisible(true);
               this.g[var2][var3].setBounds(var2 * this.k, var3 * this.l, this.k, this.l);
               this.h[var2][var3] = false;
            }
         }

         this.setSize(this.getWidth() + this.k, this.getHeight() + this.l);
         this.repaint();
      } catch (NumberFormatException var4) {
         this.setVisible(false);
         new e();
      }
   }
}
