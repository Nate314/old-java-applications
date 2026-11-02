package dinosaurgameplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Your best run, the bot's best run and where its marker sits, kept in ~/.dinogame.txt, with the evolution's
 * progress: generations bred, the best score and the brain that ran it, and each generation's best for the chart.
 */
final class Records {
   private final File file;
   int best;
   int botBest;
   int markerX = PixelBot.DEFAULT_X;
   int markerY = PixelBot.DEFAULT_Y;
   int generations;
   int evolvedBest;
   Brain champion;
   final List<Integer> chart = new ArrayList<>();

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
               } else if (kv[0].equals("evolved")) {
                  String[] gb = kv[1].split(",");
                  int gens = Math.max(0, Integer.parseInt(gb[0].trim()));
                  r.evolvedBest = Math.max(0, Integer.parseInt(gb[1].trim()));
                  r.generations = gens;
               } else if (kv[0].equals("champion")) {
                  r.champion = Brain.decode(kv[1]);
               } else if (kv[0].equals("chart")) {
                  List<Integer> chart = new ArrayList<>();
                  for (String n : kv[1].split(",")) {
                     chart.add(Math.max(0, Integer.parseInt(n.trim())));
                  }
                  r.chart.addAll(chart);
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
      StringBuilder text = new StringBuilder("best=" + this.best + "\nbot=" + this.botBest + "\nmarker=" + this.markerX + "," + this.markerY + "\n");
      if (this.champion != null && this.generations > 0) {
         text.append("evolved=").append(this.generations).append(',').append(this.evolvedBest).append('\n');
         text.append("champion=").append(this.champion.encode()).append('\n');
         StringBuilder chart = new StringBuilder();
         for (int n : this.chart) {
            chart.append(chart.length() > 0 ? "," : "").append(n);
         }
         text.append("chart=").append(chart).append('\n');
      }
      try (OutputStream out = new FileOutputStream(this.file)) {
         out.write(text.toString().getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
         System.err.println("Could not save " + this.file + ": " + e.getMessage());
      }
   }
}
