package rubikscube;

import javax.swing.JButton;

public final class a {
   protected g a = null;
   protected c b = new c();
   private String j = "blue";
   private String k = "red";
   private String l = "yellow";
   private String m = "green";
   private String n = "orange";
   private String o = "white";
   protected b c = null;
   protected b d = null;
   protected b e = null;
   protected b f = null;
   protected b g = null;
   protected b h = null;
   protected String i = "";

   public a(g var1, JButton[][] var2, int var3, int var4) {
      this.a = var1;
      int[] var8 = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
      int[] var5 = new int[]{9, 8, 7, 6, 5, 4, 3, 2, 1};
      int[] var6 = new int[]{7, 4, 1, 8, 5, 2, 9, 6, 3};
      int[] var7 = new int[]{3, 6, 9, 2, 5, 8, 1, 4, 7};
      this.c = new b(this, this.j, var2[0], var5, var3, var4);
      this.d = new b(this, this.k, var2[1], var8, var3, var4);
      this.e = new b(this, this.l, var2[2], var6, var3, var4);
      this.f = new b(this, this.m, var2[3], var8, var3, var4);
      this.g = new b(this, this.n, var2[4], var8, var3, var4);
      this.h = new b(this, this.o, var2[5], var7, var3, var4);
   }

   public final void a() {
      this.c.a();
      this.d.a();
      this.e.a();
      this.f.a();
      this.g.a();
      this.h.a();
   }
}
