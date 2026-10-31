package ultimateticktacktoe;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JPanel;

public final class b extends JPanel {
   public boolean a = false;
   private String d = "none";
   public Color b = new Color(0, 255, 0);
   public a[] c = new a[9];
   private int e;
   private int f;

   public b(c var1, int var2) {
      int var3 = (int)(Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 3.5);
      this.setSize(var3, var3);
      this.setVisible(true);

      for (int var4 = 0; var4 < 9; var4++) {
         this.c[var4] = new a(this, var1, var2, var4);
      }

      this.e = this.getWidth() / 3;
      this.f = this.getHeight() / 3;
      this.setLayout(null);
      this.setBounds(0, 0, this.getWidth(), this.getHeight());
      this.setVisible(true);

      for (int var5 = 0; var5 < this.c.length; var5++) {
         this.add(this.c[var5]);
         this.c[var5].setVisible(true);
      }

      this.a();

      for (int var6 = 0; var6 < this.c.length; var6++) {
         this.c[var6].setLocation(var6 % 3 * this.c[var6].getWidth(), var6 / 3 * this.c[var6].getHeight());
      }

      this.repaint();
   }

   public final void a() {
      this.e = this.getWidth() / 3;
      this.f = this.getHeight() / 3;
      this.c[0].setBounds(0, 0, this.e, this.f);
      this.c[1].setBounds(this.e, 0, this.e, this.f);
      this.c[2].setBounds(2 * this.e, 0, this.e, this.f);
      this.c[3].setBounds(0, this.f, this.e, this.f);
      this.c[4].setBounds(this.e, this.f, this.e, this.f);
      this.c[5].setBounds(2 * this.e, this.f, this.e, this.f);
      this.c[6].setBounds(0, 2 * this.f, this.e, this.f);
      this.c[7].setBounds(this.e, 2 * this.f, this.e, this.f);
      this.c[8].setBounds(2 * this.e, 2 * this.f, this.e, this.f);
   }

   public final String b() {
      String[] var1 = new String[]{"X", "O"};
      String var2 = "none";
      if (this.d == "none") {
         for (int var3 = 0; var3 < 2; var3++) {
            if (this.c[0].getText() == var1[var3] && this.c[1].getText() == var1[var3] && this.c[2].getText() == var1[var3]
               || this.c[3].getText() == var1[var3] && this.c[4].getText() == var1[var3] && this.c[5].getText() == var1[var3]
               || this.c[6].getText() == var1[var3] && this.c[7].getText() == var1[var3] && this.c[8].getText() == var1[var3]
               || this.c[0].getText() == var1[var3] && this.c[4].getText() == var1[var3] && this.c[8].getText() == var1[var3]
               || this.c[2].getText() == var1[var3] && this.c[4].getText() == var1[var3] && this.c[6].getText() == var1[var3]
               || this.c[0].getText() == var1[var3] && this.c[3].getText() == var1[var3] && this.c[6].getText() == var1[var3]
               || this.c[1].getText() == var1[var3] && this.c[4].getText() == var1[var3] && this.c[7].getText() == var1[var3]
               || this.c[2].getText() == var1[var3] && this.c[5].getText() == var1[var3] && this.c[8].getText() == var1[var3]) {
               var2 = var1[var3];
            }

            if (this.c[0].getText() != ""
               && this.c[1].getText() != ""
               && this.c[2].getText() != ""
               && this.c[3].getText() != ""
               && this.c[4].getText() != ""
               && this.c[5].getText() != ""
               && this.c[6].getText() != ""
               && this.c[7].getText() != ""
               && this.c[8].getText() != "") {
               var2 = "draw";
            }
         }
      }

      this.d = var2;
      return var2;
   }
}
