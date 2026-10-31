package stockchecker;

public class Main {
   public static void main(String[] args) {
      StockChecker sc = new StockChecker();
      Window window = new Window(sc);
      window.setVisible(true);
   }
}
