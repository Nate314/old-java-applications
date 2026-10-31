package flappyBird;

import java.awt.Image;
import java.util.ArrayList;

public final class a {
   public static int a = Main.b * 7 / 8;
   public Image b;
   public Image c;
   public ArrayList d = new ArrayList();
   private float e = (float)(Main.a / 1000);

   public a(Image var1, Image var2) {
      this.b = var1;
      this.c = var2;
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();
      var3.add(new e(0.0F, 0.0F));
      var3.add(new e((float)Main.a, 0.0F));
      var4.add(new e(0.0F, (float)a));
      var4.add(new e((float)Main.a, (float)a));
      this.d.add(var3);
      this.d.add(var4);
      this.a();
   }

   public final void a() {
      for (int var1 = 0; var1 < ((ArrayList)this.d.get(0)).size(); var1++) {
         ((e)((ArrayList)this.d.get(0)).get(var1)).a(new e(((e)((ArrayList)this.d.get(0)).get(var1)).a - this.e, ((e)((ArrayList)this.d.get(0)).get(var1)).b));
         if (((e)((ArrayList)this.d.get(0)).get(var1)).a + (float)Main.a == 0.0F) {
            ((e)((ArrayList)this.d.get(0)).get(var1)).a(new e((float)Main.a, 0.0F));
         }
      }

      for (int var3 = 0; var3 < ((ArrayList)this.d.get(1)).size(); var3++) {
         ((e)((ArrayList)this.d.get(1)).get(var3)).a(new e(((e)((ArrayList)this.d.get(1)).get(var3)).a - f.g, ((e)((ArrayList)this.d.get(1)).get(var3)).b));
         if (((e)((ArrayList)this.d.get(1)).get(var3)).a + (float)Main.a == 0.0F) {
            ((e)((ArrayList)this.d.get(1)).get(var3)).a(new e((float)Main.a, (float)a));
         }
      }
   }
}
