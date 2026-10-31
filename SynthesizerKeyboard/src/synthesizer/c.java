package synthesizer;

public final class c {
   public static int a(String var0) {
      int var1 = 0;
      String[] var2 = new String[]{"A", "As", "B", "C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs"};

      for (int var3 = -30; var3 < 31; var3++) {
         for (int var4 = 0; var4 < 12; var4++) {
            String var5 = var2[var4] + (var3 < 0 ? "n" + Math.abs(var3) : String.valueOf(var3));
            if (var0 == var5) {
               var1 = 12 + var3 * 12 + var4 - 3;
            }
         }
      }

      return var1;
   }
}
