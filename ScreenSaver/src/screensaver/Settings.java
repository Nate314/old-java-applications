package screensaver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/** The saver last shown, whether it cycles, the speed and the tile size, kept in ~/.screensaver.txt between runs. */
class Settings {
   private final File file;
   int saver = 0;
   boolean cycle = true;
   double speed = 1.0;
   /** 0 picks a size from the screen. */
   double tileSize = 0;

   Settings(File file) {
      this.file = file;
   }

   static File defaultFile() {
      return new File(System.getProperty("user.home", "."), ".screensaver.txt");
   }

   /** Reads the file, keeping the defaults for anything missing or unreadable. */
   void load() {
      if (!this.file.isFile()) {
         return;
      }
      try (BufferedReader in = new BufferedReader(new FileReader(this.file))) {
         String line;
         while ((line = in.readLine()) != null) {
            String[] parts = line.trim().split(" ");
            if (parts.length != 2) {
               continue;
            }
            try {
               if (parts[0].equals("saver")) {
                  this.saver = Integer.parseInt(parts[1]);
               } else if (parts[0].equals("cycle")) {
                  this.cycle = parts[1].equals("on");
               } else if (parts[0].equals("speed")) {
                  this.speed = Math.max(Main.MIN_SPEED, Math.min(Main.MAX_SPEED, Double.parseDouble(parts[1])));
               } else if (parts[0].equals("tiles")) {
                  this.tileSize = Math.max(0, Double.parseDouble(parts[1]));
               }
            } catch (NumberFormatException e) {
               // Keep the default for this line.
            }
         }
      } catch (IOException e) {
         System.out.println("Could not read " + this.file + ": " + e.getMessage());
      }
   }

   void save() {
      try (Writer out = new FileWriter(this.file)) {
         out.write("saver " + this.saver + "\ncycle " + (this.cycle ? "on" : "off") + "\nspeed " + this.speed
            + "\ntiles " + this.tileSize + "\n");
      } catch (IOException e) {
         System.out.println("Could not save " + this.file + ": " + e.getMessage());
      }
   }
}
