package audiobibledownloader;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * BibleDownloader, now a Bible reader: an open book that turns its pages chapter after chapter, remembers your place,
 * which chapters you have read and the verses you highlight, and still saves chapters as web pages.
 */
public class Main extends JFrame {
   private static final long serialVersionUID = 1L;

   public Main(Boolean visibility) {
      this.setTitle("BibleDownloader");
      URL icon = this.getClass().getResource("/assets/bible.png");
      if (icon != null) {
         this.setIconImage(Toolkit.getDefaultToolkit().getImage(icon));
      }
      this.setDefaultCloseOperation(3);
      // Read after the window exists, so the browser build has put the saved file back.
      Shelf shelf = Shelf.load(new File(System.getProperty("user.home"), ".bibledownloader.txt"));
      ReaderPanel reader = new ReaderPanel(shelf);
      // A phone-sized screen gets a window that fits it, with one page at a time.
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      reader.setPreferredSize(new Dimension(Math.max(340, Math.min(900, screen.width - 24)), Math.max(540, Math.min(600, screen.height - 60))));
      this.add(reader);
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(visibility);
      reader.requestFocusInWindow();
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
