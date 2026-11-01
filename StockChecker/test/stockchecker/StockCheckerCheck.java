package stockchecker;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/** Self-check for the quote URL and CSV parsing. Run with java -ea. */
public class StockCheckerCheck {
   public static void main(String[] args) throws Exception {
      // Fewer than 100 lines (and a trailing blank line) parse fine.
      List<String[]> rows = CSVReader.parse(new StringReader(
         "\"crm\",76.37,\"5/13/2016\",\"4:01pm\",+0.48,76.00,77.34,75.95,3583249\n"
            + "\"lmt\",242.12,\"5/13/2016\",\"4:02pm\",-2.82,242.38,244.50,241.70,1198746\n"
            + "\n"));
      check(rows.size() == 2, "expected 2 rows, got " + rows.size());
      check(CSVReader.parse(new StringReader("")).isEmpty(), "empty file gives no rows");

      // Prices are matched by symbol, ignoring quotes and case, with "--" when missing.
      check(StockChecker.priceFor(rows, "crm").equals("76.37"), "crm price");
      check(StockChecker.priceFor(rows, " LMT ").equals("242.12"), "lmt price, any case");
      check(StockChecker.priceFor(rows, "nflx").equals("--"), "missing symbol");

      // The URL lists only the given symbols: no null entries and no trailing comma.
      String url = StockChecker.quoteUrl(Arrays.asList("crm", "lmt", "googl"));
      check(url.contains("?s=crm,lmt,googl&"), "symbols in URL: " + url);
      check(!url.contains("null"), "no null symbols: " + url);

      System.out.println("StockCheckerCheck passed");
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
