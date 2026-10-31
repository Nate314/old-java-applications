package clock;

import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Main extends JFrame {
   JPanel i = new a(this);
   public int a;
   public int b;
   public int c;
   public int d;
   public int e;
   public int f;
   public int g;
   public String h;
   String[] j = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

   private Main(Boolean var1) {
      this.setDefaultCloseOperation(3);
      this.setVisible(var1);
      this.setTitle("Clock");
      Timer var2;
      (var2 = new Timer(1000, new b(this))).setRepeats(true);
      var2.start();
      this.add(this.i);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().width / 4, Toolkit.getDefaultToolkit().getScreenSize().width / 4);
   }

   public static void main(String[] var0) {
      new Main(true);
   }
}
