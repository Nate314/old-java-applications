package groupcreator;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Self-check for Look back: an earlier round's groups again for the class here now, and stepping through rounds. */
public class LookBackCheck {
   public static void main(String[] args) throws IOException {
      List<List<String>> round = groups("Alice,Bob", "Carol,Dave", "Erin,Frank");
      check(GroupMaker.again(round, names("Alice,Bob,Carol,Dave,Erin,Frank")).equals(round), "the same class gets the same groups");
      check(GroupMaker.again(round, names("Bob,Carol,Dave,Erin,Frank")).equals(groups("Bob", "Carol,Dave", "Erin,Frank")),
         "a name not here leaves its group");
      check(GroupMaker.again(round, names("Alice,Bob,Erin,Frank")).equals(groups("Alice,Bob", "Erin,Frank")), "an emptied group goes");
      check(GroupMaker.again(round, names("Alice,Carol,Dave,Erin,Frank,Zoe")).equals(groups("Alice,Zoe", "Carol,Dave", "Erin,Frank")),
         "a newcomer joins the smallest group");
      check(GroupMaker.again(round, names("Zoe,Yuri")).equals(groups("Zoe,Yuri")), "a class the round never had gets one group");
      check(GroupMaker.again(round, names("Alice,Alice,Bob")).equals(groups("Alice,Bob")), "a name typed twice is placed once");

      // Driving the board: Left steps back, Use again hands over the round shown, Esc and Right come back.
      PartnerHistory history = new PartnerHistory();
      history.add(round);
      history.add(groups("Alice,Carol", "Bob,Erin", "Dave,Frank"));
      history.add(groups("Alice,Dave", "Bob,Frank", "Carol,Erin"));
      List<Integer> reused = new ArrayList<>();
      GroupBoard board = new GroupBoard(history, new PickBag(), new ClassRules(), new Stub(reused));
      board.setSize(760, 440);
      board.showRound(names("Alice,Bob,Carol,Dave,Erin,Frank"), false);
      paint(board);
      key(board, 'u', KeyEvent.VK_U);
      check(reused.isEmpty(), "U does nothing on the newest round");
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_LEFT);
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_LEFT);
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_LEFT);
      paint(board);
      key(board, 'u', KeyEvent.VK_U);
      check(reused.size() == 1 && reused.get(0) == 1, "three Lefts stop at round 1, and U uses it again");
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_RIGHT);
      key(board, 'u', KeyEvent.VK_U);
      check(reused.size() == 2 && reused.get(1) == 2, "Right steps forward to round 2");
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_ESCAPE);
      key(board, 'u', KeyEvent.VK_U);
      check(reused.size() == 2, "Esc goes back to the newest round");
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_LEFT);
      key(board, KeyEvent.CHAR_UNDEFINED, KeyEvent.VK_RIGHT);
      key(board, 'u', KeyEvent.VK_U);
      check(reused.size() == 2, "Right from the last round before the newest ends looking back");

      // The class file remembers which round the newest one dealt again, so its header survives a reload.
      File file = File.createTempFile("groupcreator", ".txt");
      file.deleteOnExit();
      Roster roster = Roster.load(file);
      roster.history.add(round);
      roster.history.add(groups("Alice,Carol", "Bob,Erin", "Dave,Frank"));
      roster.history.add(round);
      roster.again = 1;
      roster.save();
      check(roster.text().contains("again=1\n") && Roster.load(file).again == 1, "again= round trips through the class file");
      roster.again = 0;
      roster.save();
      check(!roster.text().contains("again=") && Roster.load(file).again == 0, "a plain newest round writes no again= line");
      for (String bad : new String[] {"again=3", "again=0", "again=x"}) {
         Files.write(file.toPath(), (roster.text() + bad + "\n").getBytes(StandardCharsets.UTF_8));
         Roster read = Roster.load(file);
         check(read.again == 0 && read.history.size() == 3, bad + " is read as a plain newest round");
      }
      System.out.println("LookBackCheck: all checks passed");
   }

   private static List<String> names(String list) {
      return GroupMaker.parseNames(list);
   }

   private static List<List<String>> groups(String... lists) {
      List<List<String>> groups = new ArrayList<>();
      for (String list : lists) {
         groups.add(new ArrayList<>(Arrays.asList(list.split(","))));
      }
      return groups;
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

   private static void check(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }

   /** Records each round handed to Use again; the rest does nothing. */
   private static final class Stub implements GroupBoard.Actions {
      private final List<Integer> reused;

      Stub(List<Integer> reused) {
         this.reused = reused;
      }

      public void reuse(int from) {
         reused.add(from);
      }

      public void present(boolean on) {
      }

      public void newRound() {
      }

      public void reshuffle() {
      }

      public void clearHistory() {
      }

      public void edited(List<List<String>> groups) {
      }

      public List<String> classNames() {
         return GroupMaker.parseNames("Alice,Bob,Carol,Dave,Erin,Frank");
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
