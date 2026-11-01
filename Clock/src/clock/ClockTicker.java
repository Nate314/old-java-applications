package clock;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;

final class ClockTicker implements ActionListener {
   private final Main clock;

   ClockTicker(Main clock) {
      this.clock = clock;
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      Calendar now = Calendar.getInstance();
      this.clock.month = now.get(Calendar.MONTH);
      this.clock.dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
      this.clock.dayOfWeek = now.get(Calendar.DAY_OF_WEEK) - 1;
      this.clock.year = now.get(Calendar.YEAR);
      this.clock.hours = now.get(Calendar.HOUR_OF_DAY);
      this.clock.minutes = now.get(Calendar.MINUTE);
      this.clock.seconds = now.get(Calendar.SECOND);
      this.clock.milliseconds = now.get(Calendar.MILLISECOND);
      String hourText = this.clock.twentyFourHour
         ? twoDigits(this.clock.hours)
         : (this.clock.hours != 12 && this.clock.hours != 0 ? String.valueOf(this.clock.hours % 12) : "12");
      String text = this.clock.dayNames[this.clock.dayOfWeek] + "\n"
         + (this.clock.month + 1) + "/" + this.clock.dayOfMonth + "/" + this.clock.year + "\n"
         + hourText + ":" + twoDigits(this.clock.minutes) + ":" + twoDigits(this.clock.seconds)
         + (this.clock.twentyFourHour ? "" : this.clock.hours < 12 ? "am" : "pm");
      if (!text.equals(this.clock.timeText)) {
         System.out.println(text);
      }
      this.clock.timeText = text;
      this.clock.clockPanel.repaint();
   }

   private static String twoDigits(int value) {
      return value < 10 ? "0" + value : String.valueOf(value);
   }
}
