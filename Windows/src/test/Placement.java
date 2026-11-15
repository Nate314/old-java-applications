package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Where the user left the key, in ~/.windows-key.txt: its left edge and bottom edge on the screen ("x=1780",
 * "bottom=1016"; the bottom, since the shelf grows the window upward), its size ("size=2" for the biggest of
 * {@link #SCALES}), whether the shelf was open ("shelf=on") and whether hover to press was on ("hover=on"). A missing file or a damaged line leaves the defaults:
 * the lower right of the screen, the smallest key, the shelf shut.
 */
final class Placement {
   static final File FILE = new File(System.getProperty("user.home", "."), ".windows-key.txt");
   /** The key's sizes, as multiples of the first: a mouse, a finger, a finger from across the room. */
   static final double[] SCALES = {1, 1.5, 2};
   static final String[] SIZE_NAMES = {"Small", "Medium", "Large"};
   /** -1 until the user first moves the key. */
   int x = -1;
   int bottom = -1;
   int size;
   boolean shelf;
   boolean hover;

   double scale() {
      return SCALES[this.size];
   }

   static Placement load(File file) {
      Placement p = new Placement();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            int eq = line.indexOf('=');
            String name = eq < 0 ? line : line.substring(0, eq).trim();
            String value = eq < 0 ? "" : line.substring(eq + 1).trim();
            try {
               if (name.equals("x")) {
                  p.x = Math.max(0, Integer.parseInt(value));
               } else if (name.equals("bottom")) {
                  p.bottom = Math.max(0, Integer.parseInt(value));
               } else if (name.equals("size")) {
                  int size = Integer.parseInt(value);
                  if (size >= 0 && size < SCALES.length) {
                     p.size = size;
                  }
               } else if (name.equals("shelf")) {
                  p.shelf = value.equals("on");
               } else if (name.equals("hover")) {
                  p.hover = value.equals("on");
               }
            } catch (NumberFormatException damaged) {
               // A damaged line is skipped; the rest still count.
            }
         }
      } catch (IOException | RuntimeException missing) {
         // No file yet: the defaults.
      }
      // Half a place is no place: both edges or neither.
      if (p.x < 0 || p.bottom < 0) {
         p.x = -1;
         p.bottom = -1;
      }
      return p;
   }

   void save(File file) {
      try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
         if (this.x >= 0) {
            writer.write("x=" + this.x + "\nbottom=" + this.bottom + "\n");
         }
         writer.write("size=" + this.size + "\nshelf=" + (this.shelf ? "on" : "off") + "\nhover="
            + (this.hover ? "on" : "off") + "\n");
      } catch (IOException | RuntimeException exception) {
         System.err.println("Could not save " + file + ": " + exception);
      }
   }
}
