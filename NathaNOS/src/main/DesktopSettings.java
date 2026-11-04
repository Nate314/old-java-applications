package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The desktop as the user left it, in ~/.nathanos.txt: the wallpaper ("wallpaper=sunset", or
 * "wallpaper=picture:Picture 2" for a Paint picture) and, once the icons have
 * been moved by hand, each icon's cell ("icon=Minesweeper,2,0" for column 2, row 0). A missing file or a damaged line
 * leaves the defaults.
 */
final class DesktopSettings {
   static final File FILE = new File(System.getProperty("user.home", "."), ".nathanos.txt");
   String wallpaper = Wallpaper.DEFAULT;
   /** App name to its {column, row} on the icon grid; empty until the user first moves an icon. */
   final Map<String, int[]> places = new LinkedHashMap<>();

   static DesktopSettings load(File file) {
      DesktopSettings settings = new DesktopSettings();
      List<String> lines = new ArrayList<>();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            lines.add(line);
         }
      } catch (IOException | RuntimeException missing) {
         return settings;
      }
      for (String line : lines) {
         try {
            if (line.startsWith("wallpaper=")) {
               String id = line.substring("wallpaper=".length()).trim();
               // A Paint picture's id is kept too; Main falls back to the photo if that picture is gone.
               if (Wallpaper.byId(id) != null || id.startsWith(Wallpaper.PICTURE)) {
                  settings.wallpaper = id;
               }
            } else if (line.startsWith("icon=")) {
               String[] parts = line.substring("icon=".length()).split(",");
               int column = parts.length == 3 ? Integer.parseInt(parts[1].trim()) : -1;
               int row = parts.length == 3 ? Integer.parseInt(parts[2].trim()) : -1;
               if (column >= 0 && row >= 0) {
                  settings.places.put(parts[0].trim(), new int[] {column, row});
               }
            }
         } catch (RuntimeException damaged) {
            // A damaged line is skipped; the rest still count.
         }
      }
      return settings;
   }

   void save(File file) {
      try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
         writer.write("wallpaper=" + this.wallpaper + "\n");
         for (Map.Entry<String, int[]> place : this.places.entrySet()) {
            writer.write("icon=" + place.getKey() + "," + place.getValue()[0] + "," + place.getValue()[1] + "\n");
         }
      } catch (IOException | RuntimeException exception) {
         System.err.println("Could not save " + file + ": " + exception);
      }
   }
}
