package circlewindow;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class a extends JFrame implements ActionListener, KeyListener {
   private JPanel a = new JPanel();
   private JButton b = new JButton("Create a circle with that diameter");
   private JTextField c = new JTextField("100");

   public a() {
      this.setTitle("Circle Creator");
      this.setSize(250, 110);
      this.add(this.a);
      this.setVisible(true);
      this.addKeyListener(this);
      this.setDefaultCloseOperation(3);
      this.b.setVisible(true);
      this.b.addActionListener(this);
      this.c.setColumns(10);
      this.a.add(this.c);
      this.a.add(this.b);
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if (var1.getSource() == this.b) {
         int var8 = Integer.parseInt(this.c.getText());
         int var2 = Toolkit.getDefaultToolkit().getScreenSize().width;
         int var3 = Toolkit.getDefaultToolkit().getScreenSize().height;
         double[] var4 = new double[var8];
         double[] var5 = new double[var8];
         int var6 = var8 / 2;

         for (int var7 = 0; var7 < var8; var7++) {
            var4[var7] = (double)(2 * var7);
            var5[var7] = 2.0 * Math.sqrt(Math.pow((double)var6, 2.0) - Math.pow((double)var7, 2.0));
         }

         for (int var9 = 0; var9 < var8; var9++) {
            if (var9 < var8) {
               new b(
                  (int)((double)(var2 / 2) - var4[var9] / 2.0),
                  (int)((double)(var3 / 2) + ((double)(var8 / 2) - var5[var9]) / 2.0),
                  var4[var9],
                  var5[var9],
                  String.valueOf(var9),
                  var9 % 10 == 0
               );
            }
         }

         System.out.printf("%d, %d", this.getWidth(), this.getHeight());
      }
   }

   @Override
   public final void keyTyped(KeyEvent var1) {
   }

   @Override
   public final void keyPressed(KeyEvent var1) {
   }

   @Override
   public final void keyReleased(KeyEvent var1) {
   }
}
