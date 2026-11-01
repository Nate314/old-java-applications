package ultimateticktacktoe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Games won, lost and drawn against each computer level, and the mode played last, kept in a small text file of
 * {@code name=value} lines: {@code mode=HARD}, then one line such as {@code HARD=2,5,1} (wins, losses, draws) per
 * level that has been played.
 */
public final class Record {
   private final File file;
   private final int[][] results = new int[Mode.values().length][3];
   private Mode lastMode = Mode.EASY;

   private Record(File file) {
      this.file = file;
   }

   /** Reads the file; a missing or unreadable file gives an empty record and the Easy level. */
   public static Record load(File file) {
      Record record = new Record(file);
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         for (String line; (line = reader.readLine()) != null; ) {
            int split = line.indexOf('=');
            if (split > 0) {
               record.read(line.substring(0, split).trim(), line.substring(split + 1).trim());
            }
         }
      } catch (IOException e) {
         // No file yet: nothing played so far.
      }
      return record;
   }

   private void read(String name, String value) {
      try {
         if (name.equals("mode")) {
            this.lastMode = Mode.valueOf(value);
         } else {
            String[] counts = value.split(",");
            int[] row = this.results[Mode.valueOf(name).ordinal()];
            for (int i = 0; i < row.length && i < counts.length; i++) {
               row[i] = Math.max(0, Integer.parseInt(counts[i].trim()));
            }
         }
      } catch (IllegalArgumentException e) {
         // A line this version does not know: skip it.
      }
   }

   public Mode lastMode() {
      return this.lastMode;
   }

   public void setLastMode(Mode mode) {
      if (this.lastMode != mode) {
         this.lastMode = mode;
         this.save();
      }
   }

   /** Counts a finished game against the computer; {@code winner} is "X" (the player), "O" or DRAW. */
   public void add(Mode mode, String winner) {
      int column = "X".equals(winner) ? 0 : "O".equals(winner) ? 1 : 2;
      this.results[mode.ordinal()][column]++;
      this.save();
   }

   /** Like "3 wins, 1 loss, 0 draws". */
   public String describe(Mode mode) {
      int[] row = this.results[mode.ordinal()];
      return plural(row[0], "win", "wins") + ", " + plural(row[1], "loss", "losses") + ", " + plural(row[2], "draw", "draws");
   }

   private static String plural(int count, String one, String many) {
      return count + " " + (count == 1 ? one : many);
   }

   private void save() {
      try (Writer writer = new FileWriter(this.file)) {
         writer.write("mode=" + this.lastMode.name() + "\n");
         for (Mode mode : Mode.values()) {
            int[] row = this.results[mode.ordinal()];
            if (mode.vsComputer() && row[0] + row[1] + row[2] > 0) {
               writer.write(mode.name() + "=" + row[0] + "," + row[1] + "," + row[2] + "\n");
            }
         }
      } catch (IOException e) {
         System.err.println("Could not save the record: " + e.getMessage());
      }
   }
}
