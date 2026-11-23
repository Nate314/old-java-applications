package circlewindow;

/** Checks Light: shine at the upper left, shade at the lower right, no seam at the dome's rim, and the picture. */
public final class LightCheck {
   public static void main(String[] args) {
      int[] shine = Light.at(-0.3, -0.35);
      int[] middle = Light.at(0, 0);
      int[] shade = Light.at(0.75, 0.75);
      assert shine[0] > 200 && shine[1] > 60 : "a white shine at the upper left: " + shine[0] + "/" + shine[1];
      assert middle[1] < 60 : "the middle is lit, barely shaded: " + middle[1];
      assert shade[0] < 10 && shade[1] > 90 : "the lower right is in shade: " + shade[0] + "/" + shade[1];
      int[] top = Light.at(-0.7, -0.7);
      assert top[1] < shade[1] / 2 : "the rim facing the light is far lighter than the one turned away";
      for (double a = 0; a < 2 * Math.PI; a += 0.3) {
         int[] in = Light.at(0.9999 * Math.cos(a), 0.9999 * Math.sin(a));
         int[] out = Light.at(1.0001 * Math.cos(a), 1.0001 * Math.sin(a));
         assert Math.abs(in[1] - out[1]) <= 3 : "no seam where the dome meets its rim, at angle " + a;
      }
      int[] corner = Light.at(2 * 63.5 / Light.SIZE - 1, 2 * 63.5 / Light.SIZE - 1);
      int pixel = Light.IMAGE.getRGB(Light.SIZE - 1, Light.SIZE - 1);
      assert pixel >>> 24 == corner[1] && (pixel & 0xff) == corner[0] : "the picture holds the light it was made from";
      System.out.println("LightCheck passed");
   }
}
