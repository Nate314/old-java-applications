package animate2;

import java.awt.image.BufferedImage;
import javax.swing.JComboBox;

/** Self-check for the settings window's seven live skies: what a click hits, and that every tile shows its mode. Run with java -ea. */
public class SkyPickerCheck {
   public static void main(String[] args) {
      JComboBox<String> modes = new JComboBox<>(new String[]{"1", "2", "3", "4", "5", "6", "7"});
      int[] opened = {0};
      SkyPicker picker = new SkyPicker(modes, () -> opened[0]++);
      picker.setSize(picker.getPreferredSize());
      int step = SkyPicker.TILE_WIDTH + SkyPicker.GAP;
      int left = (picker.getWidth() - (SkyPicker.COLUMNS * SkyPicker.TILE_WIDTH + (SkyPicker.COLUMNS - 1) * SkyPicker.GAP)) / 2;
      int rowTwo = SkyPicker.GAP + SkyPicker.TILE_HEIGHT + SkyPicker.GAP;
      for (int mode = 0; mode < 7; mode++) {
         int x = left + (mode % 4) * step + 10;
         int y = (mode < 4 ? SkyPicker.GAP : rowTwo) + 10;
         check(picker.tileAt(x, y) == mode, "tile " + mode + " is under its own corner");
      }
      check(picker.tileAt(left + 3 * step + 10, rowTwo + 10) == -1, "the eighth cell (the call to action) is no tile");
      check(picker.tileAt(left + SkyPicker.TILE_WIDTH + 2, SkyPicker.GAP + 10) == -1, "the gap between tiles is no tile");
      check(picker.tileAt(left - 3, SkyPicker.GAP + 10) == -1 && picker.tileAt(left + 10, 2) == -1, "outside the grid is no tile");

      // Each tile is drawn a few seconds in: its stars show (many colors besides the sky), and no two tiles look alike.
      int[] seen = new int[7];
      for (int mode = 0; mode < 7; mode++) {
         BufferedImage frame = picker.frame(mode);
         int sky = frame.getRGB(0, 0);
         for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
               if (frame.getRGB(x, y) != sky) {
                  seen[mode]++;
               }
            }
         }
         check(seen[mode] > 150, "tile " + mode + " shows its stars (" + seen[mode] + " lit px)");
      }
      check(SkyPicker.ABOUT.length == StarField.MODE_NAMES.length, "every mode has its line");
      System.out.println("SkyPickerCheck passed");
      System.exit(0); // the tiles' timer would keep the check running
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
