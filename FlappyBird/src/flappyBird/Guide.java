package flappyBird;

import java.util.ArrayList;
import java.util.List;

/**
 * The flight guide: where the bird's body will be over the next moment if it flaps now, and if it does not, flown
 * ahead with the bird's own physics. The pipes scroll left at a steady speed, so flying ahead against still pipes is
 * the bird moving right by as much.
 */
final class Guide {
   /** The best score at which the guide stops showing by itself (G still brings it back, as practice). */
   static final int UNTIL = 5;

   private Guide() {
   }

   /**
    * The body's centre {x, y} every `every` steps for `steps` steps from now, with a flap first or not. If the body
    * would hit one of `pipes` (shapes from Hitbox.pipe) or the ground first, the path ends there and its last point
    * is {x, y, 1}. The bird is left as it was.
    */
   static List<float[]> path(Bird bird, boolean flap, int steps, int every, List<float[][]> pipes, float groundY) {
      float[] saved = bird.remember();
      List<float[]> path = new ArrayList<>();
      if (flap) {
         bird.flap();
      }
      for (int step = 1; step <= steps; step++) {
         bird.update();
         float ahead = Pipes.scrollSpeed * step;
         float[] body = Hitbox.body(bird.position.collisionX + ahead, bird.position.collisionY, Bird.WIDTH, bird.rotation());
         boolean hit = bird.position.collisionY + Bird.HEIGHT > groundY;
         for (int i = 0; i < pipes.size() && !hit; i++) {
            hit = Hitbox.touch(body, pipes.get(i)) != null;
         }
         if (hit) {
            path.add(new float[]{body[0], body[1], 1.0F});
            break;
         }
         if (step % every == 0) {
            path.add(new float[]{body[0], body[1]});
         }
      }
      bird.recall(saved);
      return path;
   }

   static boolean crashes(List<float[]> path) {
      return !path.isEmpty() && path.get(path.size() - 1).length == 3;
   }
}
