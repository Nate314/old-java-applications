package flappyBird;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Random;

public final class f {
   public static final int a = Main.b;
   public static final int b = Main.a / 8;
   public static int c = 0;
   public Image d;
   public Image e;
   public ArrayList f = new ArrayList();
   private int h = 2 * b;
   private int i = (int)((double)Main.a / 3.5);
   private Random j = new Random();
   private ArrayList k = new ArrayList();
   public static float g = (float)(Main.a / 500);

   public f(Image var1, Image var2) {
      this.d = var1;
      this.e = var2;

      for (int var3 = 0; var3 < 1000; var3++) {
         int var4 = this.j.nextInt(a - this.h - (Main.b - flappyBird.a.a));
         this.f.add(new e((float)(Main.a + this.i * var3), (float)(var4 - a)));
         this.f.add(new e((float)(Main.a + this.i * var3), (float)(var4 + this.h)));
         if (var3 < 4) {
            this.k.add(var3);
         }
      }

      this.a();
   }

   public final void a() {
      int var1 = 0;

      for (int var2 = 0; var2 < this.f.size(); var2++) {
         this.f.set(var2, new e(((e)this.f.get(var2)).a - g, ((e)this.f.get(var2)).b));
         if (((e)this.f.get(var2)).c > 0.0F && var1 == 0) {
            while (++var1 < 4) {
               c = var2 + var1;
            }
         }
      }
   }
}
