package dinosaurgameplayer;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Robot;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class DinosaurGamePlayer {
   private DinosaurGamePlayer(Robot var1) {
      int var2 = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().width / 2.44);
      int var3 = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().height / 4.5);
      int var4 = (int)((double)Toolkit.getDefaultToolkit().getScreenSize().height / 5.35);

      try {
         Thread.sleep(2500L);
      } catch (InterruptedException var7) {
         var7.printStackTrace();
      }

      JFrame var5;
      (var5 = new JFrame("Player1")).setDefaultCloseOperation(3);
      var5.setAlwaysOnTop(true);
      var5.setUndecorated(true);
      var5.setVisible(true);
      var5.setBounds(var2, var3, 10, 10);
      JPanel var6;
      (var6 = new JPanel()).setBackground(new Color(255, 0, 0));
      var5.add(var6);
      (var5 = new JFrame("Player2")).setDefaultCloseOperation(3);
      var5.setAlwaysOnTop(true);
      var5.setUndecorated(true);
      var5.setVisible(true);
      var5.setBounds(var2, var4, 10, 10);
      JPanel var9;
      (var9 = new JPanel()).setBackground(new Color(0, 255, 0));
      var5.add(var9);
      var1.keyPress(32);
      a(70);
      var1.keyRelease(32);
      Timer var8;
      (var8 = new Timer(1, new a(this, var1, var2, var3))).setRepeats(true);
      var8.start();
   }

   static void a(int var0) {
      try {
         Thread.sleep((long)var0);
      } catch (InterruptedException var1) {
         var1.printStackTrace();
      }
   }

   public static void main(String[] var0) {
      try {
         new DinosaurGamePlayer(new Robot());
      } catch (AWTException var1) {
         var1.printStackTrace();
      }
   }
}
