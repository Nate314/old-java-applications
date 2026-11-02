package animate2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumMap;
import java.util.Map;

/**
 * The most time left each Star Catcher night was caught with, kept in a small text file of
 * {@code NIGHT=stars,tenths} lines such as {@code DRIFT=3,142} (3 stars, 14.2 seconds left). A night never caught
 * has no line. The first night is always open; each other one opens once the night before it is caught.
 */
public final class NightRecords {
   private final File file;
   private final Map<Night, Integer> best = new EnumMap<>(Night.class);

   private NightRecords(File file) {
      this.file = file;
   }

   /** Reads the file; a missing or unreadable file, or a line this version does not know, is skipped. */
   public static NightRecords load(File file) {
      NightRecords records = new NightRecords(file);
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
         for (String line; (line = reader.readLine()) != null;) {
            int split = line.indexOf('=');
            String[] values = split > 0 ? line.substring(split + 1).split(",") : new String[0];
            if (values.length != 2) {
               continue;
            }
            try {
               Night night = Night.valueOf(line.substring(0, split).trim());
               int tenths = Integer.parseInt(values[1].trim());
               if (tenths >= 0) {
                  records.best.put(night, tenths);
               }
            } catch (IllegalArgumentException e) {
               // Not a record (NumberFormatException is one too): skip it.
            }
         }
      } catch (IOException e) {
         // No file yet: no night caught so far.
      }
      return records;
   }

   public boolean isCaught(Night night) {
      return this.best.containsKey(night);
   }

   /** The most tenths of a second left when {@code night} was caught, or -1 if it never was. */
   public int bestTenths(Night night) {
      Integer tenths = this.best.get(night);
      return tenths == null ? -1 : tenths;
   }

   public int stars(Night night) {
      int tenths = this.bestTenths(night);
      return tenths < 0 ? 0 : Night.starsFor(tenths / 10.0);
   }

   public int totalStars() {
      int total = 0;
      for (Night night : Night.values()) {
         total += this.stars(night);
      }
      return total;
   }

   public boolean isOpen(Night night) {
      return night.ordinal() == 0 || this.isCaught(Night.values()[night.ordinal() - 1]);
   }

   /** The first open night without three stars (the last night when every one has them). */
   public Night suggested() {
      for (Night night : Night.values()) {
         if (this.isOpen(night) && this.stars(night) < 3) {
            return night;
         }
      }
      return Night.values()[Night.values().length - 1];
   }

   /** Keeps a caught night if more time was left than before; returns true when it was. */
   public boolean add(CatchGame game) {
      if (game.state() != CatchGame.State.CAUGHT || game.tenthsLeft() <= this.bestTenths(game.night())) {
         return false;
      }
      this.best.put(game.night(), game.tenthsLeft());
      this.save();
      return true;
   }

   private void save() {
      try (Writer writer = new FileWriter(this.file)) {
         for (Map.Entry<Night, Integer> entry : this.best.entrySet()) {
            writer.write(entry.getKey().name() + "=" + Night.starsFor(entry.getValue() / 10.0) + "," + entry.getValue() + "\n");
         }
      } catch (IOException e) {
         // Could not save: the record lasts until the window closes.
      }
   }
}
