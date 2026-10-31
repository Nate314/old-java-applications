package rubikscube;

import java.awt.AWTException;
import java.awt.Robot;

public final class d {
   private String[] a = new String[]{"blue", "red", "yellow", "green", "orange", "white"};
   private String[] b = new String[]{"w", "a", "s", "d"};

   d(int var1, double var2, a var4) {
      System.out.println("Mix");
      var2 = (double)((int)(var2 * 1000.0) / var1);
      int[] var5 = new int[var1];
      int[] var6 = new int[var1];
      int[] var7 = new int[var1];

      for (int var8 = 0; var8 < var1; var8++) {
         var5[var8] = (int)(Math.random() * 6.0);
         var6[var8] = (int)(Math.random() * 8.0);
         var7[var8] = (int)(Math.random() * 3.0);
      }

      String[] var21 = new String[var1];
      String[] var9 = new String[var1];

      for (int var10 = 0; var10 < var1; var10++) {
         var21[var10] = this.a[var5[var10]];
         var9[var10] = this.b[var7[var10]];
      }

      for (int var22 = 0; var22 < var1; var22++) {
         var4.a();
         System.out.printf("color: %s, cubie: %s, letter: %s\n", var21[var22], var6[var22], var9[var22]);
         var4.i = var21[var22];
         var4.a.a = var6[var22];
         if (var9[var22] == "w") {
            try {
               Robot var17;
               (var17 = new Robot()).keyPress(87);
               var17.keyRelease(87);
            } catch (AWTException var11) {
               var11.printStackTrace();
            }
         } else if (var9[var22] == "a") {
            try {
               Robot var18;
               (var18 = new Robot()).keyPress(65);
               var18.keyRelease(65);
            } catch (AWTException var15) {
               var15.printStackTrace();
            }
         } else if (var9[var22] == "s") {
            try {
               Robot var19;
               (var19 = new Robot()).keyPress(83);
               var19.keyRelease(83);
            } catch (AWTException var14) {
               var14.printStackTrace();
            }
         } else if (var9[var22] == "d") {
            try {
               Robot var20;
               (var20 = new Robot()).keyPress(68);
               var20.keyRelease(68);
            } catch (AWTException var13) {
               var13.printStackTrace();
            }
         }

         try {
            Thread.sleep((long)var2);
         } catch (InterruptedException var12) {
            var12.printStackTrace();
         }
      }
   }
}
