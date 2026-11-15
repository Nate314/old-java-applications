package groupcreator;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Self-check for the work time: its clock text, the countdown, the one ring, a minute more or less, and the class file. */
public class WorkTimerCheck {
   public static void main(String[] args) throws Exception {
      check(WorkTimer.clock(600_000).equals("10:00"), "ten minutes");
      check(WorkTimer.clock(65_001).equals("1:06"), "seconds round up");
      check(WorkTimer.clock(1).equals("0:01") && WorkTimer.clock(0).equals("0:00"), "0:00 only when up");

      WorkTimer timer = new WorkTimer();
      check(!timer.running() && timer.left(0) == 600_000 && timer.gone(0) == 0, "idle shows the whole 10 minutes");
      timer.start(1000);
      check(timer.running() && timer.left(1000 + 240_000) == 360_000, "counts down");
      check(Math.abs(timer.gone(1000 + 300_000) - 0.5f) < 1e-6, "half gone at five minutes");
      check(!timer.up(1000 + 599_999) && !timer.ring(1000 + 599_999), "not up a moment early");
      check(timer.ring(1000 + 600_000) && !timer.ring(1000 + 700_000), "rings once when up");
      check(timer.left(1000 + 700_000) == 0 && timer.up(1000 + 700_000), "stays up at 0:00");

      // A minute more after it was up starts a minute from now, and rings again; the length follows.
      check(timer.adjust(1, 1000 + 700_000), "a minute more");
      check(timer.left(1000 + 700_000) == 60_000 && timer.minutes == 11, "one minute left, length 11");
      check(timer.ring(1000 + 760_000), "rings again");
      // A minute less never goes below zero.
      timer.start(0);
      timer.adjust(-1, 30_000);
      timer.adjust(-20, 30_000);
      check(timer.up(30_000) && timer.left(30_000) == 0, "minus stops at 0:00");
      timer.stop();
      check(!timer.running(), "stops");

      // Idle, the length stays between 1 and 60 minutes.
      timer.minutes = 1;
      check(!timer.adjust(-1, 0) && timer.minutes == 1, "not under a minute");
      timer.minutes = 60;
      check(!timer.adjust(1, 0) && timer.minutes == 60, "not over an hour");
      check(timer.adjust(-1, 0) && timer.minutes == 59 && timer.left(0) == 59 * 60_000L, "idle length changes");

      // The class file keeps a length other than 10 minutes, and skips a bad one.
      File file = File.createTempFile("groupcreator", ".txt");
      file.deleteOnExit();
      Roster roster = Roster.load(file);
      check(roster.work.minutes == 10 && !roster.text().contains("work="), "10 minutes by default, no line");
      roster.work.minutes = 15;
      roster.save();
      check(Roster.load(file).work.minutes == 15, "work=15 round trips");
      Files.write(file.toPath(), "work=abc\nnames=a,b\n".getBytes(StandardCharsets.UTF_8));
      check(Roster.load(file).work.minutes == 10, "a bad length keeps 10");
      Files.write(file.toPath(), "work=500\n".getBytes(StandardCharsets.UTF_8));
      check(Roster.load(file).work.minutes == 60, "a long length is cut to an hour");

      System.out.println("WorkTimerCheck passed");
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
