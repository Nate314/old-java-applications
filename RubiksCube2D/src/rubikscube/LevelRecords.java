package rubikscube;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * The best stars and fewest moves on each solved level and the level played last, kept in a small text file of
 * {@code name=value} lines: {@code level=4}, then one line such as {@code 3=3,3} (stars, moves) per solved level.
 */
public final class LevelRecords {
   public static final int LEVELS = 20;
   private final File file;
   private final int[] stars = new int[LEVELS + 1];
   private final int[] moves = new int[LEVELS + 1];
   private int lastLevel = 1;

   private LevelRecords(File file) {
      this.file = file;
   }

   /** Reads the file; a missing or unreadable file gives no records and level 1. */
   public static LevelRecords load(File file) {
      LevelRecords records = new LevelRecords(file);
      if (file == null) {
         return records;
      }
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         for (String line; (line = reader.readLine()) != null; ) {
            int split = line.indexOf('=');
            if (split > 0) {
               records.read(line.substring(0, split).trim(), line.substring(split + 1).trim());
            }
         }
      } catch (IOException e) {
         // No file yet: nothing solved so far.
      }
      return records;
   }

   private void read(String name, String value) {
      try {
         if (name.equals("level")) {
            this.lastLevel = clamp(Integer.parseInt(value));
            return;
         }
         int level = Integer.parseInt(name);
         String[] parts = value.split(",");
         int starCount = Integer.parseInt(parts[0].trim());
         int moveCount = Integer.parseInt(parts[1].trim());
         if (level >= 1 && level <= LEVELS && starCount >= 1 && starCount <= 3 && moveCount > 0) {
            this.stars[level] = starCount;
            this.moves[level] = moveCount;
         }
      } catch (RuntimeException e) {
         // A line this version does not know: skip it.
      }
   }

   private static int clamp(int level) {
      return Math.max(1, Math.min(LEVELS, level));
   }

   /** Stars (1 to 3) won on that level, or 0 when it has not been solved. */
   public int stars(int level) {
      return this.stars[level];
   }

   /** Fewest moves on that level, or 0 when it has not been solved. */
   public int moves(int level) {
      return this.moves[level];
   }

   public int totalStars() {
      int total = 0;
      for (int count : this.stars) {
         total += count;
      }
      return total;
   }

   /** The highest level that can be played: one past the highest level solved in order from level 1. */
   public int unlocked() {
      int level = 1;
      while (level < LEVELS && this.stars[level] > 0) {
         level++;
      }
      return level;
   }

   public int lastLevel() {
      return Math.min(this.lastLevel, this.unlocked());
   }

   public void setLastLevel(int level) {
      if (this.lastLevel != level) {
         this.lastLevel = level;
         this.save();
      }
   }

   /** Records a solve; keeps the most stars and the fewest moves seen on that level and saves. */
   public void record(int level, int starCount, int moveCount) {
      this.stars[level] = Math.max(this.stars[level], starCount);
      this.moves[level] = this.moves[level] == 0 ? moveCount : Math.min(this.moves[level], moveCount);
      this.save();
   }

   /** Stars for solving a level of that par in that many moves: 3 at par or better, 2 within twice par plus 2, else 1; a hint caps it at 1. */
   public static int starsFor(int par, int moveCount, boolean usedHint) {
      if (usedHint) {
         return 1;
      }
      return moveCount <= par ? 3 : moveCount <= 2 * par + 2 ? 2 : 1;
   }

   private void save() {
      if (this.file == null) {
         return;
      }
      try (Writer writer = new FileWriter(this.file)) {
         writer.write("level=" + this.lastLevel + "\n");
         for (int level = 1; level <= LEVELS; level++) {
            if (this.stars[level] > 0) {
               writer.write(level + "=" + this.stars[level] + "," + this.moves[level] + "\n");
            }
         }
      } catch (IOException e) {
         System.err.println("Could not save level records: " + e.getMessage());
      }
   }
}
