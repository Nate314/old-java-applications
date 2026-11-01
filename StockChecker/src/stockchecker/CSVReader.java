package stockchecker;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class CSVReader {
   /** Reads every line of {@code fileName}.csv; a missing or unreadable file gives no rows. */
   public List<String[]> CSVToDoubleStringArray(String fileName) {
      try (FileReader reader = new FileReader(fileName + ".csv")) {
         return parse(reader);
      } catch (IOException e) {
         System.err.println("Could not read " + fileName + ".csv: " + e.getMessage());
         return new ArrayList<>();
      }
   }

   /** Splits each non-blank line on commas, however many lines there are. */
   public static List<String[]> parse(Reader reader) throws IOException {
      List<String[]> rows = new ArrayList<>();
      BufferedReader lines = new BufferedReader(reader);
      String line;
      while ((line = lines.readLine()) != null) {
         if (!line.trim().isEmpty()) {
            rows.add(line.split(","));
         }
      }
      return rows;
   }
}
