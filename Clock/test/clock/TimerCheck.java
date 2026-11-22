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

      // Focus sessions: 25 minutes, a 5-minute break that starts by itself, then the next session waits for Start.
      long t0 = Focus.noon(System.currentTimeMillis()) - 6 * 60 * M; // 6 am today, so nothing crosses midnight
      int today = Focus.day(t0);
      Focus focus = new Focus();
      check(!focus.isRunning() && focus.remaining(t0) == 25 * M && focus.phase() == Focus.FOCUS, "focus starts ready at 25");
      focus.toggle(t0);
      check(focus.update(t0 + 25 * M - 1) == Focus.NONE, "not done a millisecond early");
      check(focus.update(t0 + 25 * M) == Focus.FOCUS_DONE && focus.eventAt() == t0 + 25 * M, "focus done on time");
      check(focus.phase() == Focus.SHORT && focus.isRunning() && focus.remaining(t0 + 26 * M) == 4 * M, "the break starts by itself");
      check(focus.minutesOn(today) == 25 && focus.sessionsOn(today) == 1 && focus.done() == 1, "the session is logged");
      check(focus.update(t0 + 30 * M) == Focus.BREAK_DONE && focus.phase() == Focus.FOCUS && !focus.isRunning(),
            "after the break the next session waits");
      check(focus.update(t0 + 90 * M) == Focus.NONE, "nothing runs while waiting");
      // Three more sessions, caught up in one late tick each (as if the window was closed): the fourth earns a long break.
      for (int i = 0; i < 3; i++) {
         long start = t0 + (40 + 40 * i) * M;
         focus.toggle(start);
         check(focus.update(start + 35 * M) == Focus.BREAK_DONE && focus.eventAt() == start + 30 * M || i == 2,
               "a session and its break caught up together");
      }
      check(focus.phase() == Focus.LONG && focus.done() == 4 && focus.remaining(t0 + 145 * M) == 15 * M,
            "the fourth session earns a 15-minute long break");
      check(focus.sessionsOn(today) == 4 && focus.minutesOn(today) == 100, "four sessions logged");
      focus.skip(t0 + 160 * M);
      check(focus.phase() == Focus.FOCUS && focus.done() == 0 && focus.isRunning(), "skipping the long break starts a new set");
      focus.skip(t0 + 170 * M);
      check(focus.minutesOn(today) == 110 && focus.sessionsOn(today) == 4 && focus.phase() == Focus.SHORT,
            "a skipped session logs its minutes but is not a session");
      focus.reset();
      focus.nextPlan();
      check(focus.remaining(0) == 50 * M && focus.length(Focus.SHORT) == 10 * M && focus.length(Focus.LONG) == 30 * M,
            "the next plan is 50 / 10");
      focus.toggle(t0 + 200 * M);
      focus.toggle(t0 + 210 * M);
      check(focus.isPaused() && focus.remaining(t0 + 999 * M) == 40 * M, "a paused session keeps its time");
      for (int d = 0; d < 20; d++) {
         focus.restoreDay(20000101 + d, 5, 1);
      }
      check(focus.log().size() == Focus.DAYS, "two weeks of days are kept");
      check(FocusView.amount(45).equals("45 min") && FocusView.amount(75).equals("1 h 15 min") && FocusView.amount(120).equals("2 h"),
            "focus amounts");

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
      state.focus.nextPlan();
      state.focus.toggle(t0);
      state.focus.restoreDay(today, 75, 3);
      state.save();
      ClockState back = ClockState.load(file);
      check(back.text().equals(state.text()), "state round-trips:\n" + state.text() + "\nvs\n" + back.text());
      check(back.countdown.isRunning() && back.countdown.remaining(5000 + M) == 24 * M, "the timer keeps running while closed");
      check(back.countdown.update(5000 + 26 * M) && back.countdown.finishedAt() == 5000 + 25 * M,
            "a timer that ran out while closed rings at its own time");
      check(back.stopwatch.isRunning() && back.stopwatch.lapCount() == 1, "the stopwatch keeps running");
      check(back.focus.isRunning() && back.focus.plan() == 1 && back.focus.minutesOn(today) == 75, "the focus session and log come back");
      check(back.focus.update(t0 + 50 * M) == Focus.FOCUS_DONE && back.focus.minutesOn(today) == 125,
            "a session that ended while closed is logged");

      java.nio.file.Files.write(file.toPath(), Arrays.asList("mode=nonsense", "timer=abc", "laps=1,x", "face=digital"));
      ClockState damaged = ClockState.load(file);
      check(damaged.mode.equals(ClockState.CLOCK) && !damaged.analog && damaged.countdown.length() == 5 * M,
            "a damaged file keeps the defaults for its bad lines");
      check(ClockState.load(new File(file.getPath() + ".missing")).mode.equals(ClockState.CLOCK), "a missing file gives defaults");
      state.flip = true;
      state.save();
      back = ClockState.load(file);
      check(!back.analog && back.flip && back.text().contains("face=flip\n"), "the flip clock face is remembered");

      // The flip clock: its six figures, and a flap falling from standing up, over the hinge, to lying down.
      check(new String(FlipFace.figures(9, 5, 7, false)).equals(" 90507"), "12-hour flip cards blank the hour's leading zero");
      check(new String(FlipFace.figures(0, 30, 59, false)).equals("123059"), "midnight is 12 on the flip cards");
      check(new String(FlipFace.figures(9, 5, 7, true)).equals("090507") && new String(FlipFace.figures(23, 59, 0, true)).equals("235900"),
            "24-hour flip cards keep both hour figures");
      check(FlipFace.fold(0, 500) == 1 && FlipFace.fold(500, 500) == -1 && FlipFace.fold(9999, 500) == -1 && FlipFace.fold(-5, 500) == 1,
            "a flap starts standing and ends lying down");
      check(FlipFace.fold(250, 500) > 0 && FlipFace.fold(400, 500) < 0, "a flap falls faster as it goes (edge on after half its time)");

      // The LED face's seven segments: a (top) to g (middle) as bits 0 to 6, each figure its own shape.
      int[] litCounts = {6, 2, 5, 5, 4, 5, 6, 3, 7, 6};
      java.util.Set<Integer> shapes = new java.util.HashSet<>();
      for (int figure = 0; figure < 10; figure++) {
         int lit = DigitalFace.segments((char)('0' + figure));
         check(Integer.bitCount(lit) == litCounts[figure] && lit < 0x80, figure + " lights " + litCounts[figure] + " segments");
         shapes.add(lit);
      }
      check(shapes.size() == 10, "every figure has its own shape");
      check(DigitalFace.segments('1') == 0x06 && DigitalFace.segments('7') == 0x07 && (DigitalFace.segments('0') & 0x40) == 0,
            "1 is the right side, 7 adds the top, 0 has no middle bar");
      check(DigitalFace.segments('-') == 0x40 && DigitalFace.segments(' ') == 0, "a dash is the middle bar, a blank lights nothing");

      // The analog dial: an hour tick at 12, bare face between the minute ticks, the hours' numbers, the knob's circle clear.
      java.awt.image.BufferedImage dial = new java.awt.image.BufferedImage(400, 400, java.awt.image.BufferedImage.TYPE_INT_RGB);
      java.awt.Graphics2D dialInk = dial.createGraphics();
      dialInk.setColor(Ui.FACE);
      dialInk.fillRect(0, 0, 400, 400);
      ClockFace.paintDial(dialInk, 200, 200, 190, 8);
      dialInk.dispose();
      check(dial.getRGB(200, 200 - 172) != Ui.FACE.getRGB(), "an hour tick stands at 12");
      check(dial.getRGB(200 + (int)(176 * Math.sin(Math.PI / 60)), 200 - (int)(176 * Math.cos(Math.PI / 60))) == Ui.FACE.getRGB(),
            "the face is bare between two minute ticks");
      check(inked(dial, 200 + 141, 200 - 12, 200 + 141 + 24, 200 + 12) && inked(dial, 200 - 12, 200 + 132, 200 + 12, 200 + 156),
            "the numbers 3 and 6 are printed on the dial");
      check(!inked(dial, 200 - 30, 200 - 30, 200 + 30, 200 + 30), "the middle of the dial stays clear for the hands");
      for (int hour = 0; hour < 12; hour++) {
         double knob = hour * Math.PI / 6;
         int knobX = 200 + (int)Math.round(190 * 0.62 * Math.sin(knob));
         int knobY = 200 - (int)Math.round(190 * 0.62 * Math.cos(knob));
         check(!inked(dial, knobX - 13, knobY - 13, knobX + 13, knobY + 13), "the alarm knob at " + hour + " o'clock is clear of the numbers");
      }

      // The Westminster quarters: one change at quarter past, two at half past, three at quarter to, four and the hour.
      int[] quarterPast = Westminster.steps(9, 1);
      check(Arrays.equals(quarterPast, new int[]{68, 66, 64, 59, Westminster.REST, Westminster.HUSH}), "quarter past rings the first change");
      check(Westminster.steps(9, 2).length == 12 && Westminster.steps(9, 3).length == 18, "half past two changes, quarter to three");
      check(Arrays.equals(Arrays.copyOfRange(Westminster.steps(9, 3), 12, 18), quarterPast), "quarter to ends on the first change");
      for (int hour = 0; hour < 24; hour++) {
         int strokes = 0;
         for (int step : Westminster.steps(hour, 0)) {
            strokes += step == Westminster.HOUR_BELL ? 1 : 0;
         }
         check(strokes == (hour % 12 == 0 ? 12 : hour % 12), "the hour struck at " + hour);
         check(Westminster.steps(hour, 0)[0] == 64 && Westminster.steps(hour, 0)[18] == 59, "the hour opens with changes 2 to 5");
      }
      state.chimes = true;
      state.save();
      check(ClockState.load(file).chimes && !damaged.chimes, "the chimes are remembered, off by default");

      // The alarm: every day at its time, once, with a 9-minute snooze; one missed while closed or asleep does not ring.
      long six = t0; // 6 am today
      Alarm alarm = new Alarm();
      check(!alarm.isOn() && !alarm.update(six) && !alarm.quiet(six), "the alarm starts off");
      alarm.set(6 * 60 + 30, six);
      check(alarm.nextAt(six) == six + 30 * M, "set at 6 for 6:30 rings in half an hour");
      check(Alarm.next(6 * 60, six) > six + 22 * 60 * M, "a time just gone is tomorrow");
      check(!alarm.update(six + 30 * M - 1) && alarm.update(six + 30 * M) && alarm.isRinging(), "rings on time");
      check(!alarm.update(six + 31 * M), "rings once");
      alarm.snooze(six + 31 * M);
      check(!alarm.isRinging() && alarm.nextAt(six + 31 * M) == six + 40 * M, "snooze rings again 9 minutes later");
      check(alarm.update(six + 40 * M) && !alarm.update(six + 41 * M), "the snooze rings once");
      alarm.stop();
      check(!alarm.isRinging() && alarm.nextAt(six + 41 * M) > six + 23 * 60 * M, "stopped, it rings again tomorrow");
      check(alarm.quiet(alarm.nextAt(six) - 60 * M) && !alarm.quiet(alarm.nextAt(six) - 9 * 60 * M),
            "the chimes keep quiet for eight hours before it");
      long tomorrow = alarm.nextAt(six);
      check(!alarm.update(tomorrow + 11 * M) && alarm.nextAt(tomorrow + 11 * M) > tomorrow + 23 * 60 * M,
            "found more than 10 minutes late, it is skipped");
      // The hand points at a 12-hour dial: it means the next time that hour comes round.
      alarm.setFromDial(7 * 60, 6 * 60, six);
      check(alarm.minute() == 7 * 60, "at 6 am, the hand at 7 means 7 am");
      alarm.setFromDial(5 * 60, 6 * 60, six);
      check(alarm.minute() == 17 * 60, "at 6 am, the hand at 5 means 5 pm");
      alarm.setFromDial(6 * 60, 6 * 60, six);
      check(alarm.minute() == 18 * 60, "the hand on now means 12 hours away");
      alarm.set(-5, six);
      check(alarm.minute() == 23 * 60 + 55, "Down past midnight wraps round");
      alarm.toggle(six);
      check(!alarm.isOn() && alarm.nextAt(six) == 0 && !alarm.quiet(six), "off, it never rings");
      check(Alarm.wait(six + 8 * 60 * M + 5 * M - 1, six).equals("8 h 5 min") && Alarm.wait(six + 45 * M, six).equals("45 min"), "waits");
      // Saved: the time, on or off, and a waiting snooze; a newly opened window does not ring one missed while closed.
      state.alarm.set(6 * 60 + 45, six);
      state.alarm.snooze(six);
      state.save();
      ClockState woke = ClockState.load(file);
      check(woke.alarm.isOn() && woke.alarm.minute() == 6 * 60 + 45 && woke.alarm.snoozeUntil() == six + 9 * M, "the alarm round-trips");
      check(woke.text().equals(state.text()), "the alarm lines round-trip");
      ClockState late = ClockState.load(file);
      check(!late.alarm.update(six + 2 * 60 * M) && late.alarm.nextAt(six + 2 * 60 * M) > six + 2 * 60 * M,
            "an alarm missed while closed does not ring when the window opens");
      java.nio.file.Files.write(file.toPath(), Arrays.asList("alarm=9999", "alarmOn=on"));
      check(ClockState.load(file).alarm.minute() == 7 * 60 && !damaged.alarm.isOn(), "a bad alarm time keeps 7 am, off by default");

      System.out.println("TimerCheck passed");
   }

   /** Whether any pixel in the box is not the white face. */
   private static boolean inked(java.awt.image.BufferedImage image, int left, int top, int right, int bottom) {
      for (int x = left; x <= right; x++) {
         for (int y = top; y <= bottom; y++) {
            if (image.getRGB(x, y) != Ui.FACE.getRGB()) {
               return true;
            }
         }
      }
      return false;
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
