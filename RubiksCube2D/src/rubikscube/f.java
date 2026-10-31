package rubikscube;

public final class f {
   private a a = null;
   private String[] b = new String[]{"", "", ""};
   private String c = "";
   private String d = "";
   private String e = "blue";
   private String f = "red";
   private String g = "yellow";
   private String h = "green";
   private String i = "orange";
   private String j = "white";

   public f(String var1, String var2, int var3, a var4) {
      this.a = var4;
      if (var1 == this.e) {
         if (var2 == "w") {
            this.a(var3);
         }

         if (var2 == "a") {
            this.f(var3);
         }

         if (var2 == "s") {
            this.c(var3);
         }

         if (var2 == "d") {
            this.e(var3);
            return;
         }
      } else if (var1 == this.f) {
         if (var2 == "w") {
            this.b(var3);
         }

         if (var2 == "a") {
            this.a(var3, true);
         }

         if (var2 == "s") {
            this.d(var3);
         }

         if (var2 == "d") {
            this.b(var3, true);
            return;
         }
      } else if (var1 == this.g) {
         if (var2 == "w") {
            this.f(var3);
         }

         if (var2 == "a") {
            this.c(var3, true);
         }

         if (var2 == "s") {
            this.e(var3);
         }

         if (var2 == "d") {
            this.d(var3, true);
            return;
         }
      } else if (var1 == this.h) {
         if (var2 == "w") {
            this.b(var3);
         }

         if (var2 == "a") {
            this.e(var3);
         }

         if (var2 == "s") {
            this.d(var3);
         }

         if (var2 == "d") {
            this.f(var3);
            return;
         }
      } else if (var1 == this.i) {
         if (var2 == "w") {
            this.b(var3);
         }

         if (var2 == "a") {
            this.a(var3, false);
         }

         if (var2 == "s") {
            this.d(var3);
         }

         if (var2 == "d") {
            this.b(var3, false);
            return;
         }
      } else if (var1 == this.j) {
         if (var2 == "w") {
            this.e(var3);
         }

         if (var2 == "a") {
            this.c(var3, false);
         }

         if (var2 == "s") {
            this.f(var3);
         }

         if (var2 == "d") {
            this.d(var3, false);
         }
      }
   }

   private void a(int var1) {
      int var4;
      int var5;
      int var6;
      int var2 = (
               var6 = (var5 = (var4 = var1 != 1 && var1 != 4 && var1 != 7 ? var1 : 1) != 2 && var4 != 5 && var4 != 8 ? var4 : 2) != 3 && var5 != 6 && var5 != 9
                  ? var5
                  : 3
            )
            == 1
         ? -2
         : (var6 == 2 ? 0 : (var6 == 3 ? 2 : var6));

      for (int var3 = 0; var3 < 3; var3++) {
         this.b[var3] = this.a.c.b[var6 + var3 * 3 - 1];
         this.a.c.b[var6 + var3 * 3 - 1] = this.a.d.b[var6 + var3 * 3 - var2 - 1];
         this.a.d.b[var6 + var3 * 3 - var2 - 1] = this.a.f.b[var6 + var3 * 3 - var2 - 1];
         this.a.f.b[var6 + var3 * 3 - var2 - 1] = this.a.g.b[var6 + var3 * 3 - var2 - 1];
         this.a.g.b[var6 + var3 * 3 - var2 - 1] = this.b[var3];
         this.a.a();
      }

      if (var6 == 1) {
         this.a(this.a.e);
      } else {
         if (var6 == 3) {
            this.b(this.a.h);
         }
      }
   }

   private void b(int var1) {
      int var3;
      int var10000 = (var3 = var1 != 1 && var1 != 4 && var1 != 7 ? var1 : 1) != 2 && var3 != 5 && var3 != 8 ? var3 : 2;
      var1 = var10000;
      var1 = var10000 != 3 && var1 != 6 && var1 != 9 ? var1 : 3;

      for (int var2 = 0; var2 < 3; var2++) {
         this.b[var2] = this.a.d.b[var1 + var2 * 3 - 1];
         this.a.d.b[var1 + var2 * 3 - 1] = this.a.f.b[var1 + var2 * 3 - 1];
         this.a.f.b[var1 + var2 * 3 - 1] = this.a.g.b[var1 + var2 * 3 - 1];
         this.a.g.b[var1 + var2 * 3 - 1] = this.a.c.b[-1 * var1 + 4 + var2 * 3 - 1];
         this.a.c.b[-1 * var1 + 4 + var2 * 3 - 1] = this.b[var2];
         this.a.a();
      }

      if (var1 == 1) {
         this.b(this.a.h);
      } else {
         if (var1 == 3) {
            this.a(this.a.e);
         }
      }
   }

   private void c(int var1) {
      int var4;
      int var5;
      int var6;
      int var2 = (
               var6 = (var5 = (var4 = var1 != 1 && var1 != 4 && var1 != 7 ? var1 : 1) != 2 && var4 != 5 && var4 != 8 ? var4 : 2) != 3 && var5 != 6 && var5 != 9
                  ? var5
                  : 3
            )
            == 1
         ? -2
         : (var6 == 2 ? 0 : (var6 == 3 ? 2 : var6));

      for (int var3 = 0; var3 < 3; var3++) {
         this.b[var3] = this.a.c.b[var6 + var3 * 3 - 1];
         this.a.c.b[var6 + var3 * 3 - 1] = this.a.g.b[var6 + var3 * 3 - var2 - 1];
         this.a.g.b[var6 + var3 * 3 - var2 - 1] = this.a.f.b[var6 + var3 * 3 - var2 - 1];
         this.a.f.b[var6 + var3 * 3 - var2 - 1] = this.a.d.b[var6 + var3 * 3 - var2 - 1];
         this.a.d.b[var6 + var3 * 3 - var2 - 1] = this.b[var3];
         this.a.a();
      }

      if (var6 == 1) {
         this.b(this.a.e);
      } else {
         if (var6 == 3) {
            this.a(this.a.h);
         }
      }
   }

   private void d(int var1) {
      int var3;
      int var10000 = (var3 = var1 != 1 && var1 != 4 && var1 != 7 ? var1 : 1) != 2 && var3 != 5 && var3 != 8 ? var3 : 2;
      var1 = var10000;
      var1 = var10000 != 3 && var1 != 6 && var1 != 9 ? var1 : 3;

      for (int var2 = 0; var2 < 3; var2++) {
         this.b[var2] = this.a.d.b[var1 + var2 * 3 - 1];
         this.a.d.b[var1 + var2 * 3 - 1] = this.a.c.b[-1 * var1 + 4 + var2 * 3 - 1];
         this.a.c.b[-1 * var1 + 4 + var2 * 3 - 1] = this.a.g.b[var1 + var2 * 3 - 1];
         this.a.g.b[var1 + var2 * 3 - 1] = this.a.f.b[var1 + var2 * 3 - 1];
         this.a.f.b[var1 + var2 * 3 - 1] = this.b[var2];
         this.a.a();
      }

      if (var1 == 1) {
         this.a(this.a.h);
      } else {
         if (var1 == 3) {
            this.b(this.a.e);
         }
      }
   }

   private void a(int var1, boolean var2) {
      if (var2) {
         int var3;
         int var10000 = (var3 = var1 != 1 && var1 != 2 && var1 != 3 ? var1 : 0) != 4 && var3 != 5 && var3 != 6 ? var3 : 1;
         var1 = var10000;
         var1 = var10000 != 7 && var1 != 8 && var1 != 9 ? var1 : 2;

         for (int var9 = 0; var9 < 3; var9++) {
            this.b[var9] = this.a.h.b[var1 + var9 * 3];
            this.a.h.b[var1 + var9 * 3] = this.a.d.b[var1 * 3 + var9];
            this.a.d.b[var1 * 3 + var9] = this.a.e.b[-1 * var1 + (var9 + 1) * 3 - 1];
            this.a.e.b[-1 * var1 + (var9 + 1) * 3 - 1] = this.a.g.b[10 - (var1 + 1) * 3 + var9 - 1];
            this.a.g.b[10 - (var1 + 1) * 3 + var9 - 1] = this.b[var9];
            this.a.a();
         }

         if (var1 == 0) {
            this.a(this.a.c);
            return;
         }

         if (var1 == 2) {
            this.b(this.a.f);
            return;
         }
      } else {
         int var6;
         int var11 = (var6 = var1 != 1 && var1 != 2 && var1 != 3 ? var1 : 2) != 4 && var6 != 5 && var6 != 6 ? var6 : 1;
         var1 = var11;
         var1 = var11 != 7 && var1 != 8 && var1 != 9 ? var1 : 0;

         for (int var10 = 0; var10 < 3; var10++) {
            this.b[var10] = this.a.h.b[var1 + var10 * 3];
            this.a.h.b[var1 + var10 * 3] = this.a.g.b[10 - (var1 + 1) * 3 + var10 - 1];
            this.a.g.b[10 - (var1 + 1) * 3 + var10 - 1] = this.a.e.b[-1 * var1 + (var10 + 1) * 3 - 1];
            this.a.e.b[-1 * var1 + (var10 + 1) * 3 - 1] = this.a.d.b[var1 * 3 + var10];
            this.a.d.b[var1 * 3 + var10] = this.b[var10];
            this.a.a();
         }

         if (var1 == 0) {
            this.b(this.a.c);
            return;
         }

         if (var1 == 2) {
            this.a(this.a.f);
         }
      }
   }

   private void b(int var1, boolean var2) {
      if (var2) {
         int var3;
         int var10000 = (var3 = var1 != 1 && var1 != 2 && var1 != 3 ? var1 : 0) != 4 && var3 != 5 && var3 != 6 ? var3 : 1;
         var1 = var10000;
         var1 = var10000 != 7 && var1 != 8 && var1 != 9 ? var1 : 2;

         for (int var9 = 0; var9 < 3; var9++) {
            this.b[var9] = this.a.h.b[var1 + var9 * 3];
            this.a.h.b[var1 + var9 * 3] = this.a.g.b[10 - (var1 + 1) * 3 + var9 - 1];
            this.a.g.b[10 - (var1 + 1) * 3 + var9 - 1] = this.a.e.b[-1 * var1 + (var9 + 1) * 3 - 1];
            this.a.e.b[-1 * var1 + (var9 + 1) * 3 - 1] = this.a.d.b[var1 * 3 + var9];
            this.a.d.b[var1 * 3 + var9] = this.b[var9];
            this.a.a();
         }

         if (var1 == 0) {
            this.b(this.a.c);
            return;
         }

         if (var1 == 2) {
            this.a(this.a.f);
            return;
         }
      } else {
         int var6;
         int var11 = (var6 = var1 != 1 && var1 != 2 && var1 != 3 ? var1 : 2) != 4 && var6 != 5 && var6 != 6 ? var6 : 1;
         var1 = var11;
         var1 = var11 != 7 && var1 != 8 && var1 != 9 ? var1 : 0;

         for (int var10 = 0; var10 < 3; var10++) {
            this.b[var10] = this.a.h.b[var1 + var10 * 3];
            this.a.h.b[var1 + var10 * 3] = this.a.d.b[var1 * 3 + var10];
            this.a.d.b[var1 * 3 + var10] = this.a.e.b[-1 * var1 + (var10 + 1) * 3 - 1];
            this.a.e.b[-1 * var1 + (var10 + 1) * 3 - 1] = this.a.g.b[10 - (var1 + 1) * 3 + var10 - 1];
            this.a.g.b[10 - (var1 + 1) * 3 + var10 - 1] = this.b[var10];
            this.a.a();
         }

         if (var1 == 0) {
            this.a(this.a.c);
            return;
         }

         if (var1 == 2) {
            this.b(this.a.f);
         }
      }
   }

   private void e(int var1) {
      int var3;
      int var10000 = (var3 = var1 < 4 && var1 > 0 ? 0 : var1) < 7 && var3 > 3 ? 3 : var3;
      var1 = var10000;
      var1 = var10000 < 10 && var1 > 6 ? 6 : var1;

      for (int var2 = 0; var2 < 3; var2++) {
         this.b[var2] = this.a.c.b[var1 + var2];
         this.a.c.b[var1 + var2] = this.a.h.b[var1 + var2];
         this.a.h.b[var1 + var2] = this.a.f.b[var1 + var2];
         this.a.f.b[var1 + var2] = this.a.e.b[var1 + var2];
         this.a.e.b[var1 + var2] = this.b[var2];
         this.a.a();
      }

      if (var1 == 0) {
         this.a(this.a.d);
      } else {
         if (var1 == 6) {
            this.b(this.a.g);
         }
      }
   }

   private void f(int var1) {
      int var3;
      int var10000 = (var3 = var1 < 4 && var1 > 0 ? 0 : var1) < 7 && var3 > 3 ? 3 : var3;
      var1 = var10000;
      var1 = var10000 < 10 && var1 > 6 ? 6 : var1;

      for (int var2 = 0; var2 < 3; var2++) {
         this.b[var2] = this.a.h.b[var1 + var2];
         this.a.h.b[var1 + var2] = this.a.c.b[var1 + var2];
         this.a.c.b[var1 + var2] = this.a.e.b[var1 + var2];
         this.a.e.b[var1 + var2] = this.a.f.b[var1 + var2];
         this.a.f.b[var1 + var2] = this.b[var2];
         this.a.a();
      }

      if (var1 == 0) {
         this.b(this.a.d);
      } else {
         if (var1 == 6) {
            this.a(this.a.g);
         }
      }
   }

   private void c(int var1, boolean var2) {
      int var3;
      int var10000 = (var3 = var1 != 1 && var1 != 4 && var1 != 7 ? var1 : 0) != 2 && var3 != 5 && var3 != 8 ? var3 : 1;
      var1 = var10000;
      var1 = var10000 != 3 && var1 != 6 && var1 != 9 ? var1 : 2;
      if (var2) {
         var1 = var1 == 0 ? 2 : (var1 == 2 ? 0 : var1);
      }

      for (int var6 = 0; var6 < 3; var6++) {
         this.b[var6] = this.a.h.b[var1 + var6 * 3];
         this.a.h.b[var1 + var6 * 3] = this.a.d.b[var1 * 3 + var6];
         this.a.d.b[var1 * 3 + var6] = this.a.e.b[-1 * var1 + (var6 + 1) * 3 - 1];
         this.a.e.b[-1 * var1 + (var6 + 1) * 3 - 1] = this.a.g.b[10 - (var1 + 1) * 3 + var6 - 1];
         this.a.g.b[10 - (var1 + 1) * 3 + var6 - 1] = this.b[var6];
         this.a.a();
      }

      if (var1 == 0) {
         this.a(this.a.c);
      } else {
         if (var1 == 2) {
            this.b(this.a.f);
         }
      }
   }

   private void d(int var1, boolean var2) {
      int var3;
      int var10000 = (var3 = var1 != 1 && var1 != 4 && var1 != 7 ? var1 : 0) != 2 && var3 != 5 && var3 != 8 ? var3 : 1;
      var1 = var10000;
      var1 = var10000 != 3 && var1 != 6 && var1 != 9 ? var1 : 2;
      if (var2) {
         var1 = var1 == 0 ? 2 : (var1 == 2 ? 0 : var1);
      }

      for (int var6 = 0; var6 < 3; var6++) {
         this.b[var6] = this.a.h.b[var1 + var6 * 3];
         this.a.h.b[var1 + var6 * 3] = this.a.g.b[10 - (var1 + 1) * 3 + var6 - 1];
         this.a.g.b[10 - (var1 + 1) * 3 + var6 - 1] = this.a.e.b[-1 * var1 + (var6 + 1) * 3 - 1];
         this.a.e.b[-1 * var1 + (var6 + 1) * 3 - 1] = this.a.d.b[var1 * 3 + var6];
         this.a.d.b[var1 * 3 + var6] = this.b[var6];
         this.a.a();
      }

      if (var1 == 0) {
         this.a(this.a.c);
      } else {
         if (var1 == 2) {
            this.b(this.a.f);
         }
      }
   }

   private void a(b var1) {
      this.c = var1.b[0];
      this.d = var1.b[1];
      var1.b[0] = var1.b[6];
      var1.b[6] = var1.b[8];
      var1.b[8] = var1.b[2];
      var1.b[1] = var1.b[3];
      var1.b[3] = var1.b[7];
      var1.b[7] = var1.b[5];
      var1.b[2] = this.c;
      var1.b[5] = this.d;
      this.a.a();
   }

   private void b(b var1) {
      this.c = var1.b[0];
      this.d = var1.b[1];
      var1.b[0] = var1.b[2];
      var1.b[2] = var1.b[8];
      var1.b[8] = var1.b[6];
      var1.b[1] = var1.b[5];
      var1.b[5] = var1.b[7];
      var1.b[7] = var1.b[3];
      var1.b[6] = this.c;
      var1.b[3] = this.d;
      this.a.a();
   }
}
