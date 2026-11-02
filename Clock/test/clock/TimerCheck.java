package clock;

import java.io.File;
import java.util.Arrays;

/** Self-check for the countdown, the stopwatch, the time formats and the state file. Run with java -ea. */
public class TimerCheck {
   private static final long M = Countdown.MINUTE;

   public static void main(String[] args) throws Exception {
      // Counting down against the wall clock: start, pause, resume, run out once.
      Countdown timer = new Countdown();
      timer.set(3 * M, 1000);
      check(!timer.isRunning() && timer.remaining(1000) == 3 * M, "set leaves it ready");
      timer.toggle(1000);
      check(timer.isRunning() && timer.remaining(61_000) == 2 * M, "a minute later two are left");
      timer.toggle(61_000);
      check(timer.isPaused() && timer.remaining(999_999) == 2 * M, "paused time does not pass");
      timer.toggle(100_000);
      check(!timer.update(100_000 + 2 * M - 1), "not done a millisecond early");
      check(timer.update(100_000 + 2 * M + 5), "done on time");
      check(!timer.update(100_000 + 3 * M), "done only once");
      check(timer.isDone() && timer.finishedAt() == 100_000 + 2 * M, "remembers when it rang");
      timer.toggle(0);
      check(!timer.isDone() && timer.remaining(0) == 3 * M, "dismissing makes it ready at the same length");

      // The dial's limits, and Up and Down by whole minutes.
      timer.set(90 * M, 0);
      check(timer.length() == Countdown.MAX, "capped at an hour");
      timer.set(-5, 0);
      check(timer.length() == 0, "never negative");
      timer.toggle(0);
      check(!timer.isRunning(), "an empty timer does not start");
      timer.set(5 * M, 0);
      timer.toggle(0);
      timer.addMinutes(1, 20_000);
      check(timer.isRunning() && timer.remaining(20_000) == 6 * M, "Up while running: 4:40 rounds to 5, plus 1");
      timer.addMinutes(-10, 20_000);
      check(timer.length() == 0, "Down stops at zero");

      // Stopwatch laps.
      Stopwatch watch = new Stopwatch();
      watch.toggle(10_000);
      watch.lap(13_000);
      watch.lap(14_000);
      watch.toggle(15_000);
      watch.toggle(100_000);
      watch.lap(105_000);
      check(watch.elapsed(105_000) == 10_000, "stopped time does not count");
      check(watch.lapTime(0) == 3000 && watch.lapTime(1) == 1000 && watch.lapTime(2) == 6000, "lap times");
      check(watch.extremeLap(false) == 1 && watch.extremeLap(true) == 2, "fastest and slowest laps");
      watch.reset();
      check(watch.lapCount() == 3, "no reset while running");
      watch.toggle(106_000);
      watch.reset();
      check(watch.lapCount() == 0 && watch.elapsed(200_000) == 0, "reset once stopped");

      // Formats.
      check(Ui.countdown(299_001).equals("5:00") && Ui.countdown(299_000).equals("4:59"), "countdown rounds up");
      check(Ui.countdown(Countdown.MAX).equals("60:00"), "a full hour");
      check(Ui.stopwatch(7_429).equals("0:07.42"), "stopwatch hundredths");
      check(Ui.stopwatch(3_723_040).equals("1:02:03.04"), "stopwatch hours");

      // The state file round-trips a running timer and a stopwatch with laps, and survives damage.
      File file = File.createTempFile("clock", ".txt");
      file.deleteOnExit();
      ClockState state = new ClockState(file);
      state.mode = ClockState.STOPWATCH;
      state.analog = false;
      state.twentyFour = true;
      state.countdown.set(25 * M, 0);
      state.countdown.toggle(5000);
      state.stopwatch.toggle(1000);
      state.stopwatch.lap(4000);
      state.save();
      ClockState back = ClockState.load(file);
      check(back.text().equals(state.text()), "state round-trips:\n" + state.text() + "\nvs\n" + back.text());
      check(back.countdown.isRunning() && back.countdown.remaining(5000 + M) == 24 * M, "the timer keeps running while closed");
      check(back.countdown.update(5000 + 26 * M) && back.countdown.finishedAt() == 5000 + 25 * M,
            "a timer that ran out while closed rings at its own time");
      check(back.stopwatch.isRunning() && back.stopwatch.lapCount() == 1, "the stopwatch keeps running");

      java.nio.file.Files.write(file.toPath(), Arrays.asList("mode=nonsense", "timer=abc", "laps=1,x", "face=digital"));
      ClockState damaged = ClockState.load(file);
      check(damaged.mode.equals(ClockState.CLOCK) && !damaged.analog && damaged.countdown.length() == 5 * M,
            "a damaged file keeps the defaults for its bad lines");
      check(ClockState.load(new File(file.getPath() + ".missing")).mode.equals(ClockState.CLOCK), "a missing file gives defaults");

      System.out.println("TimerCheck passed");
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
