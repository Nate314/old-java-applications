package clock;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

final class ClockTicker implements ActionListener {
   private final Main clock;

   ClockTicker(Main clock) {
      this.clock = clock;
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      Date now = new Date();
      this.clock.month = now.getMonth();
      this.clock.dayOfMonth = now.getDate();
      this.clock.dayOfWeek = now.getDay();
      this.clock.year = now.getYear();
      this.clock.hours = now.getHours();
      this.clock.minutes = now.getMinutes();
      this.clock.seconds = now.getSeconds();
      this.clock.timeText = String.format(
         "%s\n%s/%s/%s\n%s:%s:%s%s",
         this.clock.dayNames[this.clock.dayOfWeek],
         String.valueOf(this.clock.month + 1),
         String.valueOf(this.clock.dayOfMonth),
         String.valueOf(this.clock.year + 1900),
         this.clock.hours != 12 && this.clock.hours != 0 ? String.valueOf(this.clock.hours % 12) : "12",
         this.clock.minutes < 10 ? "0" + this.clock.minutes : this.clock.minutes,
         this.clock.seconds < 10 ? "0" + this.clock.seconds : this.clock.seconds,
         this.clock.hours < 12 ? "am" : "pm"
      );
      System.out.println(this.clock.timeText);
      this.clock.clockPanel.repaint();
   }
}
