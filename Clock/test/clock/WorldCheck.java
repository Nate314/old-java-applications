package clock;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-check for the World tab: every city's offset (daylight saving included) matches the JDK's time zone database
 * every quarter hour around each change from 2023 to 2030 and every six hours in between, the calendar math, the
 * hours that suit everyone, and the saved cities. Run with java -ea.
 */
public class WorldCheck {
   private static final long H = 3_600_000L;

   public static void main(String[] args) throws Exception {
      Map<String, String> ids = new HashMap<>();
      String[][] pairs = {
         {"Honolulu", "Pacific/Honolulu"}, {"Anchorage", "America/Anchorage"}, {"Los Angeles", "America/Los_Angeles"},
         {"Vancouver", "America/Vancouver"}, {"Phoenix", "America/Phoenix"}, {"Denver", "America/Denver"},
         {"Mexico City", "America/Mexico_City"}, {"Chicago", "America/Chicago"}, {"New York", "America/New_York"},
         {"Toronto", "America/Toronto"}, {"Bogota", "America/Bogota"}, {"Lima", "America/Lima"},
         {"Halifax", "America/Halifax"}, {"Buenos Aires", "America/Argentina/Buenos_Aires"},
         {"Sao Paulo", "America/Sao_Paulo"}, {"Reykjavik", "Atlantic/Reykjavik"}, {"London", "Europe/London"},
         {"Lisbon", "Europe/Lisbon"}, {"Dublin", "Europe/Dublin"}, {"Lagos", "Africa/Lagos"}, {"Paris", "Europe/Paris"},
         {"Madrid", "Europe/Madrid"}, {"Rome", "Europe/Rome"}, {"Berlin", "Europe/Berlin"},
         {"Amsterdam", "Europe/Amsterdam"}, {"Stockholm", "Europe/Stockholm"}, {"Johannesburg", "Africa/Johannesburg"},
         {"Athens", "Europe/Athens"}, {"Helsinki", "Europe/Helsinki"}, {"Kyiv", "Europe/Kyiv"},
         {"Istanbul", "Europe/Istanbul"}, {"Moscow", "Europe/Moscow"}, {"Nairobi", "Africa/Nairobi"},
         {"Riyadh", "Asia/Riyadh"}, {"Tehran", "Asia/Tehran"}, {"Dubai", "Asia/Dubai"}, {"Karachi", "Asia/Karachi"},
         {"Delhi", "Asia/Kolkata"}, {"Kathmandu", "Asia/Kathmandu"}, {"Dhaka", "Asia/Dhaka"},
         {"Bangkok", "Asia/Bangkok"}, {"Jakarta", "Asia/Jakarta"}, {"Singapore", "Asia/Singapore"},
         {"Hong Kong", "Asia/Hong_Kong"}, {"Beijing", "Asia/Shanghai"}, {"Manila", "Asia/Manila"},
         {"Perth", "Australia/Perth"}, {"Seoul", "Asia/Seoul"}, {"Tokyo", "Asia/Tokyo"},
         {"Adelaide", "Australia/Adelaide"}, {"Brisbane", "Australia/Brisbane"}, {"Sydney", "Australia/Sydney"},
         {"Melbourne", "Australia/Melbourne"}, {"Auckland", "Pacific/Auckland"}
      };
      for (String[] pair : pairs) {
         ids.put(pair[0], pair[1]);
      }
      check(ids.size() == Zone.CITIES.length, "every city has a JDK zone to compare with");

      // Offsets against the JDK, 2023 (after Mexico and Iran dropped daylight saving) to 2030.
      long from = Instant.parse("2023-01-01T00:00:00Z").toEpochMilli();
      long to = Instant.parse("2031-01-01T00:00:00Z").toEpochMilli();
      int compared = 0;
      for (Zone zone : Zone.CITIES) {
         ZoneId id = ZoneId.of(ids.get(zone.name));
         for (long t = from; t < to; t += 6 * H) {
            compared += same(zone, id, t);
         }
         // Around every change, quarter hour by quarter hour.
         for (long t = from; t < to; t += 6 * H) {
            if (zone.offset(t) != zone.offset(t + 6 * H)) {
               for (long q = t; q <= t + 6 * H; q += H / 4) {
                  compared += same(zone, id, q);
               }
               compared += same(zone, id, firstChange(zone, t, t + 6 * H) - 1);
               compared += same(zone, id, firstChange(zone, t, t + 6 * H));
            }
         }
      }
      check(compared > 600_000, "compared " + compared + " instants");
      check(Zone.named("New York").summer(Instant.parse("2026-07-01T12:00:00Z").toEpochMilli()), "New York in summer time in July");
      check(!Zone.named("Sydney").summer(Instant.parse("2026-07-01T12:00:00Z").toEpochMilli()), "Sydney in winter in July");
      check(Zone.named("Kathmandu").offset(0) == 345, "Kathmandu is 5:45 ahead");
      // Here, from Date's local-time getters, agrees with the JVM's own zone through a year.
      for (long t = from; t < from + 400 * 24 * H; t += 7 * H + 60_001) {
         check(Zone.HERE.offset(t) == java.util.TimeZone.getDefault().getOffset(t) / 60_000, "here's offset at " + t);
      }

      // Calendar math against java.time, a day at a time over 400 years.
      for (long day = LocalDate.of(1900, 1, 1).toEpochDay(); day < LocalDate.of(2300, 1, 1).toEpochDay(); day++) {
         LocalDate date = LocalDate.ofEpochDay(day);
         int[] civil = Zone.civil(day);
         if (civil[0] != date.getYear() || civil[1] != date.getMonthValue() || civil[2] != date.getDayOfMonth()
               || Zone.days(civil[0], civil[1], civil[2]) != day || Zone.weekday(day) != date.getDayOfWeek().getValue() % 7) {
            throw new AssertionError("calendar wrong on " + date);
         }
      }
      check(Zone.sunday(2026, 3, 2) == LocalDate.of(2026, 3, 8).toEpochDay(), "second Sunday of March 2026 is the 8th");
      check(Zone.lastSunday(2026, 10) == LocalDate.of(2026, 10, 25).toEpochDay(), "last Sunday of October 2026 is the 25th");

      // Midnight, hours and formats.
      Zone london = Zone.named("London");
      long summerNoon = Instant.parse("2026-07-01T11:00:00Z").toEpochMilli();
      check(london.hourOfDay(summerNoon) == 12.0, "noon in London in July is 11:00 UTC");
      check(london.midnight(summerNoon) == Instant.parse("2026-06-30T23:00:00Z").toEpochMilli(), "London's midnight in July");
      check(london.time(summerNoon, false).equals("12:00 pm") && london.time(summerNoon, true).equals("12:00"), "times");
      check(Zone.difference(330).equals("+5:30 h") && Zone.difference(-300).equals("-5 h") && Zone.difference(0).equals("same time"), "differences");
      check(Zone.hour(0, false).equals("12a") && Zone.hour(13, false).equals("1p") && Zone.hour(13, true).equals("13"), "hour labels");
      check(WorldView.away(3 * H + 15 * 60_000).equals("In 3 h 15 min") && WorldView.away(-2 * H).equals("2 h ago")
            && WorldView.away(26 * H).equals("In 1 d 2 h") && WorldView.away(20_000).equals("Now"), "away");
      check(WorldView.date(LocalDate.of(2026, 11, 3).toEpochDay()).equals("Tuesday, November 3"), "date line");

      // Hours that suit everyone: London and New York both at work 9-17 London time means 14:00 to 17:00 London.
      long londonDay = london.midnight(summerNoon);
      boolean[] work = WorldView.together(List.of(london, Zone.named("New York")), londonDay, WorldView.WORK_FROM, WorldView.WORK_TO);
      boolean[] expected = new boolean[24];
      Arrays.fill(expected, 14, 17, true);
      check(Arrays.equals(work, expected), "London and New York share 2 pm to 5 pm London time");
      boolean[] none = WorldView.together(List.of(london, Zone.named("Auckland")), londonDay, WorldView.WORK_FROM, WorldView.WORK_TO);
      check(!any(none), "London and Auckland share no working hour");
      check(WorldView.mostAwake(List.of(london, Zone.named("New York")), londonDay) == 12, "New York wakes at noon London time");
      check(!any(WorldView.together(List.of(london), londonDay, 0, 24)), "one zone alone plans nothing");

      // The saved cities: defaults, a round trip, unknown names and too many dropped.
      File file = File.createTempFile("clock-world", ".txt");
      file.delete();
      ClockState fresh = ClockState.load(file);
      check(fresh.cities.size() == 3 && fresh.cities.get(0).name.equals("London"), "London, New York and Tokyo by default");
      fresh.cities.clear();
      fresh.cities.add(Zone.named("Sydney"));
      fresh.cities.add(Zone.named("Delhi"));
      fresh.mode = ClockState.WORLD;
      fresh.save();
      ClockState back = ClockState.load(file);
      check(back.mode.equals(ClockState.WORLD) && back.cities.size() == 2 && back.cities.get(1).name.equals("Delhi"), "cities round trip");
      java.nio.file.Files.write(file.toPath(), "world=Paris, Atlantis,Paris,Lima,Rome,Seoul,Dubai,Tokyo\n".getBytes("utf-8"));
      ClockState odd = ClockState.load(file);
      check(odd.cities.size() == WorldView.MAX_CITIES && odd.cities.get(0).name.equals("Paris") && odd.cities.get(1).name.equals("Lima"),
            "unknown and repeated names dropped, at most five kept");
      java.nio.file.Files.write(file.toPath(), "world=\n".getBytes("utf-8"));
      check(ClockState.load(file).cities.isEmpty(), "an empty list stays empty");
      file.delete();

      System.out.println("WorldCheck passed (" + compared + " instants compared)");
   }

   private static int same(Zone zone, ZoneId id, long t) {
      int jdk = id.getRules().getOffset(Instant.ofEpochMilli(t)).getTotalSeconds() / 60;
      if (zone.offset(t) != jdk) {
         throw new AssertionError(zone.name + " at " + Instant.ofEpochMilli(t) + ": " + zone.offset(t) + " but the JDK says " + jdk);
      }
      return 1;
   }

   /** The first millisecond in [from, to] with the offset of {@code to}. */
   private static long firstChange(Zone zone, long from, long to) {
      int after = zone.offset(to);
      while (from < to) {
         long middle = from + (to - from) / 2;
         if (zone.offset(middle) == after) {
            to = middle;
         } else {
            from = middle + 1;
         }
      }
      return from;
   }

   private static boolean any(boolean[] values) {
      for (boolean value : values) {
         if (value) {
            return true;
         }
      }
      return false;
   }

   private static void check(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError(what);
      }
   }
}
