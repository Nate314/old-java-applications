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
      new Zone("Honolulu", -600, NONE, 21.31, -157.86), new Zone("Anchorage", -540, US, 61.22, -149.90), new Zone("Los Angeles", -480, US, 34.05, -118.24),
      new Zone("Vancouver", -480, US, 49.28, -123.12), new Zone("Phoenix", -420, NONE, 33.45, -112.07), new Zone("Denver", -420, US, 39.74, -104.99),
      new Zone("Mexico City", -360, NONE, 19.43, -99.13), new Zone("Chicago", -360, US, 41.88, -87.63), new Zone("New York", -300, US, 40.71, -74.01),
      new Zone("Toronto", -300, US, 43.65, -79.38), new Zone("Bogota", -300, NONE, 4.71, -74.07), new Zone("Lima", -300, NONE, -12.05, -77.04),
      new Zone("Halifax", -240, US, 44.65, -63.57), new Zone("Buenos Aires", -180, NONE, -34.60, -58.38), new Zone("Sao Paulo", -180, NONE, -23.55, -46.63),
      new Zone("Reykjavik", 0, NONE, 64.15, -21.94), new Zone("London", 0, EU, 51.51, -0.13), new Zone("Lisbon", 0, EU, 38.72, -9.14), new Zone("Dublin", 0, EU, 53.35, -6.26),
      new Zone("Lagos", 60, NONE, 6.52, 3.38), new Zone("Paris", 60, EU, 48.86, 2.35), new Zone("Madrid", 60, EU, 40.42, -3.70), new Zone("Rome", 60, EU, 41.90, 12.50),
      new Zone("Berlin", 60, EU, 52.52, 13.40), new Zone("Amsterdam", 60, EU, 52.37, 4.90), new Zone("Stockholm", 60, EU, 59.33, 18.07),
      new Zone("Johannesburg", 120, NONE, -26.20, 28.05), new Zone("Athens", 120, EU, 37.98, 23.73), new Zone("Helsinki", 120, EU, 60.17, 24.94),
      new Zone("Kyiv", 120, EU, 50.45, 30.52), new Zone("Istanbul", 180, NONE, 41.01, 28.98), new Zone("Moscow", 180, NONE, 55.76, 37.62),
      new Zone("Nairobi", 180, NONE, -1.29, 36.82), new Zone("Riyadh", 180, NONE, 24.71, 46.68), new Zone("Tehran", 210, NONE, 35.69, 51.39),
      new Zone("Dubai", 240, NONE, 25.20, 55.27), new Zone("Karachi", 300, NONE, 24.86, 67.01), new Zone("Delhi", 330, NONE, 28.61, 77.21),
      new Zone("Kathmandu", 345, NONE, 27.72, 85.32), new Zone("Dhaka", 360, NONE, 23.81, 90.41), new Zone("Bangkok", 420, NONE, 13.76, 100.50),
      new Zone("Jakarta", 420, NONE, -6.21, 106.85), new Zone("Singapore", 480, NONE, 1.35, 103.82), new Zone("Hong Kong", 480, NONE, 22.32, 114.17),
      new Zone("Beijing", 480, NONE, 39.90, 116.41), new Zone("Manila", 480, NONE, 14.60, 120.98), new Zone("Perth", 480, NONE, -31.95, 115.86),
      new Zone("Seoul", 540, NONE, 37.57, 126.98), new Zone("Tokyo", 540, NONE, 35.68, 139.69), new Zone("Adelaide", 570, AU, -34.93, 138.60),
      new Zone("Brisbane", 600, NONE, -27.47, 153.03), new Zone("Sydney", 600, AU, -33.87, 151.21), new Zone("Melbourne", 600, AU, -37.81, 144.96),
      new Zone("Auckland", 720, NZ, -36.85, 174.76)
   };

   /** The computer's own time zone. */
   static final Zone HERE = new Zone("Here", 0, HERE_RULE, Double.NaN, Double.NaN);

   final String name;
   /** Minutes ahead of UTC outside daylight saving time. */
   final int standard;
   final int rule;
   /** Where the city is, in degrees (north and east positive), for the World map; unknown for here. */
   final double latitude;
   final double longitude;

   Zone(String name, int standard, int rule, double latitude, double longitude) {
      this.name = name;
      this.standard = standard;
      this.rule = rule;
      this.latitude = latitude;
      this.longitude = longitude;
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
