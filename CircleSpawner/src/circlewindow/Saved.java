package circlewindow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;

/** The last shape, size, colors, word and drawing, kept as key=value lines in ~/.circlespawner.txt. */
final class Saved {
   private static final File FILE = new File(System.getProperty("user.home", "."), ".circlespawner.txt");
   private final Map<String, String> values = new LinkedHashMap<>();
   private String written = "";

   /** Reads the file; a missing or unreadable file leaves every value unset. */
   static Saved load() {
      Saved saved = new Saved();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FILE), "utf-8"))) {
         String line;
         while ((line = reader.readLine()) != null) {
            int eq = line.indexOf('=');
            if (eq > 0) {
               saved.values.put(line.substring(0, eq), line.substring(eq + 1));
            }
         }
      } catch (IOException | RuntimeException e) {
         // No file yet: everything starts at its default.
      }
      return saved;
   }

   String get(String key, String fallback) {
      return this.values.getOrDefault(key, fallback);
   }

   void put(String key, String value) {
      this.values.put(key, value.replace('\n', ' '));
   }

   /** Writes the file, unless nothing changed since the last write. */
   void store() {
      StringBuilder out = new StringBuilder();
      for (Map.Entry<String, String> e : this.values.entrySet()) {
         out.append(e.getKey()).append('=').append(e.getValue()).append('\n');
      }
      if (out.toString().equals(this.written)) {
         return;
      }
      try (Writer writer = new OutputStreamWriter(new FileOutputStream(FILE), "utf-8")) {
         writer.write(out.toString());
         this.written = out.toString();
      } catch (IOException e) {
         // Not saved; the app still works.
      }
   }
}
