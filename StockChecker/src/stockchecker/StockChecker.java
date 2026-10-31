package stockchecker;

import java.io.IOException;
import java.net.MalformedURLException;

public class StockChecker {
   private CSVReader stringToCSV = new CSVReader();
   private WebpageDownloader URLToFile = new WebpageDownloader();

   public String[][] getStockPrice(String[] stockSymbol) {
      String stockSymbols = "";

      for (int i = 0; i < stockSymbol.length; i++) {
         if (stockSymbol.length == 1) {
            stockSymbols = stockSymbols + stockSymbol[i];
         } else if (i != stockSymbol.length) {
            stockSymbols = stockSymbols + stockSymbol[i] + ",";
         } else {
            stockSymbols = stockSymbols + stockSymbol[i];
         }
      }

      try {
         this.URLToFile.DownloadWebpage("stockCheck.csv", "http://finance.yahoo.com/d/quotes.csv?s=" + stockSymbols + "&f=sl1d1t1c1ohgv&e=.csv");
      } catch (MalformedURLException var4) {
         var4.printStackTrace();
      } catch (IOException var5) {
         var5.printStackTrace();
      }

      return this.stringToCSV.CSVToDoubleStringArray("stockCheck");
   }
}
