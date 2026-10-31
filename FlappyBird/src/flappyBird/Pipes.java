package flappyBird;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Random;

public final class Pipes {
   public static final int PIPE_HEIGHT = Main.FRAME_HEIGHT;
   public static final int PIPE_WIDTH = Main.FRAME_WIDTH / 8;
   public static int currentPipeIndex = 0;
   public Image topPipeImage;
   public Image bottomPipeImage;
   public ArrayList positions = new ArrayList();
   private int gapHeight = 2 * PIPE_WIDTH;
   private int pipeSpacing = (int)((double)Main.FRAME_WIDTH / 3.5);
   private Random random = new Random();
   private ArrayList initialIndices = new ArrayList();
   public static float scrollSpeed = (float)(Main.FRAME_WIDTH / 500);

   public Pipes(Image topPipeImage, Image bottomPipeImage) {
      this.topPipeImage = topPipeImage;
      this.bottomPipeImage = bottomPipeImage;

      for (int pair = 0; pair < 1000; pair++) {
         int gapTop = this.random.nextInt(PIPE_HEIGHT - this.gapHeight - (Main.FRAME_HEIGHT - Background.groundY));
         this.positions.add(new Position((float)(Main.FRAME_WIDTH + this.pipeSpacing * pair), (float)(gapTop - PIPE_HEIGHT)));
         this.positions.add(new Position((float)(Main.FRAME_WIDTH + this.pipeSpacing * pair), (float)(gapTop + this.gapHeight)));
         if (pair < 4) {
            this.initialIndices.add(pair);
         }
      }

      this.scroll();
   }

   public final void scroll() {
      int offset = 0;

      for (int i = 0; i < this.positions.size(); i++) {
         this.positions.set(i, new Position(((Position)this.positions.get(i)).x - scrollSpeed, ((Position)this.positions.get(i)).y));
         if (((Position)this.positions.get(i)).collisionX > 0.0F && offset == 0) {
            while (++offset < 4) {
               currentPipeIndex = i + offset;
            }
         }
      }
   }
}
