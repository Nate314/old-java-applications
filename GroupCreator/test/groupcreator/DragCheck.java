package groupcreator;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Self-check for dragging a name onto another: the swap, the trade of roles in one group, a drop on no name, and clicks. */
public class DragCheck {
   private static final List<String> NAMES = GroupMaker.parseNames("Alice,Bob,Carol,Dave,Erin,Frank");
   private static PartnerHistory history;
   private static GroupBoard board;
   private static int edits;

   public static void main(String[] args) {
      reset();
      // Alice (Group 1) dragged onto Carol (Group 2): they swap, and the header says so on the way.
      press(0, 0);
      drag(0, 0, 1, 0);
      check(board.dragLine().equals("Drop to swap Alice into Group 2 and Carol into Group 1."), board.dragLine());
      release(1, 0);
      check(edits == 1, "one edit, not " + edits);
      check(groups().equals("[[Carol, Bob], [Alice, Dave], [Erin, Frank]]"), "Alice and Carol swapped: " + groups());

      // In one group the two trade places, and so roles: Alice was Leader, Bob Scribe.
      reset();
      board.roles = true;
      press(0, 0);
      drag(0, 0, 0, 1);
      check(board.dragLine().equals("Drop to trade roles: Alice becomes Scribe, Bob Leader."), board.dragLine());
      release(0, 1);
      check(groups().equals("[[Bob, Alice], [Carol, Dave], [Erin, Frank]]"), "Alice and Bob trade places: " + groups());

      // Dropped on the desk: nothing changes, and nothing stays picked (a click on another group's name only picks it).
      reset();
      press(0, 0);
      Rectangle a = board.chipBounds(0, 0);
      mouse(MouseEvent.MOUSE_DRAGGED, a.x + 5, 2);
      check(board.dragLine().startsWith("Drop Alice on a name to swap them"), board.dragLine());
      mouse(MouseEvent.MOUSE_RELEASED, a.x + 5, 2);
      paint();
      check(edits == 0, "a drop on no name changes nothing");
      press(1, 0);
      release(1, 0);
      check(edits == 0, "and lets Alice go");

      // A press that barely moves is a click: it picks, and a click in another group swaps, as before.
      reset();
      press(0, 1);
      Rectangle b = board.chipBounds(0, 1);
      mouse(MouseEvent.MOUSE_DRAGGED, b.x + 13, b.y + 7);
      mouse(MouseEvent.MOUSE_RELEASED, b.x + 13, b.y + 7);
      check(edits == 0, "a small wobble is not a drag");
      press(2, 1);
      release(2, 1);
      check(groups().equals("[[Alice, Frank], [Carol, Dave], [Erin, Bob]]"), "click, click swaps Bob and Frank: " + groups());

      // A second click on the picked name lets it go.
      reset();
      press(0, 0);
      release(0, 0);
      press(0, 0);
      release(0, 0);
      press(1, 0);
      release(1, 0);
      check(edits == 0, "a second click lets go");

      // An earlier round is only looked at: a drag there changes nothing.
      reset();
      history.add(copy());
      board.showRound(NAMES, false);
      paint();
      key(KeyEvent.VK_LEFT);
      paint();
      press(0, 0);
      drag(0, 0, 1, 0);
      release(1, 0);
      check(edits == 0, "no drag in an earlier round");
      System.out.println("DragCheck: all checks passed");
   }

   private static void reset() {
      history = new PartnerHistory();
      history.add(Arrays.asList(new ArrayList<>(NAMES.subList(0, 2)), new ArrayList<>(NAMES.subList(2, 4)), new ArrayList<>(NAMES.subList(4, 6))));
      board = new GroupBoard(history, new PickBag(), new ClassRules(), new Stub());
      board.roles = false;
      board.setSize(760, 440);
      board.showRound(NAMES, false);
      edits = 0;
      paint();
   }

   private static List<List<String>> copy() {
      return PartnerHistory.copy(history.last());
   }

   private static String groups() {
      return history.last().toString();
   }

   private static void paint() {
      BufferedImage image = new BufferedImage(board.getWidth(), board.getHeight(), BufferedImage.TYPE_INT_RGB);
      board.paint(image.createGraphics());
   }

   private static void press(int g, int k) {
      Rectangle chip = board.chipBounds(g, k);
      mouse(MouseEvent.MOUSE_PRESSED, chip.x + 10, chip.y + 5);
   }

   /** Drags from name k of group g to the middle of name k2 of group g2, in steps, painting as it goes. */
   private static void drag(int g, int k, int g2, int k2) {
      Rectangle from = board.chipBounds(g, k);
      Rectangle to = board.chipBounds(g2, k2);
      for (int step = 1; step <= 4; step++) {
         int x = from.x + 10 + (to.x + to.width / 2 - from.x - 10) * step / 4;
         int y = from.y + 5 + (to.y + to.height / 2 - from.y - 5) * step / 4;
         mouse(MouseEvent.MOUSE_DRAGGED, x, y);
         paint();
      }
   }

   private static void release(int g, int k) {
      Rectangle chip = board.chipBounds(g, k);
      mouse(MouseEvent.MOUSE_RELEASED, chip.x + chip.width / 2, chip.y + chip.height / 2);
      paint();
   }

   private static void mouse(int id, int x, int y) {
      MouseEvent event = new MouseEvent(board, id, 0, 0, x, y, 1, false);
      if (id == MouseEvent.MOUSE_DRAGGED) {
         for (java.awt.event.MouseMotionListener listener : board.getMouseMotionListeners()) {
            listener.mouseDragged(event);
         }
         return;
      }
      for (java.awt.event.MouseListener listener : board.getMouseListeners()) {
         if (id == MouseEvent.MOUSE_PRESSED) {
            listener.mousePressed(event);
         } else {
            listener.mouseReleased(event);
         }
      }
   }

   private static void key(int code) {
      for (java.awt.event.KeyListener listener : board.getKeyListeners()) {
         listener.keyPressed(new KeyEvent(board, KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED));
      }
   }

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }

   /** Keeps an edited round as the window does; the rest does nothing. */
   private static final class Stub implements GroupBoard.Actions {
      public void edited(List<List<String>> groups) {
         edits++;
         history.replaceLast(groups);
         board.showRound(NAMES, false);
      }

      public void present(boolean on) {
      }

      public void newRound() {
      }

      public void reshuffle() {
      }

      public void clearHistory() {
      }

      public void reuse(int from) {
      }

      public List<String> classNames() {
         return NAMES;
      }

      public void picksChanged() {
      }

      public void rulesChanged() {
      }

      public void rolesShown(boolean shown) {
      }

      public void workChanged() {
      }

      public int[] sizes() {
         return new int[] {2, 2};
      }
   }
}
