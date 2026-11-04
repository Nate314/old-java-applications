package ultimateticktacktoe;

/**
 * A game with a friend elsewhere travels as a code of its moves, one move per message. Each move is two characters,
 * the board as a letter a-i and the cell as a digit 1-9, both in reading order ("e5" is the very middle), so a whole
 * game is at most 162 characters. On the web the code rides in a link to the game's page ({@code ?game=e5a1}); the
 * desktop app has no page, so it sends the code alone. Either one can be pasted into either app.
 */
final class FriendLink {
   private static final String PARAM = "game=";

   private FriendLink() {
   }

   static String moveCode(int move) {
      return "" + (char)('a' + move / 9) + (char)('1' + move % 9);
   }

   /** Replays a code; null if it is not one, or one of its moves breaks the rules. */
   static GameState replay(String code) {
      if (code.length() % 2 != 0) {
         return null;
      }
      GameState game = new GameState();
      for (int i = 0; i < code.length(); i += 2) {
         int board = code.charAt(i) - 'a';
         int cell = code.charAt(i + 1) - '1';
         if (board < 0 || board > 8 || cell < 0 || cell > 8 || !game.play(board, cell)) {
            return null;
         }
      }
      return game;
   }

   /** The code in a pasted link (the value of game=) or code (the text itself), without spaces and in lower case. */
   static String codeIn(String text) {
      int at = text.indexOf(PARAM);
      String code = text;
      if (at >= 0) {
         code = text.substring(at + PARAM.length());
         int end = indexOfAny(code, "&#");
         code = end < 0 ? code : code.substring(0, end);
      }
      return code.replaceAll("\s", "").toLowerCase();
   }

   /** The address of the page a link points at (without its ? query or # part), or null if the text is not a web address. */
   static String pageOf(String text) {
      if (!text.startsWith("http://") && !text.startsWith("https://")) {
         return null;
      }
      int end = indexOfAny(text, "?#");
      return end < 0 ? text : text.substring(0, end);
   }

   /** What to send the friend: a link to the page with the game in it, or the code alone where there is no page. */
   static String message(String page, String code) {
      return page == null ? code : page + "?" + PARAM + code;
   }

   private static int indexOfAny(String text, String chars) {
      for (int i = 0; i < text.length(); i++) {
         if (chars.indexOf(text.charAt(i)) >= 0) {
            return i;
         }
      }
      return -1;
   }
}
