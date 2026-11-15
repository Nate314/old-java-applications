package windowse;

import java.awt.AWTException;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import javax.swing.SwingUtilities;

public class WindowsEMain {
   /**
    * Arguments, all optional: a list a friend sent (its code, such as Ex10-Left-E-Right, or a link with ?keys=), and
    * the address of the page the app runs on, which the browser build passes (with ?keys= in it when the page was
    * opened from a link). A friend's list is only put on the list; nothing is pressed until DO IT!!!.
    */
   public static void main(String[] args) throws AWTException {
      Keys keys = new RobotKeys();
      String page = null;
      List<Step> friend = null;
      for (String arg : args) {
         boolean web = arg.startsWith("http://") || arg.startsWith("https://");
         if (web) {
            page = upTo(arg, "?#");
         }
         int at = arg.indexOf("keys=");
         if (!web || at >= 0) {
            friend = Step.parse(upTo(at < 0 ? arg : arg.substring(at + 5), "&#"));
         }
      }
      String sendPage = page;
      List<Step> sent = friend;
      SwingUtilities.invokeLater(() -> new WindowsEWindow(keys,
         text -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null), sendPage, sent)
         .setVisible(true));
   }

   /** text up to the first of ends. */
   private static String upTo(String text, String ends) {
      for (int i = 0; i < text.length(); i++) {
         if (ends.indexOf(text.charAt(i)) >= 0) {
            return text.substring(0, i);
         }
      }
      return text;
   }
}
