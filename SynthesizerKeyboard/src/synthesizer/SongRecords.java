package synthesizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The best result for each song, kept in a small text file of {@code ID=stars,misses} lines such as
 * {@code ODE=3,0}. A song never finished has no line.
 */
public final class SongRecords {
   private final File file;
   private final Map<String, int[]> best = new LinkedHashMap<>();

   private SongRecords(File file) {
      this.file = file;
   }

   /** Reads the file; a missing or unreadable file, or a line this version does not know, is skipped. */
   public static SongRecords load(File file) {
      SongRecords records = new SongRecords(file);
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         for (String line; (line = reader.readLine()) != null; ) {
            int split = line.indexOf('=');
            String[] values = split > 0 ? line.substring(split + 1).split(",") : new String[0];
            if (values.length == 2) {
               try {
                  records.best.put(line.substring(0, split).trim(), new int[]{
                     Math.max(1, Math.min(3, Integer.parseInt(values[0].trim()))), Math.max(0, Integer.parseInt(values[1].trim()))});
               } catch (NumberFormatException e) {
                  // Not a record: skip it.
               }
            }
         }
      } catch (IOException e) {
         // No file yet: no song finished so far.
      }
      return records;
   }

   /** The best stars for {@code song} (1 to 3), or 0 if it was never finished. */
   public int stars(Song song) {
      int[] record = this.best.get(song.id());
      return record == null ? 0 : record[0];
   }

   /** Keeps a finished lesson if it beats the best one (fewer misses); returns true when it does. */
   public boolean add(Lesson lesson) {
      int[] record = this.best.get(lesson.song().id());
      if (record != null && record[1] <= lesson.misses()) {
         return false;
      }
      this.best.put(lesson.song().id(), new int[]{lesson.stars(), lesson.misses()});
      this.save();
      return true;
   }

   private void save() {
      try (Writer writer = new FileWriter(this.file)) {
         for (Map.Entry<String, int[]> entry : this.best.entrySet()) {
            writer.write(entry.getKey() + "=" + entry.getValue()[0] + "," + entry.getValue()[1] + "\n");
         }
      } catch (IOException e) {
         // Could not save: the record lasts until the window closes.
      }
   }
}
