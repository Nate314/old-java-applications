package flappyBird;

import java.awt.Toolkit;
import javax.swing.JFrame;

public class Main extends JFrame {
   public static final int a = Toolkit.getDefaultToolkit().getScreenSize().width * 3 / 4;
   public static final int b = Toolkit.getDefaultToolkit().getScreenSize().height * 3 / 4;
   private static int c;

   private Main(Boolean var1) {
      this.setVisible(var1);
      this.setTitle("Flappy Bird");
      this.setDefaultCloseOperation(3);
      int var3 = Toolkit.getDefaultToolkit().getScreenSize().width;
      int var2 = Toolkit.getDefaultToolkit().getScreenSize().height;
      this.setSize(a, b);
      this.setLocation((var3 - this.getWidth()) / 2, (var2 - this.getHeight()) / 2);
      this.setLayout(null);
      c var4;
      (var4 = new c(this, c)).setVisible(true);
      var4.setSize(this.getWidth(), this.getHeight());
      this.add(var4);
      this.addKeyListener(var4);
   }

   public static void main(String[] var0) {
      if (var0.length > 0) {
         c = Integer.parseInt(var0[0]);
      } else {
         c = 120;
      }

      System.out.println(c);
      new Main(true);
   }
}
