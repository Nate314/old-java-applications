package circlewindow;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import javax.swing.SwingUtilities;

public class Main {
   /**
    * Arguments, all optional: a shape a friend sent (its code, or a link with ?shape=), and the address of the page
    * the app runs on, which the browser build passes (with ?shape= in it when the page was opened from a link).
    */
   public static void main(String[] args) {
      String page = null;
      Link sent = null;
      for (String arg : args) {
         boolean web = arg.startsWith("http://") || arg.startsWith("https://");
         if (web) {
            int end = arg.length();
            for (char c : "?#".toCharArray()) {
               end = arg.indexOf(c) >= 0 ? Math.min(end, arg.indexOf(c)) : end;
            }
            page = arg.substring(0, end);
         }
         if (!web || arg.contains("shape=")) {
            sent = Link.parse(arg);
         }
      }
      String sendPage = page;
      Link friend = sent;
      SwingUtilities.invokeLater(() -> new CircleCreatorWindow(
         text -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null),
         sendPage, friend));
   }
}
