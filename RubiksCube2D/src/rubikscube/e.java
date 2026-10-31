package rubikscube;

import java.awt.KeyEventDispatcher;
import java.awt.event.KeyEvent;

public final class e implements KeyEventDispatcher {
   private String a = "";
   private g b = null;

   public e(g var1) {
      this.b = var1;
   }

   @Override
   public final boolean dispatchKeyEvent(KeyEvent var1) {
      if (var1.getID() == 401) {
         int var2;
         if ((var2 = var1.getKeyCode()) == 87) {
            System.out.println("w");
            this.a("w");
         } else if (var2 == 65) {
            System.out.println("a");
            this.a("a");
         } else if (var2 == 83) {
            System.out.println("s");
            this.a("s");
         } else if (var2 == 68) {
            System.out.println("d");
            this.a("d");
         }
      }

      return false;
   }

   private void a(String var1) {
      this.a = this.b.b.i;
      new f(this.a, var1, this.b.a, this.b.b);
      this.b.b.a();
   }
}
