package dinosaurgameplayer;

import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

final class a implements ActionListener {
   private final DinosaurGamePlayer a;
   private final Robot b;
   private final int c;
   private final int d;

   a(DinosaurGamePlayer var1, Robot var2, int var3, int var4) {
      this.a = var1;
      this.b = var2;
      this.c = var3;
      this.d = var4;
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if (this.b.getPixelColor(this.c - 1, this.d).getBlue() == 83) {
         this.b.keyPress(32);
         DinosaurGamePlayer.a(65);
         this.b.keyRelease(32);
      }
   }
}
