package stockchecker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * The window: the ticker list (still kept in {@code myStocks.txt}) and the COLOR! button across the top, and the
 * practice trading {@link DeskPanel} under them. The quote API the app was written for is retired, so the desk runs a
 * simulated market that opens at the last real quotes saved in {@code stockCheck.csv}.
 */
public class Window extends JFrame {
   private static final long serialVersionUID = 1L;
   /** The list the app shipped with, for when myStocks.txt is missing or empty. */
   private static final String DEFAULT_STOCKS = "crm,lmt,tsn,nflx,swk";

   private final JTextField tickers = new JTextField();
   private final JPanel top = new JPanel();
   private final Desk desk;
   private final DeskPanel deskPanel;

   public Window() {
      this.setTitle("StockChecker");
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      // Read after the window exists, so the browser build has put the saved files back.
      String stocks = new TextReader("myStocks").ReturnFileString().trim();
      if (stocks.isEmpty()) {
         stocks = DEFAULT_STOCKS;
      }
      this.desk = Desk.load(new File(System.getProperty("user.home"), ".stockchecker-desk.txt"), split(stocks), firstPrices());
      this.deskPanel = new DeskPanel(this.desk);

      JLabel label = new JLabel("Tickers:");
      label.setForeground(Color.WHITE);
      this.tickers.setColumns(34);
      this.tickers.setText(String.join(", ", this.desk.watch()));
      this.tickers.addActionListener(event -> this.applyTickers());
      JButton watch = new JButton("Watch");
      watch.addActionListener(event -> this.applyTickers());
      JButton color = new JButton("COLOR!");
      color.addActionListener(event -> {
         this.deskPanel.recolor();
         this.top.setBackground(this.deskPanel.background().darker());
      });
      this.top.add(label);
      this.top.add(this.tickers);
      this.top.add(watch);
      this.top.add(color);
      this.top.setBackground(this.deskPanel.background().darker());
      this.add(this.top, BorderLayout.NORTH);
      this.add(this.deskPanel, BorderLayout.CENTER);
      this.pack();
      this.setLocationRelativeTo(null);
      this.deskPanel.requestFocusInWindow();
   }

   /** Enter or Watch: the typed tickers become the watch list, saved to myStocks.txt. */
   private void applyTickers() {
      List<String> typed = split(this.tickers.getText());
      List<String> before = new ArrayList<>(this.desk.symbols());
      this.desk.setWatch(typed);
      this.tickers.setText(String.join(", ", this.desk.watch()));
      String note = null;
      if (Desk.clean(typed).size() < typed.size() && typed.size() > Desk.MAX_SYMBOLS) {
         note = "The desk watches up to " + Desk.MAX_SYMBOLS + " tickers.";
      }
      for (String symbol : this.desk.symbols()) {
         if (!this.desk.watch().contains(symbol)) {
            note = symbol + " stays on the desk until you sell it.";
         }
      }
      if (note == null && !this.desk.symbols().equals(before)) {
         note = "Watching " + this.desk.watch().size() + (this.desk.watch().size() == 1 ? " ticker." : " tickers.");
      }
      this.saveMyStocks();
      this.deskPanel.watchChanged(note);
      this.deskPanel.requestFocusInWindow();
   }

   private static List<String> split(String text) {
      List<String> symbols = new ArrayList<>();
      for (String part : text.split("[,\\s]+")) {
         if (!part.trim().isEmpty()) {
            symbols.add(part.trim());
         }
      }
      return symbols;
   }

   /** The last real quotes, from the stockCheck.csv the app last downloaded (May 13, 2016). */
   private static Map<String, Double> firstPrices() {
      Map<String, Double> prices = new HashMap<>();
      List<String[]> rows = new CSVReader().CSVToDoubleStringArray("stockCheck");
      for (String[] row : rows) {
         String symbol = row[0].replace("\"", "").trim();
         try {
            prices.put(symbol.toUpperCase(), Double.parseDouble(StockChecker.priceFor(rows, symbol)));
         } catch (NumberFormatException e) {
            // "N/A": no real quote for it.
         }
      }
      return prices;
   }

   /** Writes the watch list back to myStocks.txt, comma separated in lower case like the original file. */
   private void saveMyStocks() {
      try (FileWriter writer = new FileWriter("myStocks.txt")) {
         writer.write(String.join(",", this.desk.watch()).toLowerCase());
      } catch (IOException e) {
         System.err.println("Could not save myStocks.txt: " + e.getMessage());
      }
   }
}
