package flappyBird;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Today's course: the pipes are seeded by the date, so every run on the same day meets the same pipes, and the best run
 * of the day is kept as a ghost to race. The ghost is the bird's height every few steps, as a share of the window
 * height, so it replays at any window size.
 */
public final class DailyCourse {
   public static final File FILE = new File(System.getProperty("user.home", "."), ".flappybird-today.txt");
   /** The ghost stores the bird's height once every this many game steps and fills in the steps between. */
   public static final int SAMPLE_STEPS = 4;
   /** The day, as yyyymmdd. */
   public final int date;
   /** Pipes passed in today's best run, 0 before the first run of the day. */
   private int bestScore;
   /** Today's best run: the bird's height per sample, in thousandths of the window height. */
   private int[] track = new int[0];

   public DailyCourse(int date) {
      this.date = date;
   }

   /** Today in local time. Date's getters, not Calendar's, because Calendar uses UTC in the browser (TeaVM). */
   @SuppressWarnings("deprecation")
   public static int today() {
      Date now = new Date();
      return (now.getYear() + 1900) * 10000 + (now.getMonth() + 1) * 100 + now.getDate();
   }

   /** The pipe generator for this day. The date is mixed so neighbouring days do not start alike. */
   public Random pipeRandom() {
      return new Random(this.date * 0x9E3779B97F4A7C15L);
   }

   public int bestScore() {
      return this.bestScore;
   }

   public boolean hasGhost() {
      return this.track.length > 1;
   }

   /** The ghost's height at a game step, as a share of the window height, or a negative number once its run is over. */
   public float ghostY(int step) {
      int index = step / SAMPLE_STEPS;
      if (step < 0 || index + 1 >= this.track.length) {
         return -1.0F;
      }
      float between = (float)(step % SAMPLE_STEPS) / SAMPLE_STEPS;
      return (this.track[index] + (this.track[index + 1] - this.track[index]) * between) / 1000.0F;
   }

   /** Keeps a run as today's best if it passed more pipes. Returns whether it did. */
   public boolean offer(int score, List<Integer> samples) {
      if (score <= this.bestScore) {
         return false;
      }
      this.bestScore = score;
      this.track = new int[samples.size()];
      for (int i = 0; i < this.track.length; i++) {
         this.track[i] = samples.get(i);
      }
      return true;
   }

   /** Today's saved course from the file, or a fresh one if the file is missing, unreadable or from another day. */
   public static DailyCourse load(File file, int date) {
      DailyCourse course = new DailyCourse(date);
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         int savedDate = 0;
         int savedBest = 0;
         int[] savedTrack = new int[0];
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            int equals = line.indexOf('=');
            if (equals < 0) {
               continue;
            }
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            if (key.equals("date")) {
               savedDate = Integer.parseInt(value);
            } else if (key.equals("best")) {
               savedBest = Integer.parseInt(value);
            } else if (key.equals("track") && !value.isEmpty()) {
               String[] parts = value.split(",");
               savedTrack = new int[parts.length];
               for (int i = 0; i < parts.length; i++) {
                  savedTrack[i] = Integer.parseInt(parts[i].trim());
               }
            }
         }
         if (savedDate == date && savedBest > 0) {
            course.bestScore = savedBest;
            course.track = savedTrack;
         }
      } catch (IOException | RuntimeException e) {
         // No course saved for today yet.
      }
      return course;
   }

   public void save(File file) {
      StringBuilder text = new StringBuilder();
      text.append("date=").append(this.date).append('\n');
      text.append("best=").append(this.bestScore).append('\n');
      text.append("track=");
      for (int i = 0; i < this.track.length; i++) {
         if (i > 0) {
            text.append(',');
         }
         text.append(this.track[i]);
      }
      text.append('\n');
      try (FileWriter writer = new FileWriter(file)) {
         writer.write(text.toString());
      } catch (IOException e) {
         System.err.println("Could not save today's course: " + e.getMessage());
      }
   }
}
