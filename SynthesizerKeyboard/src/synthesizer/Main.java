package synthesizer;

import javax.swing.SwingUtilities;

public class Main {
   /**
    * Arguments, all optional: a tune code or link a friend sent (it opens as a lesson and plays once), and the address
    * of the page the keyboard runs on, for the links it sends (the browser build passes its own address, which carries
    * ?tune= when the page was opened from a friend's link).
    */
   public static void main(String[] args) {
      String page = null;
      String code = null;
      for (String arg : args) {
         String argPage = Tune.pageOf(arg);
         if (argPage != null) {
            page = argPage;
         }
         if (argPage == null || arg.contains("tune=")) {
            code = Tune.codeIn(arg);
         }
      }
      String tunePage = page;
      String tuneCode = code;
      SwingUtilities.invokeLater(() -> new KeyboardWindow(tunePage, tuneCode));
   }
}
