package rubikscube;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Checks the speed timer: its scrambles, Space held then let go, the time format, the averages and the saved times. */
public final class TimerCheck {
   public static void main(String[] args) throws Exception {
      Random random = new Random(7);
      for (int n = 0; n < 2000; n++) {
         String scramble = SolveTimer.scramble(random);
         String[] tokens = scramble.split(" ");
         assert tokens.length == SolveTimer.SCRAMBLE_TURNS : scramble;
         for (int i = 0; i < tokens.length; i++) {
            assert tokens[i].matches("[UDFBRL]['2]?") : scramble;
            assert i == 0 || tokens[i].charAt(0) != tokens[i - 1].charAt(0) : "same face twice: " + scramble;
            assert i < 2 || !(axis(tokens[i]) == axis(tokens[i - 1]) && axis(tokens[i]) == axis(tokens[i - 2]))
                  : "three on one axis: " + scramble;
         }
         List<Integer> turns = Patterns.parse(scramble);
         assert turns.size() >= 20 && turns.size() <= 40 : scramble;
         assert !Arrays.equals(Patterns.apply(Patterns.solved(), turns), Patterns.solved()) : "scrambles nothing: " + scramble;
      }

      SolveTimer timer = new SolveTimer();
      assert timer.state() == SolveTimer.State.IDLE && timer.shown(0) == -1;
      // A tap shorter than the hold does not start it.
      assert !timer.press(1000);
      assert !timer.ready(1000 + SolveTimer.HOLD_MILLIS - 1);
      timer.release(1000 + SolveTimer.HOLD_MILLIS - 1);
      assert timer.state() == SolveTimer.State.IDLE;
      // Held long enough: green, and letting go starts it.
      assert !timer.press(2000);
      assert !timer.press(2100) : "a key's repeat is not a second press";
      assert timer.ready(2000 + SolveTimer.HOLD_MILLIS);
      timer.release(2400);
      assert timer.state() == SolveTimer.State.RUNNING && timer.shown(3400) == 1000;
      // Pressing stops it, and letting go again does not start another.
      assert timer.press(14745);
      assert timer.last() == 12345 && timer.shown(99999) == 12345 && timer.state() == SolveTimer.State.IDLE;
      timer.release(15745);
      assert timer.state() == SolveTimer.State.IDLE;
      timer.press(20000);
      timer.cancel();
      assert timer.state() == SolveTimer.State.IDLE && !timer.press(30000) && timer.state() == SolveTimer.State.HOLDING : "cancel lets go of the key";

      assert SolveTimer.format(0).equals("0.00");
      assert SolveTimer.format(9879).equals("9.87") : "hundredths are cut off, not rounded";
      assert SolveTimer.format(12305).equals("12.30");
      assert SolveTimer.format(62359).equals("1:02.35");
      assert SolveTimer.format(600000).equals("10:00.00");

      List<Long> times = Arrays.asList(10000L, 30000L, 12000L, 11000L, 13000L);
      assert SolveTimer.average(times, 5) == 12000 : "best and worst left out";
      assert SolveTimer.average(times, 12) == -1;
      assert SolveTimer.best(times) == 10000 && SolveTimer.best(Arrays.asList()) == -1;

      File file = File.createTempFile("rubikscube-timer", ".txt");
      file.deleteOnExit();
      LevelRecords records = LevelRecords.load(file);
      records.record(3, 2, 7);
      for (long t = 1; t <= LevelRecords.TIMES_KEPT + 5; t++) {
         records.addTime(t * 1000);
      }
      assert records.times().size() == LevelRecords.TIMES_KEPT && records.times().get(0) == 6000 : "keeps the latest";
      records.dropLastTime();
      LevelRecords again = LevelRecords.load(file);
      assert again.times().equals(records.times()) && again.times().size() == LevelRecords.TIMES_KEPT - 1;
      assert again.times().get(again.times().size() - 1) == (LevelRecords.TIMES_KEPT + 4) * 1000L;
      assert again.stars(3) == 2 && again.moves(3) == 7 : "times sit beside the level records";
      System.out.println("TimerCheck passed");
   }

   private static int axis(String token) {
      return "UDFBRL".indexOf(token.charAt(0)) / 2;
   }
}
