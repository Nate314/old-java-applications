package rubikscube;

import java.awt.Color;

public final class c {
   private Color a = new Color(0, 0, 255);
   private Color b = new Color(255, 0, 0);
   private Color c = new Color(255, 255, 0);
   private Color d = new Color(0, 255, 0);
   private Color e = new Color(255, 175, 0);
   private Color f = new Color(255, 255, 255);

   public final Color a(String var1) {
      String var2 = "blue";
      String var3 = "red";
      String var4 = "yellow";
      String var5 = "green";
      String var6 = "orange";
      String var7 = "white";
      return var1 == var2
         ? this.a
         : (
            var1 == var3
               ? this.b
               : (var1 == var3 ? this.b : (var1 == var4 ? this.c : (var1 == var5 ? this.d : (var1 == var6 ? this.e : (var1 == var7 ? this.f : null)))))
         );
   }
}
