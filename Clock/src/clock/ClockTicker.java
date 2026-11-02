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
   @SuppressWarnings("deprecation")
   public final void actionPerformed(ActionEvent event) {
      // Date's getters read local time on the desktop and in the browser; TeaVM's Calendar is always UTC.
      long millis = System.currentTimeMillis();
      Date now = new Date(millis);
      this.clock.month = now.getMonth();
      this.clock.dayOfMonth = now.getDate();
      this.clock.dayOfWeek = now.getDay();
      this.clock.year = now.getYear() + 1900;
      this.clock.hours = now.getHours();
      this.clock.minutes = now.getMinutes();
      this.clock.seconds = now.getSeconds();
      this.clock.milliseconds = (int)(millis % 1000);
      boolean twentyFour = this.clock.state.twentyFour;
      String hourText = twentyFour
         ? twoDigits(this.clock.hours)
         : (this.clock.hours != 12 && this.clock.hours != 0 ? String.valueOf(this.clock.hours % 12) : "12");
      String text = this.clock.dayNames[this.clock.dayOfWeek] + "\n"
         + (this.clock.month + 1) + "/" + this.clock.dayOfMonth + "/" + this.clock.year + "\n"
         + hourText + ":" + twoDigits(this.clock.minutes) + ":" + twoDigits(this.clock.seconds)
         + (twentyFour ? "" : this.clock.hours < 12 ? "am" : "pm");
      if (!text.equals(this.clock.timeText)) {
         System.out.println(text);
      }
      this.clock.timeText = text;
      if (this.clock.state.countdown.update(millis)) {
         this.clock.timerFinished();
      }
      this.clock.refresh();
   }

   private static String twoDigits(int value) {
      return value < 10 ? "0" + value : String.valueOf(value);
   }
}
