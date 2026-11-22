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

      // An upright phone: two columns of four rows fit 400 px, the eighth cell is still the call to action.
      SkyPicker narrow = new SkyPicker(modes, () -> opened[0]++, true);
      narrow.setSize(narrow.getPreferredSize());
      check(narrow.getWidth() <= 400 - 16, "the phone grid fits 400 px (" + narrow.getWidth() + ")");
      int nStepX = SkyPicker.NARROW_TILE_WIDTH + SkyPicker.GAP;
      int nStepY = SkyPicker.NARROW_TILE_HEIGHT + SkyPicker.GAP;
      int nLeft = (narrow.getWidth() - (2 * SkyPicker.NARROW_TILE_WIDTH + SkyPicker.GAP)) / 2;
      for (int mode = 0; mode < 7; mode++) {
         int x = nLeft + (mode % 2) * nStepX + 10;
         int y = SkyPicker.GAP + (mode / 2) * nStepY + 10;
         check(narrow.tileAt(x, y) == mode, "phone tile " + mode + " is under its own corner");
      }
      check(narrow.tileAt(nLeft + nStepX + 10, SkyPicker.GAP + 3 * nStepY + 10) == -1, "the phone's eighth cell is no tile");

      // Every mode's line wraps inside the phone grid; a line that fits stays whole.
      java.awt.FontMetrics metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).getGraphics()
         .getFontMetrics(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12));
      for (int mode = 0; mode < 7; mode++) {
         String about = StarField.MODE_NAMES[mode] + ": " + SkyPicker.ABOUT[mode];
         java.util.List<String> lines = StarField.wrap(metrics, about, 360);
         check(String.join(" ", lines).equals(about), "wrapping keeps every word of " + about);
         lines.forEach(line -> check(metrics.stringWidth(line) <= 360, "\"" + line + "\" fits 360 px"));
      }
      check(StarField.wrap(metrics, "Pick a sky", 360).size() == 1, "a short line stays whole");

      // A sky opens no bigger than the screen, as full of stars as the old 800 x 800 one.
      check(SettingsFrame.skySize(new java.awt.Dimension(1920, 1080)).equals(new java.awt.Dimension(800, 800)), "a big screen keeps 800 x 800");
      check(SettingsFrame.starsFor(new java.awt.Dimension(800, 800)) == 1000, "800 x 800 keeps 1000 stars");
      check(SettingsFrame.skySize(new java.awt.Dimension(900, 560)).equals(new java.awt.Dimension(800, 560)), "a short screen gets a wide sky");
      check(SettingsFrame.skySize(new java.awt.Dimension(400, 640)).equals(new java.awt.Dimension(400, 640)), "a phone gets an upright sky");
      check(SettingsFrame.starsFor(new java.awt.Dimension(400, 640)) == 400, "a phone's sky has 400 stars");
      check(SettingsFrame.starsFor(new java.awt.Dimension(100, 100)) == 100, "never fewer than 100 stars");
      System.out.println("SkyPickerCheck passed");
      System.exit(0); // the tiles' timer would keep the check running
   }

   private static void check(boolean ok, String what) {
      if (!ok) {
         throw new AssertionError(what);
      }
   }
}
