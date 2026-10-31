package rubikscube;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public final class b implements ActionListener {
   private a c = null;
   private String d = null;
   private JButton[] e = null;
   private int f = 0;
   private int g = 0;
   protected Panel a = new Panel();
   protected String[] b = new String[9];
   private int[] h = new int[9];
   private int[] i = new int[9];

   public b(a var1, String var2, JButton[] var3, int[] var4, int var5, int var6) {
      this.c = var1;
      this.d = var2;
      this.e = var3;
      this.f = var5;
      this.g = var6;

      for (int var7 = 0; var7 < 9; var7++) {
         this.b[var7] = var2;
      }

      this.a.setLayout(null);
      this.a.setBounds(0, 0, var5 * 3, var6 * 3);

      for (int var8 = 0; var8 < 9; var8++) {
         this.h[var8] = var8 % 3 * 50 + 2;
         this.i[var8] = var8 / 3 * 50 + 2;
      }

      for (int var9 = 0; var9 < 9; var9++) {
         this.e[var9] = new JButton();
         this.e[var9].addActionListener(this);
         this.e[var9].setVisible(true);
         int var10 = var4[var9] - 1;
         this.e[var9].setBounds(this.h[var10], this.i[var10], 46, 46);
         this.e[var9].setBackground(this.c.b.a(this.b[var9]));
         this.a.add(this.e[var9]);
      }
   }

   public final void a() {
      for (int var1 = 0; var1 < 9; var1++) {
         this.h[var1] = var1 % 3 * this.f + 2;
         this.i[var1] = var1 / 3 * this.g + 2;
      }

      for (int var2 = 0; var2 < 9; var2++) {
         this.e[var2].setBackground(this.c.b.a(this.b[var2]));
      }
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if (var1.getSource() == this.e[0]) {
         this.c.i = this.d;
         System.out.println(this.d + " 1");
         this.c.a.a = 1;
      }

      if (var1.getSource() == this.e[1]) {
         this.c.i = this.d;
         System.out.println(this.d + " 2");
         this.c.a.a = 2;
      }

      if (var1.getSource() == this.e[2]) {
         this.c.i = this.d;
         System.out.println(this.d + " 3");
         this.c.a.a = 3;
      }

      if (var1.getSource() == this.e[3]) {
         this.c.i = this.d;
         System.out.println(this.d + " 4");
         this.c.a.a = 4;
      }

      if (var1.getSource() == this.e[4]) {
         this.c.i = this.d;
         System.out.println(this.d + " 5");
         this.c.a.a = 5;
      }

      if (var1.getSource() == this.e[5]) {
         this.c.i = this.d;
         System.out.println(this.d + " 6");
         this.c.a.a = 6;
      }

      if (var1.getSource() == this.e[6]) {
         this.c.i = this.d;
         System.out.println(this.d + " 7");
         this.c.a.a = 7;
      }

      if (var1.getSource() == this.e[7]) {
         this.c.i = this.d;
         System.out.println(this.d + " 8");
         this.c.a.a = 8;
      }

      if (var1.getSource() == this.e[8]) {
         this.c.i = this.d;
         System.out.println(this.d + " 9");
         this.c.a.a = 9;
      }
   }
}
