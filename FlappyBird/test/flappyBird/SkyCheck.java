package flappyBird;

import java.util.Arrays;

/** Checks the time of day: sunrise as drawn, the blend between times, the wrap after night, and the stars. */
public final class SkyCheck {
   public static void main(String[] args) {
      int[] sunrise = Sky.at(0.0F);
      assert Arrays.equals(sunrise, new int[13]) : "sunrise is the art as drawn: " + Arrays.toString(sunrise);
      assert Arrays.equals(Sky.at(4.0F), sunrise) : "40 pipes is sunrise again";
      assert Arrays.equals(Sky.at(-1.0F), Sky.at(3.0F)) : "phases wrap both ways";
      assert Sky.at(3.0F)[12] == 255 : "night has bright stars";
      assert Sky.at(1.0F)[12] == 0 && Sky.at(2.0F)[12] == 0 : "no stars by day";
      int[] dawnToNoon = Sky.at(0.5F);
      assert dawnToNoon[3] == 40 : "half way to noon is half its wash: " + dawnToNoon[3];
      assert dawnToNoon[0] == 50 && dawnToNoon[1] == 130 && dawnToNoon[2] == 230 : "fading in from clear keeps noon's color, not black: " + Arrays.toString(dawnToNoon);
      int stars = Sky.at(2.5F)[12];
      assert stars > 120 && stars < 135 : "stars come out through dusk: " + stars;
      assert Sky.at(3.5F)[11] > 0 && Sky.at(3.5F)[11] < Sky.at(3.0F)[11] : "the dimming lifts toward dawn";
      System.out.println("SkyCheck passed");
   }
}
