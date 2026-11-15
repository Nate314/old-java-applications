package launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * How often and when each app was last launched from the launcher, kept in a text file
 * (one line per app: id, count, last launch in epoch milliseconds). Beside it, {@code <name>-seen.txt} keeps the
 * newest change each app had when it was last launched (id, commit hash), so the launcher can tell what is new since.
 * It is a file of its own so that a launcher from before it still reads the play record.
 */
public final class PlayLog {
   private final Path file;
   private final Path seenFile;
   private final Map<String, long[]> entries = new LinkedHashMap<>();   // id -> {count, last millis}
   private final Map<String, String> seen = new LinkedHashMap<>();      // id -> newest commit hash at its last launch

   public PlayLog(Path file) {
      this.file = file;
      seenFile = file.resolveSibling(file.getFileName().toString().replaceFirst("\\.txt$", "") + "-seen.txt");
      try {
         for (String line : Files.readAllLines(seenFile, StandardCharsets.UTF_8)) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 2) seen.put(parts[0], parts[1]);
         }
      } catch (IOException ignored) {
         // none kept yet
      }
      try {
         for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length != 3) continue;
            try {
               entries.put(parts[0], new long[] {Long.parseLong(parts[1]), Long.parseLong(parts[2])});
            } catch (NumberFormatException ignored) {
               // a damaged line is skipped
            }
         }
      } catch (IOException ignored) {
         // no file yet: nothing launched
      }
   }

   public int count(String id) {
      long[] e = entries.get(id);
      return e == null ? 0 : (int) e[0];
   }

   public long last(String id) {
      long[] e = entries.get(id);
      return e == null ? 0 : e[1];
   }

   /** The hash of the app's newest change when it was last launched, or null when not known. */
   public String seen(String id) {
      return seen.get(id);
   }

   public void launched(String id, long now) {
      launched(id, now, null);
   }

   /**
    * Records a launch now, with the app's newest change at that moment (null keeps the one kept before), and saves
    * the files. A file that cannot be written only loses the record.
    */
   public void launched(String id, long now, String newest) {
      long[] e = entries.computeIfAbsent(id, k -> new long[2]);
      e[0]++;
      e[1] = now;
      StringBuilder out = new StringBuilder();
      for (Map.Entry<String, long[]> en : entries.entrySet()) {
         out.append(en.getKey()).append(' ').append(en.getValue()[0]).append(' ').append(en.getValue()[1]).append('\n');
      }
      write(file, out);
      if (newest == null) return;
      seen.put(id, newest);
      StringBuilder kept = new StringBuilder();
      for (Map.Entry<String, String> en : seen.entrySet()) kept.append(en.getKey()).append(' ').append(en.getValue()).append('\n');
      write(seenFile, kept);
   }

   private static void write(Path to, CharSequence text) {
      try {
         Files.write(to, text.toString().getBytes(StandardCharsets.UTF_8));
      } catch (IOException ex) {
         System.err.println("Could not save " + to + ": " + ex.getMessage());
      }
   }

   /** "New", or "Played 3 times, last today" (yesterday, 4 days ago). */
   public String describe(String id, long now, ZoneId zone) {
      int n = count(id);
      if (n == 0) return "New: not launched here yet";
      LocalDate then = Instant.ofEpochMilli(last(id)).atZone(zone).toLocalDate();
      long days = ChronoUnit.DAYS.between(then, Instant.ofEpochMilli(now).atZone(zone).toLocalDate());
      String when = days <= 0 ? "today" : days == 1 ? "yesterday" : days + " days ago";
      return "Played " + (n == 1 ? "once" : n == 2 ? "twice" : n + " times") + ", last " + when;
   }
}
