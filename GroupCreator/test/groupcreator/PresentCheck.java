package groupcreator;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Self-check for Present: how much it enlarges the board, and F, Esc, the Back button and the chart turning it off. */
public class PresentCheck {
   public static void main(String[] args) {
      // Twelve names in pairs on the window's board without the inputs (760 x 510): twice the size, and no more.
      double s = GroupBoard.presentScale(760, 510, 6, 2, false);
      check(s == 2.0, "twelve in pairs are drawn at twice the size, not " + s);
      check(GroupBoard.fullColumns((int) (760 / 2.0), (int) (510 / 2.0) - 62, 6, 2) > 0, "and they fit at full size");
      check(GroupBoard.fullColumns((int) (760 / 2.05), (int) (510 / 2.05) - 62, 6, 2) < 0, "a little more would not fit");
      // A bigger screen enlarges more, up to four times; a big class still never shrinks.
      double big = GroupBoard.presentScale(1920, 1040, 6, 2, false);
      check(big > 3 && big <= 4, "a full HD screen enlarges more: " + big);
      check(GroupBoard.presentScale(1920, 1040, 1, 2, false) == 4, "at most four times");
      check(GroupBoard.presentScale(760, 510, 15, 2, false) >= 1, "thirty names never shrink");
      // The wheel (and the empty deck) keep their own 760 x 390 view's shape.
      check(GroupBoard.presentScale(760, 510, 6, 2, true) == 1, "the wheel fills the width already");
      check(GroupBoard.presentScale(1520, 780, 6, 2, true) == 2, "the wheel on a screen twice as big");

      // Driving the board: F presents, Esc and the Back button go back, the chart is for the teacher's screen.
      List<Boolean> calls = new ArrayList<>();
      PartnerHistory history = new PartnerHistory();
      List<String> names = GroupMaker.parseNames("Alice,Bob,Carol,Dave,Erin,Frank,Grace,Heidi,Ivan,Judy,Ken,Lena");
      List<List<String>> round = new ArrayList<>();
      for (int i = 0; i < names.size(); i += 2) {
         round.add(new ArrayList<>(names.subList(i, i + 2)));
      }
      history.add(round);
      GroupBoard board = new GroupBoard(history, new PickBag(), new ClassRules(), new Stub(calls, names));
      board.setSize(760, 510);
      board.showRound(names, false);
      paint(board);
      key(board, 'f', KeyEvent.VK_F);
      check(calls.equals(Collections.singletonList(true)), "F presents");
      paint(board);
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_ESCAPE);
      check(calls.size() == 2 && !calls.get(1), "Esc goes back");
      key(board, 'f', KeyEvent.VK_F);
      paint(board);
      // The Back button sits at the header's right, drawn twice the size: at about (720, 50) on the board.
      click(board, 720, 50);
      check(calls.size() == 4 && !calls.get(3), "the Back button goes back");
      key(board, 'f', KeyEvent.VK_F);
      paint(board);
      key(board, 'c', KeyEvent.VK_C);
      check(calls.size() == 6 && !calls.get(5), "the partner chart leaves Present");
      key(board, 'f', KeyEvent.VK_F);
      check(calls.size() == 6, "no Present from the partner chart");
      System.out.println("PresentCheck: all checks passed");
   }

   private static void paint(GroupBoard board) {
      BufferedImage image = new BufferedImage(board.getWidth(), board.getHeight(), BufferedImage.TYPE_INT_RGB);
      board.paint(image.createGraphics());
   }

   private static void key(GroupBoard board, char c, int code) {
      for (java.awt.event.KeyListener listener : board.getKeyListeners()) {
         listener.keyPressed(new KeyEvent(board, KeyEvent.KEY_PRESSED, 0, 0, code, c));
      }
   }

   private static void click(GroupBoard board, int x, int y) {
      for (java.awt.event.MouseListener listener : board.getMouseListeners()) {
         listener.mousePressed(new MouseEvent(board, MouseEvent.MOUSE_PRESSED, 0, 0, x, y, 1, false));
      }
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }

   /** Records each Present on or off; the rest does nothing. */
   private static final class Stub implements GroupBoard.Actions {
      private final List<Boolean> calls;
      private final List<String> names;

      Stub(List<Boolean> calls, List<String> names) {
         this.calls = calls;
         this.names = names;
      }

      public void present(boolean on) {
         calls.add(on);
      }

      public void newRound() {
      }

      public void reshuffle() {
      }

      public void clearHistory() {
      }

      public void edited(List<List<String>> groups) {
      }

      public void reuse(List<List<String>> round) {
      }

      public List<String> classNames() {
         return names;
      }

      public void picksChanged() {
      }

      public void rulesChanged() {
      }

      public void rolesShown(boolean shown) {
      }

      public void workChanged() {
      }
   }
}
