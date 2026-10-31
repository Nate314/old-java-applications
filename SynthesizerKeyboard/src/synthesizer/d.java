package synthesizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class d extends JFrame implements ActionListener, KeyListener {
   private b b = new b(this);
   private c c = new c();
   private JLayeredPane d = new JLayeredPane();
   private JPanel e = new JPanel();
   private JPanel f = new JPanel();
   private JButton[] g = new a[8];
   private JButton[] h = new a[5];
   private int[] i = new int[]{60, 62, 64, 65, 67, 69, 71, 72};
   private int[] j = new int[]{61, 63, 66, 68, 70};
   private int k = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
   private int l = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
   private int m;
   private boolean n = true;
   private Color o = new Color(255, 255, 255);
   private Color p = new Color(0, 0, 0);
   private Color q = new Color(175, 175, 175);
   private Color r = new Color(50, 50, 50);
   private int s = 0;
   private int t = 0;
   public long a = 0L;

   public d() {
      new HashMap();
      this.k = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
      this.l = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
      this.setIconImage(Toolkit.getDefaultToolkit().getImage("icon.jpg"));
      this.setTitle("AWT Synthesizer");
      this.setSize(this.k * 3 / 4, this.l / 2);
      this.setDefaultCloseOperation(3);
      this.setVisible(true);
      this.addKeyListener(this);
      this.add(this.d);
      Timer var1;
      (var1 = new Timer(1, new e(this))).setRepeats(true);
      var1.start();
      int var2 = synthesizer.c.a("C5");
      System.out.println(var2);
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      Object[] var2 = new Object[2];

      for (int var3 = 0; var3 < this.g.length; var3++) {
         if (var1.getSource() == this.g[var3]) {
            var2[0] = "whiteKeys";
            var2[1] = var3;
         }
      }

      for (int var4 = 0; var4 < this.h.length; var4++) {
         if (var1.getSource() == this.h[var4]) {
            var2[0] = "blackKeys";
            var2[1] = var4;
         }
      }

      if (var2[0] == "whiteKeys") {
         this.b.a(this.i[(Integer)var2[1]], 150, 10, 0, 0, 0);
      } else {
         if (var2[0] == "blackKeys") {
            this.b.a(this.j[(Integer)var2[1]], 150, 10, 0, 0, 0);
         }
      }
   }

   @Override
   public final void paint(Graphics var1) {
      d var3 = this;
      if (!this.n) {
         for (int var2 = 0; var2 < var3.g.length; var2++) {
            var3.g[var2].setVisible(false);
            var3.g[var2].disable();
         }

         for (int var4 = 0; var4 < var3.h.length; var4++) {
            var3.h[var4].setVisible(false);
            var3.h[var4].disable();
         }
      }

      var3.m = var3.getWidth() / 8;
      var3.e.setLayout(null);
      var3.f.setLayout(null);
      var3.e.setBounds(0, 0, var3.getWidth(), var3.getHeight());
      var3.f.setBounds(0, 0, var3.getWidth(), var3.getHeight() * 3 / 5);
      var3.f.setBackground(new Color(0, 0, 0, 0));

      for (int var5 = 0; var5 < var3.g.length; var5++) {
         var3.g[var5] = new a();
         var3.g[var5].addActionListener(var3);
         var3.g[var5].setBounds(var5 * var3.m, 0, var3.m, var3.e.getHeight());
         var3.g[var5].setBackground(new Color(255, 255, 255));
         var3.e.add(var3.g[var5]);
      }

      for (int var6 = 0; var6 < var3.h.length; var6++) {
         var3.h[var6] = new a();
         var3.h[var6].addActionListener(var3);
         if (var6 != 0 && var6 != 1) {
            var3.h[var6].setBounds(var3.m + var3.m / 2 + var6 * var3.m + var3.m / 8, 0, var3.m - var3.m / 4, var3.f.getHeight());
         } else {
            var3.h[var6].setBounds(var3.m / 2 + var6 * var3.m + var3.m / 8, 0, var3.m - var3.m / 4, var3.f.getHeight());
         }

         var3.h[var6].setBackground(new Color(0, 0, 0));
         var3.f.add(var3.h[var6]);
      }

      var3.d.add(var3.e, Integer.valueOf(0), 0);
      var3.d.add(var3.f, Integer.valueOf(1), 0);
      var3.n = false;
   }

   @Override
   public final void keyTyped(KeyEvent var1) {
   }

   @Override
   public final void keyPressed(KeyEvent var1) {
      if (var1.getKeyCode() == 37) {
         this.s--;
      } else if (var1.getKeyCode() == 39) {
         this.s++;
      } else if (var1.getKeyCode() == 38) {
         if (this.t < 120) {
            this.t++;
         }

         this.b.a(0, this.t);
      } else if (var1.getKeyCode() == 40) {
         if (this.t > 0) {
            this.t--;
         }

         this.b.a(0, this.t);
      }

      if (var1.getKeyCode() == 90) {
         this.b.b(0, 60 + 12 * this.s);
         this.g[0].setBackground(this.q);
      }

      if (var1.getKeyCode() == 83) {
         this.b.b(0, 61 + 12 * this.s);
         this.h[0].setBackground(this.r);
      }

      if (var1.getKeyCode() == 88) {
         this.b.b(0, 62 + 12 * this.s);
         this.g[1].setBackground(this.q);
      }

      if (var1.getKeyCode() == 68) {
         this.b.b(0, 63 + 12 * this.s);
         this.h[1].setBackground(this.r);
      }

      if (var1.getKeyCode() == 67) {
         this.b.b(0, 64 + 12 * this.s);
         this.g[2].setBackground(this.q);
      }

      if (var1.getKeyCode() == 86) {
         this.b.b(0, 65 + 12 * this.s);
         this.g[3].setBackground(this.q);
      }

      if (var1.getKeyCode() == 71) {
         this.b.b(0, 66 + 12 * this.s);
         this.h[2].setBackground(this.r);
      }

      if (var1.getKeyCode() == 66) {
         this.b.b(0, 67 + 12 * this.s);
         this.g[4].setBackground(this.q);
      }

      if (var1.getKeyCode() == 72) {
         this.b.b(0, 68 + 12 * this.s);
         this.h[3].setBackground(this.r);
      }

      if (var1.getKeyCode() == 78) {
         this.b.b(0, 69 + 12 * this.s);
         this.g[5].setBackground(this.q);
      }

      if (var1.getKeyCode() == 74) {
         this.b.b(0, 70 + 12 * this.s);
         this.h[4].setBackground(this.r);
      }

      if (var1.getKeyCode() == 77) {
         this.b.b(0, 71 + 12 * this.s);
         this.g[6].setBackground(this.q);
      }

      if (var1.getKeyCode() == 44) {
         this.b.b(0, 72 + 12 * this.s);
         this.g[7].setBackground(this.q);
      }
   }

   @Override
   public final void keyReleased(KeyEvent var1) {
      if (var1.getKeyCode() == 90) {
         this.b.c(0, 60 + 12 * this.s);
         this.g[0].setBackground(this.o);
      }

      if (var1.getKeyCode() == 83) {
         this.b.c(0, 61 + 12 * this.s);
         this.h[0].setBackground(this.p);
      }

      if (var1.getKeyCode() == 88) {
         this.b.c(0, 62 + 12 * this.s);
         this.g[1].setBackground(this.o);
      }

      if (var1.getKeyCode() == 68) {
         this.b.c(0, 63 + 12 * this.s);
         this.h[1].setBackground(this.p);
      }

      if (var1.getKeyCode() == 67) {
         this.b.c(0, 64 + 12 * this.s);
         this.g[2].setBackground(this.o);
      }

      if (var1.getKeyCode() == 86) {
         this.b.c(0, 65 + 12 * this.s);
         this.g[3].setBackground(this.o);
      }

      if (var1.getKeyCode() == 71) {
         this.b.c(0, 66 + 12 * this.s);
         this.h[2].setBackground(this.p);
      }

      if (var1.getKeyCode() == 66) {
         this.b.c(0, 67 + 12 * this.s);
         this.g[4].setBackground(this.o);
      }

      if (var1.getKeyCode() == 72) {
         this.b.c(0, 68 + 12 * this.s);
         this.h[3].setBackground(this.p);
      }

      if (var1.getKeyCode() == 78) {
         this.b.c(0, 69 + 12 * this.s);
         this.g[5].setBackground(this.o);
      }

      if (var1.getKeyCode() == 74) {
         this.b.c(0, 70 + 12 * this.s);
         this.h[4].setBackground(this.p);
      }

      if (var1.getKeyCode() == 77) {
         this.b.c(0, 71 + 12 * this.s);
         this.g[6].setBackground(this.o);
      }

      if (var1.getKeyCode() == 44) {
         this.b.c(0, 72 + 12 * this.s);
         this.g[7].setBackground(this.o);
      }
   }
}
