package rubikscube;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class g extends JFrame implements ActionListener {
   private String[] c = new String[]{"", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
   private JButton[][] d = new JButton[][]{
      {
            new JButton(this.c[1]),
            new JButton(this.c[2]),
            new JButton(this.c[3]),
            new JButton(this.c[4]),
            new JButton(this.c[5]),
            new JButton(this.c[6]),
            new JButton(this.c[7]),
            new JButton(this.c[8]),
            new JButton(this.c[9])
      },
      {
            new JButton(this.c[1]),
            new JButton(this.c[2]),
            new JButton(this.c[3]),
            new JButton(this.c[4]),
            new JButton(this.c[5]),
            new JButton(this.c[6]),
            new JButton(this.c[7]),
            new JButton(this.c[8]),
            new JButton(this.c[9])
      },
      {
            new JButton(this.c[1]),
            new JButton(this.c[2]),
            new JButton(this.c[3]),
            new JButton(this.c[4]),
            new JButton(this.c[5]),
            new JButton(this.c[6]),
            new JButton(this.c[7]),
            new JButton(this.c[8]),
            new JButton(this.c[9])
      },
      {
            new JButton(this.c[1]),
            new JButton(this.c[2]),
            new JButton(this.c[3]),
            new JButton(this.c[4]),
            new JButton(this.c[5]),
            new JButton(this.c[6]),
            new JButton(this.c[7]),
            new JButton(this.c[8]),
            new JButton(this.c[9])
      },
      {
            new JButton(this.c[1]),
            new JButton(this.c[2]),
            new JButton(this.c[3]),
            new JButton(this.c[4]),
            new JButton(this.c[5]),
            new JButton(this.c[6]),
            new JButton(this.c[7]),
            new JButton(this.c[8]),
            new JButton(this.c[9])
      },
      {
            new JButton(this.c[1]),
            new JButton(this.c[2]),
            new JButton(this.c[3]),
            new JButton(this.c[4]),
            new JButton(this.c[5]),
            new JButton(this.c[6]),
            new JButton(this.c[7]),
            new JButton(this.c[8]),
            new JButton(this.c[9])
      }
   };
   private JButton[] e = new JButton[]{new JButton("w"), new JButton("a"), new JButton("s"), new JButton("d")};
   private JButton f = new JButton("Mix");
   private JLabel[] g = new JLabel[]{new JLabel("Moves:"), new JLabel("Secs:")};
   private JTextField[] h = new JTextField[]{new JTextField("1000"), new JTextField("0.5")};
   public int a = 0;
   private JPanel i = new JPanel();
   private int j = 0;
   private int k = 0;
   private int l = 0;
   private int m = 0;
   protected a b = null;

   public g() {
      this.setTitle("Rubik's Cube");
      this.setSize(550, 700);
      this.j = this.getWidth();
      this.k = this.getHeight();
      this.l = this.j / 11;
      this.m = this.k / 14;
      this.b = new a(this, this.d, this.l, this.m);
      this.setLocationRelativeTo(null);
      this.setDefaultCloseOperation(3);
      this.setResizable(true);
      this.add(this.i);
      this.i.setLayout(null);
      this.i.setBackground(new Color(255, 255, 255));
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new e(this));
      this.e[0].addActionListener(this);
      this.e[1].addActionListener(this);
      this.e[2].addActionListener(this);
      this.e[3].addActionListener(this);
      this.e[0]
         .setBounds(
            this.l + (int)(2.75 * (double)this.l / 3.0), (int)(1.75 * (double)this.m), (int)(2.75 * (double)this.l / 3.0), (int)(2.75 * (double)this.m / 3.0)
         );
      this.e[1].setBounds(this.e[0].getX() - this.e[0].getWidth(), this.e[0].getY() + this.e[0].getHeight(), this.e[0].getWidth(), this.e[0].getHeight());
      this.e[2].setBounds(this.e[1].getX() + this.e[1].getWidth(), this.e[1].getY(), this.e[0].getWidth(), this.e[0].getHeight());
      this.e[3].setBounds(this.e[2].getX() + this.e[2].getWidth(), this.e[1].getY(), this.e[0].getWidth(), this.e[0].getHeight());
      this.i.add(this.e[0]);
      this.i.add(this.e[1]);
      this.i.add(this.e[2]);
      this.i.add(this.e[3]);
      this.g[0].setBounds((int)(7.5 * (double)this.l), this.m, (int)(1.25 * (double)this.l), this.m / 2);
      this.g[1].setBounds((int)(8.75 * (double)this.l), this.m, (int)(1.25 * (double)this.l), this.m / 2);
      this.i.add(this.g[0]);
      this.i.add(this.g[1]);
      this.h[0].setBounds((int)(7.5 * (double)this.l), (int)(1.5 * (double)this.m), (int)(1.25 * (double)this.l), this.m / 2);
      this.h[1].setBounds((int)(8.75 * (double)this.l), (int)(1.5 * (double)this.m), (int)(1.25 * (double)this.l), this.m / 2);
      this.i.add(this.h[0]);
      this.i.add(this.h[1]);
      this.f.addActionListener(this);
      this.f.setBounds((int)(7.5 * (double)this.l), 2 * this.m, (int)(2.5 * (double)this.l), (int)(1.5 * (double)this.m));
      this.i.add(this.f);
      this.b.c.a.setBounds(4 * this.l, this.m, 3 * this.l, 3 * this.m);
      this.b.d.a.setBounds(4 * this.l, 4 * this.m, 3 * this.l, 3 * this.m);
      this.b.e.a.setBounds(7 * this.l, 4 * this.m, 3 * this.l, 3 * this.m);
      this.b.f.a.setBounds(4 * this.l, 7 * this.m, 3 * this.l, 3 * this.m);
      this.b.g.a.setBounds(4 * this.l, 10 * this.m, 3 * this.l, 3 * this.m);
      this.b.h.a.setBounds(this.l, 4 * this.m, 3 * this.l, 3 * this.m);
      this.b.c.a.setBackground(new Color(0, 0, 0));
      this.b.d.a.setBackground(new Color(0, 0, 0));
      this.b.e.a.setBackground(new Color(0, 0, 0));
      this.b.f.a.setBackground(new Color(0, 0, 0));
      this.b.g.a.setBackground(new Color(0, 0, 0));
      this.b.h.a.setBackground(new Color(0, 0, 0));
      this.i.add(this.b.c.a);
      this.i.add(this.b.d.a);
      this.i.add(this.b.e.a);
      this.i.add(this.b.f.a);
      this.i.add(this.b.g.a);
      this.i.add(this.b.h.a);
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if (var1.getSource() == this.e[0]) {
         try {
            Robot var2;
            (var2 = new Robot()).keyPress(87);
            var2.keyRelease(87);
         } catch (AWTException var6) {
            var6.printStackTrace();
         }
      }

      if (var1.getSource() == this.e[1]) {
         try {
            Robot var7;
            (var7 = new Robot()).keyPress(65);
            var7.keyRelease(65);
         } catch (AWTException var5) {
            var5.printStackTrace();
         }
      }

      if (var1.getSource() == this.e[2]) {
         try {
            Robot var8;
            (var8 = new Robot()).keyPress(83);
            var8.keyRelease(83);
         } catch (AWTException var4) {
            var4.printStackTrace();
         }
      }

      if (var1.getSource() == this.e[3]) {
         try {
            Robot var9;
            (var9 = new Robot()).keyPress(68);
            var9.keyRelease(68);
         } catch (AWTException var3) {
            var3.printStackTrace();
         }
      }

      if (var1.getSource() == this.f) {
         new d(Integer.parseInt(this.h[0].getText()), Double.parseDouble(this.h[1].getText()), this.b);
      }
   }
}
