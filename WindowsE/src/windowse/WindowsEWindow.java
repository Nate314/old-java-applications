package windowse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A keyboard of Windows shortcuts: pick a key (Win+E, as ever, or Win+D, Win+R, Win+Tab, the Windows key alone or the
 * snapping arrows), say how many times, and DO IT!!! presses it that many times. Add step puts the pick on a list
 * instead, and DO IT!!! then presses the whole list in order. While it runs the button is Stop. A little screen
 * beside the keyboard shows what that will do, and Preview plays it there without pressing any key. Send copies a
 * link (on the desktop, a code) that opens the window with the same list.
 */
public final class WindowsEWindow extends JFrame {
   private static final String TITLE = "Windows+E";
   static final int MAX_STEPS = 8;
   /** Two File Explorer windows, snapped to the two halves of the screen. */
   static final List<Step> SIDE_BY_SIDE = Arrays.asList(new Step(Shortcut.EXPLORER, 1), new Step(Shortcut.LEFT, 1),
      new Step(Shortcut.EXPLORER, 1), new Step(Shortcut.RIGHT, 1));
   /** Where Preview's presses go: nowhere. */
   private static final Keys NO_KEYS = new Keys() {
      @Override
      public void press(int keyCode) {
      }

      @Override
      public void release(int keyCode) {
      }
   };
   private final Keys keys;
   /** Narrower than the full keyboard fits (a phone, in the browser): the pocket pad. */
   static final int POCKET_BELOW = 880;
   private final ShortcutBoard board;
   private final JTextField countField = new JTextField("10");
   private final JButton sideBySideButton = new JButton("Side by side");
   private final JButton addButton = new JButton("Add step");
   private final JButton clearButton = new JButton("Clear");
   private final JButton sendButton = new JButton("Send");
   private final JButton previewButton = new JButton("Preview");
   private final JButton doItButton = new JButton("DO IT!!!");
   private final List<Step> steps = new ArrayList<>();
   /** Puts text on the clipboard (the check passes one that only keeps it). */
   private final Consumer<String> copy;
   /** The page's address in the browser, without its query; null on the desktop. */
   private final String page;
   private Shortcut shortcut;
   private WindowsEPresser presser;
   /** The count is the one the picked key offered, untouched: the next digit typed replaces it. */
   private boolean freshCount;

   /** page: see {@link #page}; friend: the list a friend's link or code brought, or null. */
   WindowsEWindow(Keys keys, Consumer<String> copy, String page, List<Step> friend) {
      this(keys, copy, page, friend, Toolkit.getDefaultToolkit().getScreenSize().width < POCKET_BELOW);
   }

   WindowsEWindow(Keys keys, Consumer<String> copy, String page, List<Step> friend, boolean pocket) {
      this.keys = keys;
      this.copy = copy;
      this.page = page;
      this.board = new ShortcutBoard(this::pick, this::pickAndPress, pocket, this.countField);
      this.setTitle(TITLE);
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      this.countField.setColumns(5);
      this.countField.setFont(new Font("SansSerif", Font.BOLD, 16));
      this.countField.setToolTipText("How many times DO IT!!! presses the picked key, or Add step puts it on the list");
      this.countField.getAccessibleContext().setAccessibleName("How many times");
      this.countField.addActionListener(event -> this.doIt(false));
      // The real keyboard plays the drawn one: E, D or R picks that key, a number is the count, Enter presses it.
      KeyAdapter typing = new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            if (WindowsEWindow.this.presser == null) {
               WindowsEWindow.this.board.echo(event.getKeyCode());
            }
         }

         @Override
         public void keyReleased(KeyEvent event) {
            WindowsEWindow.this.typed(event);
         }
      };
      this.countField.addKeyListener(typing);
      this.board.listen(typing);
      this.sideBySideButton.setToolTipText("Lists Win+E, Win+Left, Win+E, Win+Right: two File Explorer windows, one on each half");
      this.sideBySideButton.addActionListener(event -> this.setSteps(SIDE_BY_SIDE));
      this.addButton.setToolTipText("Puts the picked key on the list; DO IT!!! then presses the list in order");
      this.addButton.addActionListener(event -> this.addStep());
      this.clearButton.addActionListener(event -> this.setSteps(Collections.emptyList()));
      this.sendButton.setToolTipText(page == null ? "Copies a code for this list: a friend opens it with ./run.ps1 old WindowsE and the code"
         : "Copies a link to this list: a friend who opens it gets the same list on their pretend Windows");
      this.sendButton.addActionListener(event -> this.send());
      this.previewButton.setToolTipText("Plays it on the little screen first, without pressing any key");
      this.previewButton.addActionListener(event -> this.doIt(true));
      this.doItButton.addActionListener(event -> this.doIt(false));
      // The one button that presses keys stands out in the keys' blue; the rest are about the list.
      this.doItButton.setBackground(KeyCap.ACCENT);
      this.doItButton.setForeground(Color.WHITE);
      this.doItButton.setFont(new Font("SansSerif", Font.BOLD, 15));

      // The list's buttons on the left, Preview and DO IT!!! on the right (pocket: on a row of their own, under them).
      JPanel list = buttonRow(FlowLayout.LEFT);
      list.add(this.addButton);
      list.add(this.clearButton);
      list.add(this.sideBySideButton);
      JPanel go = buttonRow(FlowLayout.RIGHT);
      (pocket ? go : list).add(this.sendButton);
      go.add(this.previewButton);
      go.add(this.doItButton);
      JPanel row = new JPanel(new BorderLayout());
      row.add(list, pocket ? BorderLayout.NORTH : BorderLayout.WEST);
      row.add(go, pocket ? BorderLayout.SOUTH : BorderLayout.EAST);
      row.setBackground(list.getBackground());
      this.add(this.board, BorderLayout.CENTER);
      this.add(row, BorderLayout.SOUTH);
      this.pick(Shortcut.EXPLORER);
      this.setSteps(friend != null ? friend : Collections.emptyList());
      if (friend != null) {
         this.setTitle(TITLE + ": a friend sent this list. Preview it, then DO IT!!!");
      }
      this.pack();
   }

   private static JPanel buttonRow(int align) {
      JPanel row = new JPanel(new FlowLayout(align, 8, 6));
      row.setBackground(new Color(243, 245, 249));
      return row;
   }

   private void pick(Shortcut shortcut) {
      if (this.presser != null || shortcut == this.shortcut) {
         return;
      }
      this.shortcut = shortcut;
      this.freshCount = true;
      this.countField.setText(String.valueOf(shortcut.times));
      this.board.pick(shortcut);
      this.planChanged();
   }

   /**
    * A key let go on the real keyboard, in the count field or on a drawn key: the drawn key with that letter goes down,
    * E, D or R picks its shortcut, and the field keeps only digits. The first digit after a pick replaces the count it
    * offered, so E 5 Enter presses Win+E five times.
    */
   private void typed(KeyEvent event) {
      if (this.presser != null) {
         return;
      }
      int code = event.getKeyCode();
      this.board.letGo();
      Shortcut letter = Shortcut.forLetter(code);
      if (letter != null) {
         this.pick(letter);
         this.freshCount = true;
      } else if (code == KeyEvent.VK_BACK_SPACE || code == KeyEvent.VK_DELETE || code == KeyEvent.VK_LEFT
         || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_HOME || code == KeyEvent.VK_END) {
         this.freshCount = false;   // editing the count by hand: digits go where the caret is
      }
      int digit = code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9 ? code - KeyEvent.VK_0
         : code >= KeyEvent.VK_NUMPAD0 && code <= KeyEvent.VK_NUMPAD9 ? code - KeyEvent.VK_NUMPAD0 : -1;
      String text = this.countField.getText();
      String next = countAfter(text, this.freshCount ? digit : -1);
      if (digit >= 0) {
         this.freshCount = false;
         if (event.getSource() != this.countField) {   // typed on a drawn key: the count goes on in the field
            next = String.valueOf(digit);
            this.countField.requestFocusInWindow();
         }
      }
      if (!next.equals(text)) {
         this.countField.setText(next);
      }
      this.planChanged();
   }

   /** The field's text after a key: only its digits, or just the digit typed when it replaces a picked key's count (-1: none). */
   static String countAfter(String text, int replacingDigit) {
      return replacingDigit >= 0 ? String.valueOf(replacingDigit) : text.replaceAll("[^0-9]", "");
   }

   /** A double-clicked key: picked, then pressed as many times as the field says. */
   private void pickAndPress(Shortcut shortcut) {
      if (this.presser == null) {
         this.pick(shortcut);
         this.doIt(false);
      }
   }

   /** The field's count, or 0 when it is not a whole number of 1 or more (the title then says so). */
   private int count() {
      int count = parseCount(this.countField.getText());
      this.setTitle(count > 0 ? TITLE : TITLE + (count == 0 ? ": enter 1 or more" : ": enter a whole number"));
      return Math.max(0, count);
   }

   /** The count typed: -1 when it is not a whole number. */
   static int parseCount(String text) {
      try {
         return Math.max(0, Integer.parseInt(text.trim()));
      } catch (NumberFormatException e) {
         return -1;
      }
   }

   /** What DO IT!!! presses now: the list, else the picked key as many times as the field says (none if it is not a count). */
   private List<Step> plan() {
      int count = parseCount(this.countField.getText());
      return !this.steps.isEmpty() ? this.steps : count > 0 ? Collections.singletonList(new Step(this.shortcut, count))
         : Collections.emptyList();
   }

   private void planChanged() {
      this.setTitle(TITLE);
      this.board.plan(this.plan());
      this.idle();
   }

   private void addStep() {
      int count = this.count();
      if (count > 0 && this.steps.size() < MAX_STEPS) {
         List<Step> more = new ArrayList<>(this.steps);
         more.add(new Step(this.shortcut, count));
         this.setSteps(more);
      }
   }

   private void setSteps(List<Step> steps) {
      if (this.presser != null) {
         return;
      }
      this.steps.clear();
      this.steps.addAll(steps);
      this.board.steps(this.steps);
      this.planChanged();
   }

   /** The list (or the picked key and its count) as a link to this page, or a code on the desktop, on the clipboard. */
   private void send() {
      List<Step> plan = this.plan();
      if (plan.isEmpty()) {
         this.count();   // the title says what is wrong with the count
         return;
      }
      String code = Step.code(plan);
      String text = this.page == null ? code : this.page + "?keys=" + code;
      try {
         this.copy.accept(text);
         this.setTitle(TITLE + (this.page == null ? ": code " + code + " copied" : ": link copied, send it to a friend"));
      } catch (RuntimeException e) {
         this.setTitle(TITLE + ": send this: " + text);   // headless, or the clipboard is busy
      }
   }

   /**
    * DO IT!!! presses the list, or the picked key when the list is empty; while that runs, the same button stops it.
    * dry (Preview): the same presses, on the little screen only.
    */
   private void doIt(boolean dry) {
      if (this.presser != null) {
         this.presser.stop();
         return;
      }
      List<Step> run = this.steps;
      if (run.isEmpty()) {
         int count = this.count();
         if (count == 0) {
            return;
         }
         run = Collections.singletonList(new Step(this.shortcut, count));
      }
      this.doItButton.setText("Stop");
      this.doItButton.getAccessibleContext().setAccessibleName("Stop");
      for (JButton b : new JButton[] {this.sideBySideButton, this.addButton, this.clearButton, this.sendButton, this.previewButton}) {
         b.setEnabled(false);
      }
      this.countField.setEnabled(false);
      this.board.started(run, dry);
      this.presser = new WindowsEPresser(dry ? NO_KEYS : this.keys, run, this.board::tapped, this::finished);
      this.presser.start();
   }

   private void finished() {
      this.presser = null;
      this.board.stopped();
      this.doItButton.setText("DO IT!!!");
      this.countField.setEnabled(true);
      this.sideBySideButton.setEnabled(true);
      this.sendButton.setEnabled(true);
      this.previewButton.setEnabled(true);
      this.idle();
   }

   /** The buttons that depend on the list, as it is now. */
   private void idle() {
      this.addButton.setEnabled(this.steps.size() < MAX_STEPS);
      this.clearButton.setEnabled(!this.steps.isEmpty());
      this.doItButton.getAccessibleContext().setAccessibleName((this.steps.isEmpty() ? "DO IT!!!"
         : "DO IT!!! presses the " + this.steps.size() + " steps: " + labels(this.steps)) + ". After: " + this.board.afterSummary());
      List<Step> plan = this.plan();
      this.doItButton.setToolTipText(plan.isEmpty() ? null : "Presses " + labels(plan) + (this.page == null ? " on the real keyboard" : ""));
   }

   static String labels(List<Step> steps) {
      StringBuilder s = new StringBuilder();
      for (Step step : steps) {
         s.append(s.length() == 0 ? "" : ", ").append(step.label());
      }
      return s.toString();
   }
}
