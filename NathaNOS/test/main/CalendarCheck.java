package main;

/** Checks the calendar's date sums: day numbers both ways, weekdays, month grids, leap years and the footer lines. */
public final class CalendarCheck {
   public static void main(String[] args) {
      check(CalendarFlyout.days(1970, 0, 1) == 0, "1970-01-01 is day 0");
      check(CalendarFlyout.weekday(0) == 4, "1970-01-01 was a Thursday");
      check(CalendarFlyout.weekday(CalendarFlyout.days(2026, 10, 15)) == 0, "2026-11-15 is a Sunday");
      check(CalendarFlyout.weekday(CalendarFlyout.days(2000, 0, 1)) == 6, "2000-01-01 was a Saturday");
      check(CalendarFlyout.days(2024, 2, 1) - CalendarFlyout.days(2024, 1, 1) == 29, "February 2024 has 29 days");
      check(CalendarFlyout.days(2100, 2, 1) - CalendarFlyout.days(2100, 1, 1) == 28, "February 2100 has 28 days");
      check(CalendarFlyout.days(2000, 2, 1) - CalendarFlyout.days(2000, 1, 1) == 29, "February 2000 has 29 days");
      check(CalendarFlyout.days(2026, 12, 1) == CalendarFlyout.days(2027, 0, 1), "month 12 rolls into next January");
      check(CalendarFlyout.days(2026, -1, 1) == CalendarFlyout.days(2025, 11, 1), "month -1 rolls back to last December");
      for (int d = -800000; d <= 800000; d += 997) {
         int[] date = CalendarFlyout.date(d);
         check(CalendarFlyout.days(date[0], date[1], date[2]) == d, "day " + d + " reads back from its date", false);
      }
      System.out.println("ok - every sampled day from about 220 BC to 4160 reads back from its date");
      int first = CalendarFlyout.firstCell(2026, 10);
      check(CalendarFlyout.weekday(first) == 0, "the grid starts on a Sunday");
      check(first == CalendarFlyout.days(2026, 10, 1), "November 2026 starts its grid on the 1st (a Sunday)");
      check(CalendarFlyout.firstCell(2026, 11) == CalendarFlyout.days(2026, 10, 29), "December 2026's grid starts on November 29");
      int today = CalendarFlyout.days(2026, 10, 15);
      check(CalendarFlyout.distance(today, today).equals("Sunday, November 15: today"), "picking today says today");
      check(CalendarFlyout.distance(today + 1, today).endsWith(": tomorrow"), "tomorrow");
      check(CalendarFlyout.distance(today - 1, today).endsWith(": yesterday"), "yesterday");
      check(CalendarFlyout.distance(CalendarFlyout.days(2026, 11, 25), today).equals("Friday, December 25: in 40 days"), "Christmas is in 40 days");
      check(CalendarFlyout.distance(CalendarFlyout.days(2027, 0, 1), today).equals("Friday, January 1, 2027: in 47 days"), "another year names the year");
      check(CalendarFlyout.distance(today - 14, today).endsWith(": 14 days ago"), "days ago");
      check(CalendarFlyout.yearLine(today).equals("Day 319 of 365"), "November 15 is day 319 of 365");
      check(CalendarFlyout.yearLine(CalendarFlyout.days(2024, 11, 31)).equals("Day 366 of 366"), "a leap year has 366 days");
      int[] now = CalendarFlyout.today();
      check(now[1] >= 0 && now[1] < 12 && now[2] >= 1 && now[2] <= 31, "today is a real date");
      System.out.println("CalendarCheck: all checks passed");
   }

   private static void check(boolean ok, String what) {
      check(ok, what, true);
   }

   private static void check(boolean ok, String what, boolean say) {
      if (!ok) {
         throw new AssertionError(what);
      }
      if (say) {
         System.out.println("ok - " + what);
      }
   }
}
