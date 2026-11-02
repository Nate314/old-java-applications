package clock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * What the window remembers between runs, in {@code ~/.clock.txt}: the tab, the face, 12 or 24 hours, and the timer
 * and stopwatch as wall clock times, so a running timer is still running (or has rung) when the window opens again.
 */
final class ClockState {
   static final String CLOCK = "clock";
   static final String TIMER = "timer";
   static final String STOPWATCH = "stopwatch";

   private final File file;
   String mode = CLOCK;
   boolean analog = true;
   boolean twentyFour = false;
   final Countdown countdown = new Countdown();
   final Stopwatch stopwatch = new Stopwatch();

   ClockState(File file) {
      this.file = file;
   }

   /** Reads the file; a missing file or an unknown line leaves the defaults. */
   static ClockState load(File file) {
      ClockState state = new ClockState(file);
      state.read(file);
      return state;
   }

   private void read(File file) {
      long length = this.countdown.length();
      long endsAt = 0;
      long left = -1;
      long finishedAt = 0;
      long watchStart = 0;
      long watchBanked = 0;
      List<Long> laps = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
         for (String line; (line = reader.readLine()) != null;) {
            int split = line.indexOf('=');
            if (split < 0) {
               continue;
            }
            String key = line.substring(0, split);
            String value = line.substring(split + 1).trim();
            try {
               if (key.equals("mode") && (value.equals(CLOCK) || value.equals(TIMER) || value.equals(STOPWATCH))) {
                  this.mode = value;
               } else if (key.equals("face")) {
                  this.analog = !value.equals("digital");
               } else if (key.equals("hours")) {
                  this.twentyFour = value.equals("24");
               } else if (key.equals("timer")) {
                  length = Long.parseLong(value);
               } else if (key.equals("timerEnd")) {
                  endsAt = Long.parseLong(value);
               } else if (key.equals("timerLeft")) {
                  left = Long.parseLong(value);
               } else if (key.equals("timerDone")) {
                  finishedAt = Long.parseLong(value);
               } else if (key.equals("watchStart")) {
                  watchStart = Long.parseLong(value);
               } else if (key.equals("watchBanked")) {
                  watchBanked = Long.parseLong(value);
               } else if (key.equals("laps") && !value.isEmpty()) {
                  for (String lap : value.split(",")) {
                     laps.add(Long.parseLong(lap.trim()));
                  }
               }
            } catch (NumberFormatException e) {
               // A damaged number: keep the default for that line.
            }
         }
      } catch (IOException e) {
         return;
      }
      this.countdown.restore(length, endsAt, left < 0 ? length : left, finishedAt);
      this.stopwatch.restore(watchStart, watchBanked, laps);
   }

   void save() {
      try (Writer writer = new OutputStreamWriter(new FileOutputStream(this.file), "utf-8")) {
         writer.write(this.text());
      } catch (IOException e) {
         System.err.println("Could not save " + this.file + ": " + e);
      }
   }

   /** The file's contents. */
   String text() {
      StringBuilder text = new StringBuilder();
      text.append("mode=").append(this.mode).append('\n');
      text.append("face=").append(this.analog ? "analog" : "digital").append('\n');
      text.append("hours=").append(this.twentyFour ? "24" : "12").append('\n');
      text.append("timer=").append(this.countdown.length()).append('\n');
      if (this.countdown.isRunning()) {
         text.append("timerEnd=").append(this.countdown.endsAt()).append('\n');
      } else {
         text.append("timerLeft=").append(this.countdown.left()).append('\n');
      }
      if (this.countdown.isDone()) {
         text.append("timerDone=").append(this.countdown.finishedAt()).append('\n');
      }
      if (this.stopwatch.isRunning()) {
         text.append("watchStart=").append(this.stopwatch.startedAt()).append('\n');
      }
      text.append("watchBanked=").append(this.stopwatch.banked()).append('\n');
      List<String> laps = new ArrayList<>();
      for (int i = 0; i < this.stopwatch.lapCount(); i++) {
         laps.add(String.valueOf(this.stopwatch.lapEnd(i)));
      }
      text.append("laps=").append(String.join(",", laps)).append('\n');
      return text.toString();
   }
}
