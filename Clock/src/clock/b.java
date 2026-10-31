package clock;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

final class b implements ActionListener {
   private final Main a;

   b(Main var1) {
      this.a = var1;
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      Date var2 = new Date();
      this.a.a = var2.getMonth();
      this.a.b = var2.getDate();
      this.a.c = var2.getDay();
      this.a.d = var2.getYear();
      this.a.e = var2.getHours();
      this.a.f = var2.getMinutes();
      this.a.g = var2.getSeconds();
      this.a.h = String.format(
         "%s\n%s/%s/%s\n%s:%s:%s%s",
         this.a.j[this.a.c],
         String.valueOf(this.a.a + 1),
         String.valueOf(this.a.b),
         String.valueOf(this.a.d + 1900),
         this.a.e != 12 && this.a.e != 0 ? String.valueOf(this.a.e % 12) : "12",
         this.a.f < 10 ? "0" + this.a.f : this.a.f,
         this.a.g < 10 ? "0" + this.a.g : this.a.g,
         this.a.e < 12 ? "am" : "pm"
      );
      System.out.println(this.a.h);
      this.a.i.repaint();
   }
}
