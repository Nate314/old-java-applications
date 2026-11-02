package ultimateticktacktoe;

import javax.swing.SwingUtilities;

public class Main {
   /**
    * Arguments, all optional: a game code or link a friend sent (opens it in Friend by link mode), and the address of
    * the page the game runs on, for the links it sends (the browser build passes its own address, which carries
    * ?game= when the page was opened from a friend's link).
    */
   public static void main(String[] args) {
      String page = null;
      String code = null;
      for (String arg : args) {
         String argPage = FriendLink.pageOf(arg);
         if (argPage != null) {
            page = argPage;
         }
         if (argPage == null || arg.contains("game=")) {
            code = FriendLink.codeIn(arg);
         }
      }
      String friendPage = page;
      String friendCode = code;
      SwingUtilities.invokeLater(() -> new GameFrame(friendPage, friendCode));
   }
}
