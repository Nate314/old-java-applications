package ultimateticktacktoe;

import java.awt.Toolkit;
import javax.swing.JFrame;

public final class c extends JFrame {
   public int a = 2;
   public boolean b = true;
   public boolean c = true;
   public b[] d = new b[]{
      new b(this, 0), new b(this, 1), new b(this, 2), new b(this, 3), new b(this, 4), new b(this, 5), new b(this, 6), new b(this, 7), new b(this, 8)
   };

   public c() {
      this.setTitle("AWT Ultimate Tick Tack Toe");
      this.setDefaultCloseOperation(3);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4, Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4);
      this.setVisible(true);
      this.setLayout(null);
      this.setResizable(false);
      int var1 = this.getWidth() / 3;
      int var2 = this.getHeight() / 3;

      for (int var3 = 0; var3 < this.d.length; var3++) {
         this.d[var3].setBounds(var3 % 3 * var1, var3 / 3 * var2, var1 - 2, var2 - 2);
         this.add(this.d[var3]);
         this.d[var3].a();
      }

      this.setSize(this.getWidth() + 50, this.getHeight() + 50);
   }

   public final String a() {
      String[] var1 = new String[]{"X", "O"};
      String var2 = "none";

      for (int var3 = 0; var3 < 2; var3++) {
         if (this.d[0].c[0].getText() == var1[var3] && this.d[1].c[0].getText() == var1[var3] && this.d[2].c[0].getText() == var1[var3]
            || this.d[3].c[0].getText() == var1[var3] && this.d[4].c[0].getText() == var1[var3] && this.d[5].c[0].getText() == var1[var3]
            || this.d[6].c[0].getText() == var1[var3] && this.d[7].c[0].getText() == var1[var3] && this.d[8].c[0].getText() == var1[var3]
            || this.d[0].c[0].getText() == var1[var3] && this.d[4].c[0].getText() == var1[var3] && this.d[8].c[0].getText() == var1[var3]
            || this.d[2].c[0].getText() == var1[var3] && this.d[4].c[0].getText() == var1[var3] && this.d[6].c[0].getText() == var1[var3]
            || this.d[0].c[0].getText() == var1[var3] && this.d[3].c[0].getText() == var1[var3] && this.d[6].c[0].getText() == var1[var3]
            || this.d[1].c[0].getText() == var1[var3] && this.d[4].c[0].getText() == var1[var3] && this.d[7].c[0].getText() == var1[var3]
            || this.d[2].c[0].getText() == var1[var3] && this.d[5].c[0].getText() == var1[var3] && this.d[8].c[0].getText() == var1[var3]) {
            var2 = var1[var3];
         }
      }

      return var2;
   }
}
