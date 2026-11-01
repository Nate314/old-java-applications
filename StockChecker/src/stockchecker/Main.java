package stockchecker;

import javax.swing.SwingUtilities;

public class Main {
   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> {
         StockChecker sc = new StockChecker();
         Window window = new Window(sc);
         window.setVisible(true);
      });
   }
}
