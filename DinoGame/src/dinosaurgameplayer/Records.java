package dinosaurgameplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Your best run, the bot's best run and where its marker sits, kept in ~/.dinogame.txt. */
final class Records {
   private final File file;
   int best;
   int botBest;
   int markerX = PixelBot.DEFAULT_X;
   int markerY = PixelBot.DEFAULT_Y;

   Records(File file) {
      this.file = file;
   }

   static Records load(File file) {
      Records r = new Records(file);
      if (!file.isFile()) {
         return r;
      }
      try (InputStream in = new FileInputStream(file)) {
         for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
            String[] kv = line.trim().split("=", 2);
            if (kv.length < 2) {
               continue;
            }
            try {
               if (kv[0].equals("best")) {
                  r.best = Math.max(0, Integer.parseInt(kv[1].trim()));
               } else if (kv[0].equals("bot")) {
                  r.botBest = Math.max(0, Integer.parseInt(kv[1].trim()));
               } else if (kv[0].equals("marker")) {
                  String[] xy = kv[1].split(",");
                  int x = Integer.parseInt(xy[0].trim());
                  r.markerY = Integer.parseInt(xy[1].trim());
                  r.markerX = x;
               }
            } catch (RuntimeException e) {
               // a damaged line is skipped
            }
         }
      } catch (IOException e) {
         System.err.println("Could not read " + file + ": " + e.getMessage());
      }
      return r;
   }

   void save() {
      String text = "best=" + this.best + "\nbot=" + this.botBest + "\nmarker=" + this.markerX + "," + this.markerY + "\n";
      try (OutputStream out = new FileOutputStream(this.file)) {
         out.write(text.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
         System.err.println("Could not save " + this.file + ": " + e.getMessage());
      }
   }
}
