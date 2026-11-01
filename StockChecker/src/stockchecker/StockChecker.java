package stockchecker;

import java.io.IOException;
import java.util.List;

public class StockChecker {
   private CSVReader stringToCSV = new CSVReader();
   private WebpageDownloader URLToFile = new WebpageDownloader();

   /** Downloads quotes for the symbols into stockCheck.csv (keeping the old file if that fails) and returns its rows. */
   public List<String[]> getStockPrice(List<String> stockSymbols) {
      try {
         this.URLToFile.DownloadWebpage("stockCheck.csv", quoteUrl(stockSymbols));
      } catch (IOException | RuntimeException e) {
         System.err.println("Download failed, using the saved stockCheck.csv: " + e);
      }

      return this.stringToCSV.CSVToDoubleStringArray("stockCheck");
   }

   /** The retired Yahoo Finance CSV URL for the given symbols, comma separated with no empty entries. */
   public static String quoteUrl(List<String> stockSymbols) {
      return "https://finance.yahoo.com/d/quotes.csv?s=" + String.join(",", stockSymbols) + "&f=sl1d1t1c1ohgv&e=.csv";
   }

   /** The last price for a symbol from the CSV rows (first column is the quoted symbol), or "--" if absent. */
   public static String priceFor(List<String[]> rows, String symbol) {
      for (String[] row : rows) {
         if (row.length > 1 && row[0].replace("\"", "").trim().equalsIgnoreCase(symbol.trim())) {
            return row[1].trim();
         }
      }
      return "--";
   }
}
