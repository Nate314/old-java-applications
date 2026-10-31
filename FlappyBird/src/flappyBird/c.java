package flappyBird;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public final class c extends JPanel implements KeyListener {
   private boolean a = false;
   boolean b = false;
   private boolean c = false;
   private int d = 25;
   private int e;
   private float f;
   private float g;
   private float h;
   private ArrayList i = new ArrayList();
   private AffineTransform j = new AffineTransform();
   private AffineTransform k = new AffineTransform();
   private a l;
   private f m;
   private b n;

   public c(Main var1, int var2) {
      this.e = var2;
      this.i.add(new ImageIcon("bg.jpg").getImage().getScaledInstance(Main.a, Main.b, 4));
      this.i.add(new ImageIcon("ground.jpg").getImage().getScaledInstance(Main.a, Main.b / 8, 4));
      this.i.add(new ImageIcon("topPipe.png").getImage().getScaledInstance(flappyBird.f.b, flappyBird.f.a, 4));
      this.i.add(new ImageIcon("bottomPipe.png").getImage().getScaledInstance(flappyBird.f.b, flappyBird.f.a, 4));
      this.i.add(new ImageIcon("bird.png").getImage().getScaledInstance(flappyBird.b.a, flappyBird.b.b, 4));
      this.l = new a((Image)this.i.get(0), (Image)this.i.get(1));
      this.m = new f((Image)this.i.get(2), (Image)this.i.get(3));
      this.n = new b((Image)this.i.get(4));
      this.k.setTransform(this.j);
      new Thread(new d(this)).start();
   }

   private boolean a() {
      for (int var1 = flappyBird.f.c - 3; var1 < flappyBird.f.c; var1++) {
         if (var1 % 2 == 0) {
            int var2;
            int var3 = (var2 = (int)((e)this.m.f.get(var1)).c) + flappyBird.f.b;
            int var4 = (int)((e)this.m.f.get(var1)).d + flappyBird.f.a;
            int var5;
            int var6 = (var5 = (int)this.n.d.c) + flappyBird.b.a;
            int var7 = (int)this.n.d.d;
            if (var2 < var6 && var6 < var3 && var4 > var7 || var2 < var5 && var5 < var3 && var4 > var7) {
               return true;
            }
         } else {
            int var8;
            int var9 = (var8 = (int)((e)this.m.f.get(var1)).c) + flappyBird.f.b;
            int var10 = (int)((e)this.m.f.get(var1)).d;
            int var11;
            int var12 = (var11 = (int)this.n.d.c) + flappyBird.b.a;
            int var13;
            var13 = (var13 = (int)this.n.d.d) + flappyBird.b.b;
            if (var8 < var12 && var12 < var9 && var10 < var13 || var8 < var11 && var11 < var9 && var10 < var13) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   public final void paint(Graphics var1) {
      if (this.b) {
         this.l.a();
         this.n.b();
         this.m.a();
      }

      Graphics2D var5;
      (var5 = (Graphics2D)var1)
         .setColor(
            new Color(
               (int)(127.0 * (Math.cos((double)(this.f += 0.02F)) + 1.0)),
               (int)(127.0 * (Math.cos((double)(this.g += 0.02F)) + 1.0)),
               (int)(127.0 * (Math.sin((double)(this.h += 0.02F)) + 1.0))
            )
         );
      var5.setStroke(new BasicStroke((float)(Main.a / 250)));

      for (int var2 = 0; var2 < ((ArrayList)this.l.d.get(0)).size(); var2++) {
         var5.drawImage(this.l.b, (int)((e)((ArrayList)this.l.d.get(0)).get(var2)).a, (int)((e)((ArrayList)this.l.d.get(0)).get(var2)).b, null);
         if (this.a) {
            var5.drawRect(
               (int)((e)((ArrayList)this.l.d.get(0)).get(var2)).a,
               (int)((e)((ArrayList)this.l.d.get(0)).get(var2)).b,
               this.l.b.getWidth(null),
               this.l.b.getHeight(null)
            );
         }
      }

      int var6 = flappyBird.f.c < 2 ? flappyBird.f.c - 1 : (flappyBird.f.c < 4 ? flappyBird.f.c - 3 : flappyBird.f.c - 5);
      int var3 = flappyBird.f.c + 5;

      for (int var4 = var6; var4 < var3; var4++) {
         if (var4 % 2 == 0) {
            var5.drawImage(this.m.d, (int)((e)this.m.f.get(var4)).a, (int)((e)this.m.f.get(var4)).b, null);
            if (this.a) {
               var5.drawRect((int)((e)this.m.f.get(var4)).a, (int)((e)this.m.f.get(var4)).b, this.m.d.getWidth(null), this.m.d.getHeight(null));
            }
         } else {
            var5.drawImage(this.m.e, (int)((e)this.m.f.get(var4)).a, (int)((e)this.m.f.get(var4)).b, null);
            if (this.a) {
               var5.drawRect((int)((e)this.m.f.get(var4)).a, (int)((e)this.m.f.get(var4)).b, this.m.e.getWidth(null), this.m.e.getHeight(null));
            }
         }
      }

      for (int var9 = 0; var9 < ((ArrayList)this.l.d.get(1)).size(); var9++) {
         var5.drawImage(this.l.c, (int)((e)((ArrayList)this.l.d.get(1)).get(var9)).a, (int)((e)((ArrayList)this.l.d.get(1)).get(var9)).b, null);
         if (this.a) {
            var5.drawRect(0, Main.b * 7 / 8, Main.a, Main.b / 8);
         }
      }

      var5.drawImage(this.n.c, (int)this.n.d.c, (int)this.n.d.d, null);
      if (this.a) {
         var5.drawRect((int)this.n.d.c, (int)this.n.d.d, this.n.c.getWidth(null), this.n.c.getHeight(null));
      }

      if (!this.a() && !(this.n.d.d + (float)flappyBird.b.b > (float)flappyBird.a.a)) {
         this.c = false;
      } else {
         this.c = true;
      }

      if (this.c) {
         if (this.a) {
            var5.setColor(new Color(255, 0, 0));
            var5.setFont(new Font("Times New Roman", 1, Main.b / 10));
            var5.drawString("COLISION", Main.a / 2, (Main.b << 1) / 10);
         } else {
            this.b();
         }
      }

      var5.setColor(new Color(0, 0, 255));
      var5.setFont(new Font("Times New Roman", 1, Main.b / 10));
      int var10 = (flappyBird.f.c - 1) / 2 - 1;
      var5.drawString(String.valueOf(var10), Main.a / 2, Main.b / 10);
      if (this.a) {
         var5.setColor(new Color(0, 0, 0));
         var5.setFont(new Font("Times New Roman", 1, this.d));
         String[] var7 = new String[]{
            "bird x: " + this.n.d.c,
            "bird y: " + this.n.d.d,
            "current pipe x: " + ((e)this.m.f.get(flappyBird.f.c)).c,
            "current pipe y: " + ((e)this.m.f.get(flappyBird.f.c)).d,
            "paused: " + !this.b,
            "colision: " + this.c,
            "consoleFontSize: " + this.d,
            "rendered pipes: " + var6 + " - " + var3,
            ""
         };

         for (int var8 = 0; var8 < 9; var8++) {
            var5.drawString(var7[var8], Main.a - 11 * this.d, Main.b - this.d * 10 + this.d * var8);
         }
      }
   }

   private void b() {
      this.l = new a((Image)this.i.get(0), (Image)this.i.get(1));
      this.m = new f((Image)this.i.get(2), (Image)this.i.get(3));
      this.n = new b((Image)this.i.get(4));
   }

   @Override
   public final void keyTyped(KeyEvent var1) {
   }

   @Override
   public final void keyPressed(KeyEvent var1) {
      switch (var1.getKeyCode()) {
         case 32:
            this.n.a();
            return;
         case 38:
            this.d += 5;
            return;
         case 40:
            this.d -= 5;
            return;
         case 68:
            this.a = !this.a;
            return;
         case 80:
            this.b = !this.b;
            return;
         case 82:
            this.b();
            return;
         case 84:
            c var4 = this;
            Image var2 = (Image)this.i.get(0);

            for (int var3 = 0; var3 < var4.i.size() - 1; var3++) {
               var4.i
                  .set(
                     var3,
                     ((Image)var4.i.get(var3 + 1)).getScaledInstance(((Image)var4.i.get(var3)).getWidth(null), ((Image)var4.i.get(var3)).getHeight(null), 4)
                  );
            }

            var4.i
               .set(
                  var4.i.size() - 1,
                  var2.getScaledInstance(((Image)var4.i.get(var4.i.size() - 1)).getWidth(null), ((Image)var4.i.get(var4.i.size() - 1)).getHeight(null), 4)
               );
            System.out.println("test");
      }
   }

   @Override
   public final void keyReleased(KeyEvent var1) {
   }
}
