package flappyBird;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/** Self-check for today's course, its ghost and the medals. Run with java -ea. */
public final class CourseCheck {
   public static void main(String[] args) throws Exception {
      if (!CourseCheck.class.desiredAssertionStatus()) {
         throw new IllegalStateException("Run with -ea");
      }

      // The same day gives the same pipes, another day different ones.
      DailyCourse today = new DailyCourse(20261101);
      assert pipes(today).equals(pipes(new DailyCourse(20261101))) : "same day, same pipes";
      assert !pipes(today).equals(pipes(new DailyCourse(20261102))) : "next day, new pipes";
      assert DailyCourse.today() > 20000000 : "today is yyyymmdd";

      // A run is kept only if it passes more pipes, and the ghost replays it, filling in between samples.
      assert !today.hasGhost() && today.bestScore() == 0;
      assert today.ghostY(0) < 0.0F : "no ghost before a run";
      List<Integer> run = Arrays.asList(300, 340, 380, 360);
      assert today.offer(5, run) && today.bestScore() == 5 && today.hasGhost();
      assert !today.offer(5, Arrays.asList(1, 2)) : "a tie does not replace the ghost";
      assert !today.offer(3, Arrays.asList(1, 2)) : "a worse run does not replace the ghost";
      assert near(today.ghostY(0), 0.300F);
      assert near(today.ghostY(DailyCourse.SAMPLE_STEPS), 0.340F);
      assert near(today.ghostY(DailyCourse.SAMPLE_STEPS + DailyCourse.SAMPLE_STEPS / 2), 0.360F) : "halfway between samples";
      assert near(today.ghostY(3 * DailyCourse.SAMPLE_STEPS - 1), 0.360F + 0.005F);
      assert today.ghostY(3 * DailyCourse.SAMPLE_STEPS) < 0.0F : "the ghost ends with its run";

      // Saved and loaded back the same day; another day starts fresh; a broken file is ignored.
      File file = File.createTempFile("flappybird-today", ".txt");
      file.deleteOnExit();
      today.save(file);
      DailyCourse loaded = DailyCourse.load(file, 20261101);
      assert loaded.bestScore() == 5 && loaded.hasGhost();
      for (int step = 0; step < 3 * DailyCourse.SAMPLE_STEPS + 2; step++) {
         assert loaded.ghostY(step) == today.ghostY(step) : "same ghost at step " + step;
      }
      DailyCourse tomorrow = DailyCourse.load(file, 20261102);
      assert tomorrow.bestScore() == 0 && !tomorrow.hasGhost() : "yesterday's run is not today's";
      assert DailyCourse.load(new File(file.getPath() + ".missing"), 20261101).bestScore() == 0;
      try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
         writer.write("date=20261101\nbest=x\n");
      }
      assert DailyCourse.load(file, 20261101).bestScore() == 0 : "broken file";

      // Medals at 10, 20, 30 and 40 pipes, with the distance to the next.
      assert GamePanel.medalName(9) == null && "Bronze".equals(GamePanel.medalName(10));
      assert "Silver".equals(GamePanel.medalName(29)) && "Gold".equals(GamePanel.medalName(30)) && "Platinum".equals(GamePanel.medalName(55));
      assert GamePanel.medalLine(0).equals("No medal yet: 10 more pipes for Bronze") : GamePanel.medalLine(0);
      assert GamePanel.medalLine(19).equals("Bronze medal: 1 more pipe for Silver") : GamePanel.medalLine(19);
      assert GamePanel.medalLine(40).equals("Platinum medal!");

      System.out.println("CourseCheck passed");
   }

   /** The gaps of the first pipes of a course, after scrolling a few hundred steps. */
   private static String pipes(DailyCourse course) {
      Pipes pipes = new Pipes(null, null, course.pipeRandom());
      for (int step = 0; step < 2000; step++) {
         pipes.scroll();
      }
      StringBuilder gaps = new StringBuilder();
      for (Position pipe : pipes.positions) {
         gaps.append(pipe.y).append(' ');
      }
      return gaps.toString();
   }

   private static boolean near(float a, float b) {
      return Math.abs(a - b) < 0.0001F;
   }
}
