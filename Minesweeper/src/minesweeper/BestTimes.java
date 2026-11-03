package minesweeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * The fastest win on each level, the level played last and whether boards are dealt with no guessing, kept in a small
 * text file of {@code name=value} lines: {@code level=CLASSIC}, {@code noguess=on}, then one line such as
 * {@code BEGINNER=42317} (milliseconds) per level that has been won.
 */
public final class BestTimes {
   private final File file;
   private final long[] best = new long[Level.values().length];
   private Level lastLevel = Level.CLASSIC;
   private boolean noGuess = true;

   private BestTimes(File file) {
      this.file = file;
   }

   /** Reads the file; a missing or unreadable file gives no times and the Classic level. */
   public static BestTimes load(File file) {
      BestTimes times = new BestTimes(file);
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         for (String line; (line = reader.readLine()) != null; ) {
            int split = line.indexOf('=');
            if (split > 0) {
               times.read(line.substring(0, split).trim(), line.substring(split + 1).trim());
            }
         }
      } catch (IOException e) {
         // No file yet: nothing won so far.
      }
      return times;
   }

   private void read(String name, String value) {
      try {
         if (name.equals("level")) {
            this.lastLevel = Level.valueOf(value);
         } else if (name.equals("noguess")) {
            this.noGuess = !value.equals("off");
         } else {
            long millis = Long.parseLong(value);
            if (millis > 0) {
               this.best[Level.valueOf(name).ordinal()] = millis;
            }
         }
      } catch (IllegalArgumentException e) {
         // A line this version does not know: skip it.
      }
   }

   /** The best time on that level in milliseconds, or 0 when it has not been won. */
   public long best(Level level) {
      return this.best[level.ordinal()];
   }

   public Level lastLevel() {
      return this.lastLevel;
   }

   public void setLastLevel(Level level) {
      if (this.lastLevel != level) {
         this.lastLevel = level;
         this.save();
      }
   }

   /** True (the default) when every board is dealt so that it can be cleared without a guess (see {@link FairBoard}). */
   public boolean noGuess() {
      return this.noGuess;
   }

   public void setNoGuess(boolean on) {
      if (this.noGuess != on) {
         this.noGuess = on;
         this.save();
      }
   }

   /** Records a win; returns true (and saves) when it beats the best time on that level. */
   public boolean record(Level level, long millis) {
      long before = this.best(level);
      if (before != 0 && millis >= before) {
         return false;
      }
      this.best[level.ordinal()] = millis;
      this.save();
      return true;
   }

   private void save() {
      try (Writer writer = new FileWriter(this.file)) {
         writer.write("level=" + this.lastLevel.name() + "\n");
         writer.write("noguess=" + (this.noGuess ? "on" : "off") + "\n");
         for (Level level : Level.values()) {
            if (this.best(level) > 0) {
               writer.write(level.name() + "=" + this.best(level) + "\n");
            }
         }
      } catch (IOException e) {
         System.err.println("Could not save the best times: " + e.getMessage());
      }
   }

   /** Minutes, seconds and tenths, like 1:05.3. */
   public static String format(long millis) {
      long tenths = millis / 100;
      long seconds = tenths / 10;
      return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60 + "." + tenths % 10;
   }
}
