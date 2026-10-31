package circlewindow;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;

public final class b extends JFrame {
   public b(int var1, int var2, double var3, double var5, String var7, boolean var8) {
      Toolkit.getDefaultToolkit().getScreenSize();
      JPanel var9 = new JPanel();
      this.setTitle(var7);
      this.setSize((int)var3, (int)var5);
      this.add(var9);
      this.setUndecorated(true);
      this.setVisible(true);
      this.setLocation(var1, var2);
      this.setAlwaysOnTop(var8);
      var9.setBackground(new Color(0, 0, 255));
   }
}
