package flappyBird;

import java.awt.Image;

public final class b {
   public static final int a;
   public static final int b = a = Main.b / 12;
   public Image c;
   public e d;
   private float e;
   private float f;
   private float g;

   public b(Image var1) {
      this.d = new e((float)(Main.a / 4), 0.0F);
      this.e = (float)(b / 10);
      this.f = 0.0F;
      this.g = 0.0F;
      this.c = var1;
      this.b();
   }

   public final void a() {
      this.f = this.e;
   }

   public final void b() {
      this.f = this.f - this.e / 35.0F;
      if (this.f < 0.0F) {
         this.g += 3.0F;
      } else {
         this.g -= 3.0F;
      }

      float var1 = this.f;
      this.d.a(new e(this.d.c, this.d.d - var1 < (float)(-b) ? (float)(-b) : (this.d.d - var1 > (float)Main.b ? (float)Main.b : this.d.d - var1)));
   }
}
