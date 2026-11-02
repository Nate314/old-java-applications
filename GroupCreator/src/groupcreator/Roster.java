package groupcreator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * The class the window remembers between runs: the names as typed, min and max, and every round made so far. It is
 * a small UTF-8 text file:
 *
 * <pre>
 * names=Alice,Bob,Carol,Dave
 * min=2
 * max=3
 * round=Alice,Carol|Bob,Dave
 * </pre>
 *
 * with one {@code round=} line per round, oldest first, its groups separated by {@code |}.
 */
public final class Roster {
   static final String EXAMPLE = "Alice,Bob,Carol,Dave,Erin,Frank,Grace,Heidi,Ivan,Judy,Mallory,Niaj";

   private final File file;
   String names = EXAMPLE;
   String min = "2";
   String max = "3";
   final PartnerHistory history = new PartnerHistory();

   private Roster(File file) {
      this.file = file;
   }

   /** Reads the file; a missing file gives the example class with no rounds, and unknown lines are skipped. */
   public static Roster load(File file) {
      Roster roster = new Roster(file);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"))) {
         for (String line; (line = reader.readLine()) != null;) {
            int split = line.indexOf('=');
            if (split < 0) {
               continue;
            }
            String key = line.substring(0, split);
            String value = line.substring(split + 1);
            if (key.equals("names")) {
               roster.names = value;
            } else if (key.equals("min")) {
               roster.min = value;
            } else if (key.equals("max")) {
               roster.max = value;
            } else if (key.equals("round")) {
               List<List<String>> groups = new ArrayList<>();
               for (String group : value.split("\\|")) {
                  List<String> members = GroupMaker.parseNames(group);
                  if (!members.isEmpty()) {
                     groups.add(members);
                  }
               }
               if (!groups.isEmpty()) {
                  roster.history.add(groups);
               }
            }
         }
      } catch (IOException e) {
         // No file yet: the example class.
      }
      return roster;
   }

   public void save() throws IOException {
      try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "utf-8")) {
         writer.write(text());
      }
   }

   /** The file's contents. */
   String text() {
      StringBuilder text = new StringBuilder();
      text.append("names=").append(names).append('\n');
      text.append("min=").append(min).append('\n');
      text.append("max=").append(max).append('\n');
      for (int r = 1; r <= history.size(); r++) {
         List<String> groups = new ArrayList<>();
         for (List<String> group : history.round(r)) {
            groups.add(String.join(",", group));
         }
         text.append("round=").append(String.join("|", groups)).append('\n');
      }
      return text.toString();
   }
}
