package circlewindow;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;

/** Checks Spin: flat face on, mirrored and darker from behind, edge-on thin, perspective, and a whole turn. */
public final class SpinCheck {
   public static void main(String[] args) {
      // Two windows: a tall one on the left, a short one on the right, the shape 200 wide centered on x = 200.
      List<Rectangle> home = Arrays.asList(new Rectangle(100, 100, 40, 200), new Rectangle(260, 150, 40, 100));
      Color red = new Color(200, 100, 50);
      Spin spin = new Spin(home, Arrays.asList(red, red));

      assert spin.at(0, 0).equals(home.get(0)) && spin.at(1, 0).equals(home.get(1)) : "face on is the shape as built";
      assert spin.colorAt(0, 0).equals(red) : "face on keeps its color";

      Rectangle back = spin.at(0, Math.PI);
      assert back.equals(new Rectangle(260, 100, 40, 200)) : "from behind the left window is on the right: " + back;
      Color dark = spin.colorAt(0, Math.PI);
      assert dark.getRed() < red.getRed() && dark.getRed() * 2 > red.getRed() : "the back is darker, not black";

      Rectangle edge = spin.at(0, Math.PI / 2);
      assert edge.width <= 2 && Math.abs(edge.x - 200) <= 1 : "edge on, a window is a thin line at the middle: " + edge;
      assert spin.colorAt(0, Math.PI / 2).getRed() < spin.colorAt(0, Math.PI / 4).getRed() : "edge on is dimmer";

      // An eighth of a turn swings the right side toward the eye: it looks taller than it is, the left side shorter.
      double turn = Math.PI / 4;
      assert spin.at(1, turn).height > 100 && spin.at(0, turn).height < 200 : "the near side looks bigger";
      assert spin.at(0, -turn).height > 200 : "and turning the other way, the other side";

      assert Spin.angle(Spin.TURN_MILLIS / 2) == Math.PI : "half the time, half a turn";
      assert Spin.angle(Spin.TURN_MILLIS) == 0 && spin.at(1, Spin.angle(3 * Spin.TURN_MILLIS)).equals(home.get(1))
         : "a whole turn comes back to the start";
      assert spin.home(0).equals(home.get(0)) && spin.color(1).equals(red) : "the flat places are kept";
      System.out.println("SpinCheck passed");
   }
}
