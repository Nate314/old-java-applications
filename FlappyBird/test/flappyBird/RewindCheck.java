package flappyBird;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Self-check for rewinding a crash: the tape, and pipes and bird put back exactly. Run with java -ea. */
public final class RewindCheck {
   public static void main(String[] args) {
      if (!RewindCheck.class.desiredAssertionStatus()) {
         throw new IllegalStateException("Run with -ea");
      }

      // The tape keeps only the latest moments and gives them back newest first, building the path into the crash.
      Rewind tape = new Rewind(3);
      assert tape.size() == 0 && tape.back() == null && tape.path().isEmpty();
      for (int step = 0; step < 5; step++) {
         tape.record(new Rewind.Moment(step, new float[]{step * 10.0F, 0, 0, 0}, new float[]{0, 0}, new float[4]));
      }
      assert tape.size() == 3 : "keeps only its capacity";
      tape.start();
      assert tape.back().steps == 4 && tape.back().steps == 3 && tape.back().steps == 2 : "newest first";
      assert tape.back() == null && tape.size() == 0;
      assert tape.path().size() == 3 && tape.path().get(0)[0] == 4.0F && tape.path().get(0)[1] == 40.0F : "the crash leads the path";
      tape.record(new Rewind.Moment(9, new float[4], new float[]{0, 0}, new float[4]));
      tape.clear();
      assert tape.size() == 0 && tape.path().isEmpty() : "clear forgets the tape and the path";

      // Pipes put back from memory scroll on into the same pipes, including pairs that had not been made yet.
      Pipes pipes = new Pipes(null, null, new Random(7));
      for (int i = 0; i < 200; i++) {
         pipes.scroll();
      }
      float[] memory = pipes.remember();
      int passedThen = pipes.passed;
      List<String> first = scrollAndList(pipes, 3000);
      pipes.recall(memory);
      assert pipes.passed == passedThen : "the score comes back with the pipes";
      List<String> again = scrollAndList(pipes, 3000);
      assert first.equals(again) : "a rewound run meets the same pipes";
      assert pipes.passed > passedThen : "the check scrolled past some pipes";

      // The bird comes back at the same height, speed, tilt and stretch, so it flies on the same way.
      Bird bird = new Bird(null);
      bird.flap();
      bird.update();
      float[] then = bird.remember();
      for (int i = 0; i < 40; i++) {
         bird.update();
      }
      float laterY = bird.position.collisionY;
      float laterTilt = bird.rotation();
      bird.recall(then);
      for (int i = 0; i < 40; i++) {
         bird.update();
      }
      assert bird.position.collisionY == laterY && bird.rotation() == laterTilt : "same flight after a rewind";

      assert GamePanel.rewindCount(1).equals("1 rewind") && GamePanel.rewindCount(3).equals("3 rewinds");
      System.out.println("RewindCheck passed");
   }

   private static List<String> scrollAndList(Pipes pipes, int steps) {
      List<String> seen = new ArrayList<>();
      for (int i = 0; i < steps; i++) {
         pipes.scroll();
         if (i % 50 == 0) {
            StringBuilder line = new StringBuilder();
            for (Position p : pipes.positions) {
               line.append(p.x).append(',').append(p.y).append(' ');
            }
            seen.add(line.append(pipes.passed).toString());
         }
      }
      return seen;
   }
}
