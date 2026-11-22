package flappyBird;

import java.util.ArrayList;
import java.util.List;

/** Self-check for the flight guide: the paths ahead with and without a flap, and where they crash. Run with java -ea. */
public final class GuideCheck {
   public static void main(String[] args) {
      if (!GuideCheck.class.desiredAssertionStatus()) {
         throw new IllegalStateException("Run with -ea");
      }
      Bird bird = new Bird(null);
      bird.position.set(new Position(bird.position.collisionX, Main.FRAME_HEIGHT / 3.0F));
      float[] before = bird.remember();
      List<float[][]> none = new ArrayList<>();
      float ground = Main.FRAME_HEIGHT * 2.0F;

      List<float[]> fall = Guide.path(bird, false, 60, 6, none, ground);
      List<float[]> flap = Guide.path(bird, true, 60, 6, none, ground);
      assert java.util.Arrays.equals(before, bird.remember()) : "the bird is left as it was";
      assert fall.size() == 10 && flap.size() == 10 && !Guide.crashes(fall) : "a dot every 6 steps, no crash in the open";
      assert fall.get(9)[1] > fall.get(0)[1] : "without a flap the bird falls";
      assert flap.get(0)[1] < fall.get(0)[1] : "a flap lifts it first";
      assert fall.get(9)[0] - fall.get(0)[0] > Pipes.scrollSpeed * 50 : "the path runs ahead as the pipes come in";

      // The path the bird flies matches the bird flying it: the 6th step's body is the first dot, moved ahead.
      for (int i = 0; i < 6; i++) {
         bird.update();
      }
      float[] body = Hitbox.body(bird.position.collisionX + Pipes.scrollSpeed * 6, bird.position.collisionY, Bird.WIDTH, bird.rotation());
      assert Math.abs(body[0] - fall.get(0)[0]) < 0.01F && Math.abs(body[1] - fall.get(0)[1]) < 0.01F : "the first dot is where the bird goes";
      bird.recall(before);

      // A bottom pipe just ahead and below: the falling path ends in a crash on it.
      float x = bird.position.collisionX + Bird.WIDTH;
      float y = fall.get(5)[1];
      List<float[][]> pipe = new ArrayList<>();
      pipe.add(Hitbox.pipe(x, y, Pipes.PIPE_WIDTH, Pipes.PIPE_HEIGHT, false));
      List<float[]> into = Guide.path(bird, false, 60, 6, pipe, ground);
      assert Guide.crashes(into) && into.size() < fall.size() : "the path stops at the crash";
      float[] end = into.get(into.size() - 1);
      assert Hitbox.touch(new float[]{end[0], end[1], Hitbox.BODY_R * Bird.WIDTH}, pipe.get(0)) != null : "the crash ring sits on the pipe";

      // The ground counts too.
      assert Guide.crashes(Guide.path(bird, false, 600, 6, none, bird.position.collisionY + Bird.HEIGHT + 20)) : "the ground is a crash";
      // A pair's opening leaves room for the flap on any window: a flap's rise, measured in bird heights (the
      // physics scale with the bird), fits 2.5 times in an upright phone's gap; a wide window keeps two pipe widths.
      bird.recall(before);
      bird.flap();
      float start = bird.position.collisionY;
      float top = start;
      for (int i = 0; i < 200; i++) {
         bird.update();
         top = Math.min(top, bird.position.collisionY);
      }
      bird.recall(before);
      float rise = (start - top) / Bird.HEIGHT;
      assert rise > 1.5F && rise < 2.0F : "a flap rises about 1.75 bird heights, was " + rise;
      int phoneBird = 525 / 12;
      assert Pipes.gapHeight(300 / 8, phoneBird) >= 2.5F * rise * phoneBird : "an upright phone's gap fits the flap";
      assert Pipes.gapHeight(675 / 8, 405 / 12) == 2 * (675 / 8) : "a wide window's gap is two pipe widths, as before";
      assert Pipes.gapHeight(1440 / 8, 810 / 12) == 2 * (1440 / 8) : "a desktop window's gap is unchanged";
      System.out.println("GuideCheck passed");
   }
}
