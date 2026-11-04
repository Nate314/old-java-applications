package clock;

import java.util.Date;

/**
 * A city's time zone: its standard offset from UTC and which daylight saving rule it follows, if any. The rules are
 * worked out here from the date alone, without {@code TimeZone}, so the desktop and the browser (where TeaVM knows
 * only UTC) agree. "Here" is the computer's own zone, read from {@code Date}.
 */
final class Zone {
   static final int NONE = 0;
   /** United States and Canada: second Sunday of March to first Sunday of November, at 2 am local time. */
   static final int US = 1;
   /** European Union and the UK: last Sunday of March to last Sunday of October, at 1 am UTC. */
   static final int EU = 2;
   /** South-east Australia: first Sunday of October to first Sunday of April, at 2 am standard time. */
   static final int AU = 3;
   /** New Zealand: last Sunday of September to first Sunday of April, at 2 am standard time. */
   static final int NZ = 4;
   private static final int HERE_RULE = -1;

   private static final long MINUTE = 60_000L;
   private static final long HOUR = 60 * MINUTE;
   static final long DAY = 24 * HOUR;

   /** The cities to pick from, west to east. */
   static final Zone[] CITIES = {
      new Zone("Honolulu", -600, NONE), new Zone("Anchorage", -540, US), new Zone("Los Angeles", -480, US),
      new Zone("Vancouver", -480, US), new Zone("Phoenix", -420, NONE), new Zone("Denver", -420, US),
      new Zone("Mexico City", -360, NONE), new Zone("Chicago", -360, US), new Zone("New York", -300, US),
      new Zone("Toronto", -300, US), new Zone("Bogota", -300, NONE), new Zone("Lima", -300, NONE),
      new Zone("Halifax", -240, US), new Zone("Buenos Aires", -180, NONE), new Zone("Sao Paulo", -180, NONE),
      new Zone("Reykjavik", 0, NONE), new Zone("London", 0, EU), new Zone("Lisbon", 0, EU), new Zone("Dublin", 0, EU),
      new Zone("Lagos", 60, NONE), new Zone("Paris", 60, EU), new Zone("Madrid", 60, EU), new Zone("Rome", 60, EU),
      new Zone("Berlin", 60, EU), new Zone("Amsterdam", 60, EU), new Zone("Stockholm", 60, EU),
      new Zone("Johannesburg", 120, NONE), new Zone("Athens", 120, EU), new Zone("Helsinki", 120, EU),
      new Zone("Kyiv", 120, EU), new Zone("Istanbul", 180, NONE), new Zone("Moscow", 180, NONE),
      new Zone("Nairobi", 180, NONE), new Zone("Riyadh", 180, NONE), new Zone("Tehran", 210, NONE),
      new Zone("Dubai", 240, NONE), new Zone("Karachi", 300, NONE), new Zone("Delhi", 330, NONE),
      new Zone("Kathmandu", 345, NONE), new Zone("Dhaka", 360, NONE), new Zone("Bangkok", 420, NONE),
      new Zone("Jakarta", 420, NONE), new Zone("Singapore", 480, NONE), new Zone("Hong Kong", 480, NONE),
      new Zone("Beijing", 480, NONE), new Zone("Manila", 480, NONE), new Zone("Perth", 480, NONE),
      new Zone("Seoul", 540, NONE), new Zone("Tokyo", 540, NONE), new Zone("Adelaide", 570, AU),
      new Zone("Brisbane", 600, NONE), new Zone("Sydney", 600, AU), new Zone("Melbourne", 600, AU),
      new Zone("Auckland", 720, NZ)
   };

   /** The computer's own time zone. */
   static final Zone HERE = new Zone("Here", 0, HERE_RULE);

   final String name;
   /** Minutes ahead of UTC outside daylight saving time. */
   final int standard;
   final int rule;

   Zone(String name, int standard, int rule) {
      this.name = name;
      this.standard = standard;
      this.rule = rule;
   }

   static Zone named(String name) {
      for (Zone zone : CITIES) {
         if (zone.name.equalsIgnoreCase(name)) {
            return zone;
         }
      }
      return null;
   }

   /** Minutes ahead of UTC at the instant {@code utc}, daylight saving included. */
   @SuppressWarnings("deprecation")
   int offset(long utc) {
      if (this.rule == HERE_RULE) {
         // Date's local-time getters, against the instant. (Its getTimezoneOffset asks TimeZone, which in the
         // browser is UTC, while the getters read the browser's local time.)
         Date date = new Date(utc);
         long local = days(date.getYear() + 1900, date.getMonth() + 1, date.getDate()) * DAY
               + date.getHours() * HOUR + date.getMinutes() * MINUTE;
         return (int) Math.round((local - Math.floorDiv(utc, MINUTE) * MINUTE) / (double) MINUTE);
      }
      return this.standard + (this.summer(utc) ? 60 : 0);
   }

   /** True while daylight saving time is on. */
   boolean summer(long utc) {
      if (this.rule == NONE || this.rule == HERE_RULE) {
         return false;
      }
      long std = this.standard * MINUTE;
      int year = civil(Math.floorDiv(utc + std, DAY))[0];
      switch (this.rule) {
         case US:
            // 2 am standard time to 2 am daylight time (1 am standard).
            return utc >= sunday(year, 3, 2) * DAY + 2 * HOUR - std && utc < sunday(year, 11, 1) * DAY + HOUR - std;
         case EU:
            return utc >= lastSunday(year, 3) * DAY + HOUR && utc < lastSunday(year, 10) * DAY + HOUR;
         case AU:
            return utc < sunday(year, 4, 1) * DAY + 2 * HOUR - std || utc >= sunday(year, 10, 1) * DAY + 2 * HOUR - std;
         case NZ:
            return utc < sunday(year, 4, 1) * DAY + 2 * HOUR - std || utc >= lastSunday(year, 9) * DAY + 2 * HOUR - std;
         default:
            return false;
      }
   }

   /** The wall clock time in this zone, as milliseconds since 1970 counted on that wall clock. */
   long local(long utc) {
      return utc + this.offset(utc) * MINUTE;
   }

   /** The local hour of the day as a fraction: 13.5 for half past one in the afternoon. */
   double hourOfDay(long utc) {
      return Math.floorMod(this.local(utc), DAY) / (double) HOUR;
   }

   /** The local day, as days since 1 January 1970. */
   long day(long utc) {
      return Math.floorDiv(this.local(utc), DAY);
   }

   /** The UTC instant of local midnight starting the local day that holds {@code utc}. */
   long midnight(long utc) {
      long local = this.day(utc) * DAY;
      return local - this.offset(local - this.offset(utc) * MINUTE) * MINUTE;
   }

   /** The local time of day: "3:42 pm" or "15:42". */
   String time(long utc, boolean twentyFour) {
      long minutes = Math.floorMod(this.local(utc), DAY) / MINUTE;
      return clock((int) (minutes / 60), (int) (minutes % 60), twentyFour);
   }

   static String clock(int hours, int minutes, boolean twentyFour) {
      String mm = Ui.twoDigits(minutes);
      if (twentyFour) {
         return Ui.twoDigits(hours) + ":" + mm;
      }
      return (hours % 12 == 0 ? 12 : hours % 12) + ":" + mm + (hours < 12 ? " am" : " pm");
   }

   /** An hour on a strip or a dial: "3p" or "15". */
   static String hour(int hours, boolean twentyFour) {
      if (twentyFour) {
         return String.valueOf(hours);
      }
      return (hours % 12 == 0 ? 12 : hours % 12) + (hours < 12 ? "a" : "p");
   }

   /** A difference in minutes as "+9 h", "-5:30 h" or "same time". */
   static String difference(int minutes) {
      if (minutes == 0) {
         return "same time";
      }
      int size = Math.abs(minutes);
      return (minutes > 0 ? "+" : "-") + size / 60 + (size % 60 == 0 ? "" : ":" + Ui.twoDigits(size % 60)) + " h";
   }

   // ---- Dates from day numbers (days since 1 January 1970), after Howard Hinnant's civil calendar algorithms ----

   static long days(int year, int month, int day) {
      int y = month <= 2 ? year - 1 : year;
      long era = Math.floorDiv(y, 400);
      long yearOfEra = y - era * 400;
      long dayOfYear = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
      long dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
      return era * 146097 + dayOfEra - 719468;
   }

   /** Year, month (1 to 12) and day of the month. */
   static int[] civil(long days) {
      long z = days + 719468;
      long era = Math.floorDiv(z, 146097);
      long dayOfEra = z - era * 146097;
      long yearOfEra = (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365;
      long dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
      long mp = (5 * dayOfYear + 2) / 153;
      int day = (int) (dayOfYear - (153 * mp + 2) / 5 + 1);
      int month = (int) (mp < 10 ? mp + 3 : mp - 9);
      return new int[]{(int) (yearOfEra + era * 400 + (month <= 2 ? 1 : 0)), month, day};
   }

   /** 0 for Sunday to 6 for Saturday. */
   static int weekday(long days) {
      return (int) Math.floorMod(days + 4, 7);
   }

   /** The day of the {@code n}th Sunday of the month. */
   static long sunday(int year, int month, int n) {
      long first = days(year, month, 1);
      return first + (7 - weekday(first)) % 7 + 7 * (n - 1);
   }

   static long lastSunday(int year, int month) {
      long last = days(year, month + 1, 1) - 1;
      return last - weekday(last);
   }
}
