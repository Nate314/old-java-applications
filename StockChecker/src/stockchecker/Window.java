package stockchecker;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

public class Window extends JFrame implements ActionListener {
   private static final long serialVersionUID = 1L;
   private JPanel panel = new JPanel();
   private JButton addStockBtn = new JButton("addStock");
   private JButton deleteStockBtn = new JButton("deleteStock");
   private JButton makeItColorfulBtn = new JButton("COLOR!");
   private JButton refreshStockPriceBtn = new JButton("refresh");
   private SpringLayout layout = new SpringLayout();
   private int numberOfStocks = 0;
   private JTextField[] textBoxes = new JTextField[100];
   private JLabel[] labels = new JLabel[100];
   private JButton center = new JButton();
   private int width = 330;
   private int height = 0;
   private int screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
   private StockChecker stockchecker = null;

   public Window(StockChecker sc) {
      this.stockchecker = sc;
      this.setTitle("StockChecker");
      this.setSize(1000, 1000);
      this.setLocationRelativeTo(null);
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      this.createWindow();
   }

   private void createWindow() {
      this.center.setVisible(false);
      this.add(this.panel);
      this.panel.add(this.center);
      this.panel.setBackground(new Color(0, 0, 255));
      Font font = new Font("Helvetica", 1, 22);

      for (int i = 0; i < 100; i++) {
         this.textBoxes[i] = new JTextField("");
         // Enter in a ticker field saves the list and refreshes the prices.
         this.textBoxes[i].addActionListener(this);
         this.labels[i] = new JLabel("");
         this.labels[i].setFont(font);
         this.labels[i].setForeground(new Color(0, 255, 255));
      }

      this.panel.setVisible(true);
      this.panel.setLayout(this.layout);
      this.addStockBtn.addActionListener(this);
      this.deleteStockBtn.addActionListener(this);
      this.makeItColorfulBtn.addActionListener(this);
      this.refreshStockPriceBtn.addActionListener(this);
      this.panel.add(this.addStockBtn);
      this.panel.add(this.deleteStockBtn);
      this.panel.add(this.makeItColorfulBtn);
      this.panel.add(this.refreshStockPriceBtn);
      this.layout.putConstraint("North", this.center, 0, "North", this.panel);
      this.layout.putConstraint("East", this.center, 0, "HorizontalCenter", this.panel);
      this.layout.putConstraint("South", this.center, 0, "VerticalCenter", this.panel);
      this.layout.putConstraint("West", this.center, 0, "West", this.panel);
      this.layout.putConstraint("North", this.addStockBtn, 5, "North", this.panel);
      this.layout.putConstraint("East", this.addStockBtn, -5, "East", this.panel);
      this.layout.putConstraint("West", this.addStockBtn, 5, "East", this.center);
      this.layout.putConstraint("North", this.deleteStockBtn, 5, "South", this.addStockBtn);
      this.layout.putConstraint("East", this.deleteStockBtn, -5, "East", this.panel);
      this.layout.putConstraint("West", this.deleteStockBtn, 5, "HorizontalCenter", this.panel);
      this.layout.putConstraint("North", this.makeItColorfulBtn, 5, "North", this.panel);
      this.layout.putConstraint("East", this.makeItColorfulBtn, -5, "HorizontalCenter", this.panel);
      this.layout.putConstraint("West", this.makeItColorfulBtn, 5, "West", this.panel);
      this.layout.putConstraint("North", this.refreshStockPriceBtn, 5, "South", this.makeItColorfulBtn);
      this.layout.putConstraint("East", this.refreshStockPriceBtn, -5, "HorizontalCenter", this.panel);
      this.layout.putConstraint("West", this.refreshStockPriceBtn, 5, "West", this.panel);
      this.getFromMyStocks();
   }

   private void putConstraint(Component component, Component north, Component east, Component south, Component west, int pad) {
      this.layout.putConstraint("North", component, pad, "North", north);
      this.layout.putConstraint("East", component, -1 * pad, "East", east);
      this.layout.putConstraint("West", component, pad, "West", west);
      this.layout.putConstraint("South", component, -1 * pad, "South", south);
   }

   private void getFromMyStocks() {
      String stocks = new TextReader("myStocks").ReturnFileString();
      String[] myStocks = stocks.trim().split(",");
      this.numberOfStocks = Math.min(myStocks.length, this.textBoxes.length);
      if (this.numberOfStocks != 0) {
         for (int i = 0; i < this.numberOfStocks; i++) {
            JTextField textfield = this.textBoxes[i];
            textfield.setColumns(10);
            textfield.setText(myStocks[i].trim());
            textfield.setVisible(true);
            this.panel.add(textfield);
            JLabel label = this.labels[i];
            label.setText("$--");
            label.setVisible(true);
            this.panel.add(label);
            if (i == 0) {
               this.layout.putConstraint("North", textfield, 5, "South", this.deleteStockBtn);
               this.layout.putConstraint("West", textfield, 5, "West", this.panel);
               this.layout.putConstraint("North", label, 0, "North", textfield);
               this.layout.putConstraint("East", label, -10, "East", this.panel);
            } else {
               this.layout.putConstraint("North", textfield, 5, "South", this.textBoxes[i - 1]);
               this.layout.putConstraint("West", textfield, 0, "West", this.textBoxes[i - 1]);
               this.layout.putConstraint("North", label, 0, "North", textfield);
               this.layout.putConstraint("East", label, -10, "East", this.panel);
            }

         }
      }

      this.resize();
   }

   @Override
   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == this.addStockBtn) {
         this.addStock();
      }

      if (e.getSource() == this.deleteStockBtn) {
         this.deleteStock();
      }

      if (e.getSource() == this.makeItColorfulBtn) {
         this.makeItColorful();
      }

      if (e.getSource() == this.refreshStockPriceBtn || e.getSource() instanceof JTextField) {
         this.refreshStockPrice();
      }
   }

   private void addStock() {
      Dimension dim = new Dimension(330, 60 + this.numberOfStocks * (this.textBoxes[0].getHeight() + 5) + 2 * (this.textBoxes[0].getHeight() + 5));
      if (this.numberOfStocks < this.textBoxes.length && dim.getHeight() <= (double)(this.screenHeight - 50)) {
         JTextField textfield = this.textBoxes[this.numberOfStocks];
         textfield.setColumns(10);
         textfield.setVisible(true);
         this.panel.add(textfield);
         JLabel label = this.labels[this.numberOfStocks];
         label.setText("$--");
         label.setVisible(true);
         this.panel.add(label);
         if (this.numberOfStocks == 0) {
            this.layout.putConstraint("North", textfield, 5, "South", this.deleteStockBtn);
            this.layout.putConstraint("West", textfield, 5, "West", this.panel);
            this.layout.putConstraint("North", label, 0, "North", textfield);
            this.layout.putConstraint("East", label, -10, "East", this.panel);
         } else {
            this.layout.putConstraint("North", textfield, 5, "South", this.textBoxes[this.numberOfStocks - 1]);
            this.layout.putConstraint("West", textfield, 0, "West", this.textBoxes[this.numberOfStocks - 1]);
            this.layout.putConstraint("North", label, 0, "North", textfield);
            this.layout.putConstraint("East", label, -10, "East", this.panel);
         }

         this.numberOfStocks++;
         this.resize();
         this.saveMyStocks();
      }
   }

   private void deleteStock() {
      if (this.numberOfStocks != 0) {
         this.numberOfStocks--;
         this.textBoxes[this.numberOfStocks].setText("");
         this.textBoxes[this.numberOfStocks].setVisible(false);
         this.labels[this.numberOfStocks].setText("");
         this.labels[this.numberOfStocks].setVisible(false);
      }

      this.resize();
      this.saveMyStocks();
   }

   private void makeItColorful() {
      this.panel.setBackground(new Color((int)(Math.random() * 255.0), (int)(Math.random() * 255.0), (int)(Math.random() * 255.0)));

      for (int i = 0; i < 100; i++) {
         this.labels[i].setForeground(new Color((int)(Math.random() * 255.0), (int)(Math.random() * 255.0), (int)(Math.random() * 255.0)));
      }
   }

   /** The trimmed, non-empty tickers in list order. */
   private List<String> tickers() {
      List<String> tickers = new ArrayList<>();
      for (int i = 0; i < this.numberOfStocks; i++) {
         String ticker = this.textBoxes[i].getText().trim();
         if (!ticker.isEmpty()) {
            tickers.add(ticker);
         }
      }
      return tickers;
   }

   /** Writes the current tickers back to myStocks.txt, comma separated like the original file. */
   private void saveMyStocks() {
      try (FileWriter writer = new FileWriter("myStocks.txt")) {
         writer.write(String.join(",", this.tickers()));
      } catch (IOException e) {
         System.err.println("Could not save myStocks.txt: " + e.getMessage());
      }
   }

   /** Saves the list, then downloads quotes on a background thread and fills the labels back on the event thread. */
   private void refreshStockPrice() {
      this.saveMyStocks();
      List<String> tickers = this.tickers();
      this.refreshStockPriceBtn.setEnabled(false);
      this.refreshStockPriceBtn.setText("loading...");
      new Thread(() -> {
         List<String[]> rows = this.stockchecker.getStockPrice(tickers);
         SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < this.numberOfStocks; i++) {
               String ticker = this.textBoxes[i].getText().trim();
               this.labels[i].setText(ticker.isEmpty() ? "$--" : "$" + StockChecker.priceFor(rows, ticker));
            }
            this.refreshStockPriceBtn.setText("refresh");
            this.refreshStockPriceBtn.setEnabled(true);
         });
      }).start();
   }

   private void resize() {
      this.width = 330;
      this.height = 60 + this.numberOfStocks * 25 + 50;
      this.setSize(this.width, this.height);
      this.panel.revalidate();
      this.panel.repaint();
   }
}
