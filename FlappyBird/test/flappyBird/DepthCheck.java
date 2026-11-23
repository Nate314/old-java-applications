package flappyBird;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/** Checks the scenery's depth: further layers scroll slower, each wraps at its tile, a rewind puts them back, and the art keeps its shape. */
public final class DepthCheck {
   public static void main(String[] args) {
      Background background = new Background(null);
      background.recall(new float[Background.PACE.length]);
      for (int i = 0; i < 50; i++) {
         background.scroll();
      }
      float[] at = background.remember();
      for (int i = 1; i < at.length; i++) {
         assert at[i] > at[i - 1] : "layer " + i + " is nearer, so it moves further: " + at[i] + " vs " + at[i - 1];
      }
      assert Math.abs(at[at.length - 1] - 50 * Pipes.scrollSpeed % Main.FRAME_WIDTH) < 0.01F : "the ground keeps the pipes' pace";
      for (int i = 0; i < 20000; i++) {
         background.scroll();
      }
      float[] later = background.remember();
      for (int i = 0; i < later.length; i++) {
         int tile = i == later.length - 1 ? Main.FRAME_WIDTH : Background.TILE;
         assert later[i] >= 0 && later[i] < tile : "layer " + i + " wraps at its tile: " + later[i];
      }
      background.recall(at);
      assert java.util.Arrays.equals(background.remember(), at) : "a rewind puts every layer back";

      // Drawn unscrolled, a cloud sits where the art puts it, scaled by the window's height alone (not stretched to its width).
      background.recall(new float[Background.PACE.length]);
      BufferedImage picture = new BufferedImage(Main.FRAME_WIDTH, Main.FRAME_HEIGHT, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = picture.createGraphics();
      background.paintBack(g);
      g.dispose();
      float s = Main.FRAME_HEIGHT / 1000.0F;
      Color cloud = new Color(picture.getRGB(Math.round(260 * s), Math.round(190 * s)));
      assert cloud.getRed() > 220 && cloud.getGreen() > 220 : "a cloud at (260, 190) of the art: " + cloud;
      Color between = new Color(picture.getRGB(Math.round(520 * s), Math.round(190 * s)));
      assert between.getRed() < 200 : "clear sky between clouds: " + between;
      Color hill = new Color(picture.getRGB(Math.round(480 * s), Math.round(720 * s)));
      assert hill.getGreen() > hill.getRed() + 30 : "the far hill under its curve at (480, 720): " + hill;
      System.out.println("DepthCheck passed");
   }
}
