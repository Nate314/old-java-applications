package rubikscube;

import java.awt.AWTException;
import java.awt.Robot;

public final class Scrambler {
   private String[] colorNames = new String[]{"blue", "red", "yellow", "green", "orange", "white"};
   private String[] moveKeys = new String[]{"w", "a", "s", "d"};

   Scrambler(int moveCount, double delay, Cube cube) {
      System.out.println("Mix");
      delay = (double)((int)(delay * 1000.0) / moveCount);
      int[] faceIndices = new int[moveCount];
      int[] cubies = new int[moveCount];
      int[] keyIndices = new int[moveCount];

      for (int i = 0; i < moveCount; i++) {
         faceIndices[i] = (int)(Math.random() * 6.0);
         cubies[i] = (int)(Math.random() * 8.0);
         keyIndices[i] = (int)(Math.random() * 3.0);
      }

      String[] faceColors = new String[moveCount];
      String[] keys = new String[moveCount];

      for (int i = 0; i < moveCount; i++) {
         faceColors[i] = this.colorNames[faceIndices[i]];
         keys[i] = this.moveKeys[keyIndices[i]];
      }

      for (int i = 0; i < moveCount; i++) {
         cube.refresh();
         System.out.printf("color: %s, cubie: %s, letter: %s\n", faceColors[i], cubies[i], keys[i]);
         cube.selectedFaceColor = faceColors[i];
         cube.frame.selectedCubie = cubies[i];
         if (keys[i] == "w") {
            try {
               Robot robot;
               (robot = new Robot()).keyPress(87);
               robot.keyRelease(87);
            } catch (AWTException e) {
               e.printStackTrace();
            }
         } else if (keys[i] == "a") {
            try {
               Robot robot;
               (robot = new Robot()).keyPress(65);
               robot.keyRelease(65);
            } catch (AWTException e) {
               e.printStackTrace();
            }
         } else if (keys[i] == "s") {
            try {
               Robot robot;
               (robot = new Robot()).keyPress(83);
               robot.keyRelease(83);
            } catch (AWTException e) {
               e.printStackTrace();
            }
         } else if (keys[i] == "d") {
            try {
               Robot robot;
               (robot = new Robot()).keyPress(68);
               robot.keyRelease(68);
            } catch (AWTException e) {
               e.printStackTrace();
            }
         }

         try {
            Thread.sleep((long)delay);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
   }
}
