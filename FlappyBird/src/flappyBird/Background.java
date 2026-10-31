package flappyBird;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

public final class Background {
   public static int groundY = Main.FRAME_HEIGHT * 7 / 8;
   public Image backgroundImage;
   public Image groundImage;
   public List<List<Position>> layers = new ArrayList<>();
   private float backgroundSpeed = Main.FRAME_WIDTH / 1000.0F;

   public Background(Image backgroundImage, Image groundImage) {
      this.backgroundImage = backgroundImage;
      this.groundImage = groundImage;
      List<Position> backgroundTiles = new ArrayList<>();
      List<Position> groundTiles = new ArrayList<>();
      backgroundTiles.add(new Position(0.0F, 0.0F));
      backgroundTiles.add(new Position((float)Main.FRAME_WIDTH, 0.0F));
      groundTiles.add(new Position(0.0F, (float)groundY));
      groundTiles.add(new Position((float)Main.FRAME_WIDTH, (float)groundY));
      this.layers.add(backgroundTiles);
      this.layers.add(groundTiles);
      this.scroll();
   }

   public final void scroll() {
      scrollLayer(this.layers.get(0), this.backgroundSpeed);
      scrollLayer(this.layers.get(1), Pipes.scrollSpeed);
   }

   /** Moves each tile left; a tile that has fully left the screen jumps behind the other one. */
   private static void scrollLayer(List<Position> tiles, float speed) {
      for (Position tile : tiles) {
         float x = tile.x - speed;
         if (x <= (float)(-Main.FRAME_WIDTH)) {
            x += (float)(Main.FRAME_WIDTH * tiles.size());
         }
         tile.set(new Position(x, tile.y));
      }
   }
}
