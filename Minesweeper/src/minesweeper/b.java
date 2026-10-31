package minesweeper;

public final class b {
   private int a;
   private String[][] b;

   public b(int var1, int var2) {
      this.a = (int)((double)var2 * 1.25);
      this.b = new String[var1][var2];
      String[][] var3 = new String[var1][var2];
      int[] var4 = new int[this.a];
      int var5 = var1 - 1;
      int var6 = var2 - 1;

      for (int var7 = 0; var7 < this.a; var7++) {
         System.out.println(var1);
         var4[var7] = (int)(Math.random() * (double)(var1 * var2));
      }

      for (int var9 = 0; var9 < var1 * var2 - 1; var9++) {
         for (int var8 = 0; var8 < this.a; var8++) {
            if (var9 == var4[var8]) {
               var3[var9 / var1][var9 % var1] = "b";
            }
         }

         if (var3[var9 / var1][var9 % var1] != "b") {
            var3[var9 / var1][var9 % var1] = " ";
         }
      }

      for (int var10 = 0; var10 < var1; var10++) {
         for (int var11 = 0; var11 < var2; var11++) {
            if (var3[var10][var11] == "b") {
               var3[var10][var11] = "b";
               if (var10 == 0 && var11 == 0) {
                  var3[var10][var11 + 1] = var3[var10][var11 + 1] == " "
                     ? "1"
                     : (var3[var10][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 + 1]) + 1));
                  var3[var10 + 1][var11 + 1] = var3[var10 + 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 + 1]) + 1));
                  var3[var10 + 1][var11] = var3[var10 + 1][var11] == " "
                     ? "1"
                     : (var3[var10 + 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11]) + 1));
               } else if (var10 == 0 && var11 == var6) {
                  var3[var10 + 1][var11] = var3[var10 + 1][var11] == " "
                     ? "1"
                     : (var3[var10 + 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11]) + 1));
                  var3[var10 + 1][var11 - 1] = var3[var10 + 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 - 1]) + 1));
                  var3[var10][var11 - 1] = var3[var10][var11 - 1] == " "
                     ? "1"
                     : (var3[var10][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 - 1]) + 1));
               } else if (var10 == var5 && var11 == var6) {
                  var3[var10 - 1][var11] = var3[var10 - 1][var11] == " "
                     ? "1"
                     : (var3[var10 - 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11]) + 1));
                  var3[var10][var11 - 1] = var3[var10][var11 - 1] == " "
                     ? "1"
                     : (var3[var10][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 - 1]) + 1));
                  var3[var10 - 1][var11 - 1] = var3[var10 - 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 - 1]) + 1));
               } else if (var10 == var5 && var11 == 0) {
                  var3[var10 - 1][var11] = var3[var10 - 1][var11] == " "
                     ? "1"
                     : (var3[var10 - 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11]) + 1));
                  var3[var10 - 1][var11 + 1] = var3[var10 - 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 + 1]) + 1));
                  var3[var10][var11 + 1] = var3[var10][var11 + 1] == " "
                     ? "1"
                     : (var3[var10][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 + 1]) + 1));
               } else if (var10 == 0 && var11 != 0 && var11 != var6) {
                  var3[var10][var11 + 1] = var3[var10][var11 + 1] == " "
                     ? "1"
                     : (var3[var10][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 + 1]) + 1));
                  var3[var10 + 1][var11 + 1] = var3[var10 + 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 + 1]) + 1));
                  var3[var10 + 1][var11] = var3[var10 + 1][var11] == " "
                     ? "1"
                     : (var3[var10 + 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11]) + 1));
                  var3[var10 + 1][var11 - 1] = var3[var10 + 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 - 1]) + 1));
                  var3[var10][var11 - 1] = var3[var10][var11 - 1] == " "
                     ? "1"
                     : (var3[var10][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 - 1]) + 1));
               } else if (var11 == var6 && var10 != 0 && var10 != var5) {
                  var3[var10 - 1][var11] = var3[var10 - 1][var11] == " "
                     ? "1"
                     : (var3[var10 - 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11]) + 1));
                  var3[var10 + 1][var11] = var3[var10 + 1][var11] == " "
                     ? "1"
                     : (var3[var10 + 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11]) + 1));
                  var3[var10 + 1][var11 - 1] = var3[var10 + 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 - 1]) + 1));
                  var3[var10][var11 - 1] = var3[var10][var11 - 1] == " "
                     ? "1"
                     : (var3[var10][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 - 1]) + 1));
                  var3[var10 - 1][var11 - 1] = var3[var10 - 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 - 1]) + 1));
               } else if (var10 == var5 && var11 != 0 && var11 != var6) {
                  var3[var10 - 1][var11] = var3[var10 - 1][var11] == " "
                     ? "1"
                     : (var3[var10 - 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11]) + 1));
                  var3[var10 - 1][var11 + 1] = var3[var10 - 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 + 1]) + 1));
                  var3[var10][var11 + 1] = var3[var10][var11 + 1] == " "
                     ? "1"
                     : (var3[var10][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 + 1]) + 1));
                  var3[var10][var11 - 1] = var3[var10][var11 - 1] == " "
                     ? "1"
                     : (var3[var10][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 - 1]) + 1));
                  var3[var10 - 1][var11 - 1] = var3[var10 - 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 - 1]) + 1));
               } else if (var11 == 0 && var10 != 0 && var10 != var5) {
                  var3[var10 - 1][var11] = var3[var10 - 1][var11] == " "
                     ? "1"
                     : (var3[var10 - 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11]) + 1));
                  var3[var10 - 1][var11 + 1] = var3[var10 - 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 + 1]) + 1));
                  var3[var10][var11 + 1] = var3[var10][var11 + 1] == " "
                     ? "1"
                     : (var3[var10][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 + 1]) + 1));
                  var3[var10 + 1][var11 + 1] = var3[var10 + 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 + 1]) + 1));
                  var3[var10 + 1][var11] = var3[var10 + 1][var11] == " "
                     ? "1"
                     : (var3[var10 + 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11]) + 1));
               } else {
                  var3[var10 - 1][var11] = var3[var10 - 1][var11] == " "
                     ? "1"
                     : (var3[var10 - 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11]) + 1));
                  var3[var10 - 1][var11 + 1] = var3[var10 - 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 + 1]) + 1));
                  var3[var10][var11 + 1] = var3[var10][var11 + 1] == " "
                     ? "1"
                     : (var3[var10][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 + 1]) + 1));
                  var3[var10 + 1][var11 + 1] = var3[var10 + 1][var11 + 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 + 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 + 1]) + 1));
                  var3[var10 + 1][var11] = var3[var10 + 1][var11] == " "
                     ? "1"
                     : (var3[var10 + 1][var11] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11]) + 1));
                  var3[var10 + 1][var11 - 1] = var3[var10 + 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 + 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 + 1][var11 - 1]) + 1));
                  var3[var10][var11 - 1] = var3[var10][var11 - 1] == " "
                     ? "1"
                     : (var3[var10][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10][var11 - 1]) + 1));
                  var3[var10 - 1][var11 - 1] = var3[var10 - 1][var11 - 1] == " "
                     ? "1"
                     : (var3[var10 - 1][var11 - 1] == "b" ? "b" : "" + (Integer.parseInt(var3[var10 - 1][var11 - 1]) + 1));
               }
            } else if (var3[var10][var11] == " ") {
               var3[var10][var11] = " ";
            }
         }
      }

      this.b = var3;
   }

   public final String[][] a() {
      return this.b;
   }
}
