package flappyBird;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pipe pairs, generated endlessly: new pairs appear on the right and pairs that leave the screen are dropped. */
public final class Pipes {
   public static final int PIPE_HEIGHT = Main.FRAME_HEIGHT;
   public static final int PIPE_WIDTH = Main.FRAME_WIDTH / 8;
   public static float scrollSpeed = Main.FRAME_WIDTH / 500.0F;
   public Image topPipeImage;
   public Image bottomPipeImage;
   /** Alternating top and bottom pipe of each on-screen pair: even indexes are top pipes. */
   public List<Position> positions = new ArrayList<>();
   /** Pairs whose right edge has passed the bird: the score. */
   public int passed = 0;
   private int dropped = 0;
   private int gapHeight = 2 * PIPE_WIDTH;
   private int pipeSpacing = (int)((double)Main.FRAME_WIDTH / 3.5);
   private Random random = new Random();

   public Pipes(Image topPipeImage, Image bottomPipeImage) {
      this.topPipeImage = topPipeImage;
      this.bottomPipeImage = bottomPipeImage;
      this.addPair(Main.FRAME_WIDTH);
      this.scroll();
   }

   private void addPair(float x) {
      int gapTop = this.random.nextInt(PIPE_HEIGHT - this.gapHeight - (Main.FRAME_HEIGHT - Background.groundY));
      this.positions.add(new Position(x, (float)(gapTop - PIPE_HEIGHT)));
      this.positions.add(new Position(x, (float)(gapTop + this.gapHeight)));
   }

   public final void scroll() {
      for (Position position : this.positions) {
         position.set(new Position(position.x - scrollSpeed, position.y));
      }

      // Drop pairs that are fully off the left edge; they still count toward the score.
      while (!this.positions.isEmpty() && this.positions.get(0).x + PIPE_WIDTH < 0.0F) {
         this.positions.remove(0);
         this.positions.remove(0);
         this.dropped++;
      }

      int behindBird = 0;
      for (int i = 0; i < this.positions.size(); i += 2) {
         if (this.positions.get(i).x + PIPE_WIDTH < Main.FRAME_WIDTH / 4.0F) {
            behindBird++;
         }
      }
      this.passed = this.dropped + behindBird;

      float lastX = this.positions.get(this.positions.size() - 1).x;
      if (lastX < Main.FRAME_WIDTH + PIPE_WIDTH) {
         this.addPair(lastX + this.pipeSpacing);
      }
   }
}
