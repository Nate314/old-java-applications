package flappyBird;

import java.awt.Image;
import java.util.ArrayList;

public final class Background {
   public static int groundY = Main.FRAME_HEIGHT * 7 / 8;
   public Image backgroundImage;
   public Image groundImage;
   public ArrayList layers = new ArrayList();
   private float backgroundSpeed = (float)(Main.FRAME_WIDTH / 1000);

   public Background(Image backgroundImage, Image groundImage) {
      this.backgroundImage = backgroundImage;
      this.groundImage = groundImage;
      ArrayList backgroundTiles = new ArrayList();
      ArrayList groundTiles = new ArrayList();
      backgroundTiles.add(new Position(0.0F, 0.0F));
      backgroundTiles.add(new Position((float)Main.FRAME_WIDTH, 0.0F));
      groundTiles.add(new Position(0.0F, (float)groundY));
      groundTiles.add(new Position((float)Main.FRAME_WIDTH, (float)groundY));
      this.layers.add(backgroundTiles);
      this.layers.add(groundTiles);
      this.scroll();
   }

   public final void scroll() {
      for (int i = 0; i < ((ArrayList)this.layers.get(0)).size(); i++) {
         ((Position)((ArrayList)this.layers.get(0)).get(i)).set(new Position(((Position)((ArrayList)this.layers.get(0)).get(i)).x - this.backgroundSpeed, ((Position)((ArrayList)this.layers.get(0)).get(i)).y));
         if (((Position)((ArrayList)this.layers.get(0)).get(i)).x + (float)Main.FRAME_WIDTH == 0.0F) {
            ((Position)((ArrayList)this.layers.get(0)).get(i)).set(new Position((float)Main.FRAME_WIDTH, 0.0F));
         }
      }

      for (int i = 0; i < ((ArrayList)this.layers.get(1)).size(); i++) {
         ((Position)((ArrayList)this.layers.get(1)).get(i)).set(new Position(((Position)((ArrayList)this.layers.get(1)).get(i)).x - Pipes.scrollSpeed, ((Position)((ArrayList)this.layers.get(1)).get(i)).y));
         if (((Position)((ArrayList)this.layers.get(1)).get(i)).x + (float)Main.FRAME_WIDTH == 0.0F) {
            ((Position)((ArrayList)this.layers.get(1)).get(i)).set(new Position((float)Main.FRAME_WIDTH, (float)groundY));
         }
      }
   }
}
