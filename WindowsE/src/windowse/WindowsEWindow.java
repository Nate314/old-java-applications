package windowse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A keyboard of Windows shortcuts: pick a key (Win+E, as ever, or Win+D, Win+R, Win+Tab, the Windows key alone or the
 * snapping arrows), say how many times, and DO IT!!! presses it that many times. Add step puts the pick on a list
 * instead, and DO IT!!! then presses the whole list in order. While it runs the button is Stop. A little screen
 * beside the keyboard shows what that will do, and Preview plays it there without pressing any key.
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
   private final ShortcutBoard board = new ShortcutBoard(this::pick, this::pickAndPress);
   private final JTextField countField = new JTextField("10");
   private final JButton sideBySideButton = new JButton("Side by side");
   private final JButton addButton = new JButton("Add step");
   private final JButton clearButton = new JButton("Clear");
   private final JButton previewButton = new JButton("Preview");
   private final JButton doItButton = new JButton("DO IT!!!");
   private final List<Step> steps = new ArrayList<>();
   private Shortcut shortcut;
   private WindowsEPresser presser;

   WindowsEWindow(Keys keys) {
      this.keys = keys;
      this.setTitle(TITLE);
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      this.countField.setColumns(5);
      this.countField.getAccessibleContext().setAccessibleName("How many times");
      this.countField.addActionListener(event -> this.doIt(false));
      this.countField.addKeyListener(new KeyAdapter() {
         @Override
         public void keyReleased(KeyEvent event) {
            if (WindowsEWindow.this.presser == null) {
               WindowsEWindow.this.planChanged();
            }
         }
      });
      this.sideBySideButton.setToolTipText("Lists Win+E, Win+Left, Win+E, Win+Right: two File Explorer windows, one on each half");
      this.sideBySideButton.addActionListener(event -> this.setSteps(SIDE_BY_SIDE));
      this.addButton.setToolTipText("Puts the picked key on the list; DO IT!!! then presses the list in order");
      this.addButton.addActionListener(event -> this.addStep());
      this.clearButton.addActionListener(event -> this.setSteps(Collections.emptyList()));
      this.previewButton.setToolTipText("Plays it on the little screen first, without pressing any key");
      this.previewButton.addActionListener(event -> this.doIt(true));
      this.doItButton.addActionListener(event -> this.doIt(false));

      JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
      row.setBackground(new Color(243, 245, 249));
      row.add(this.sideBySideButton);
      row.add(new JLabel("Press it"));
      row.add(this.countField);
      row.add(new JLabel("times"));
      row.add(this.addButton);
      row.add(this.clearButton);
      row.add(this.previewButton);
      row.add(this.doItButton);
      this.add(row, BorderLayout.SOUTH);   // added first, so the field has the keyboard when the window opens
      this.add(this.board, BorderLayout.CENTER);
      this.pick(Shortcut.EXPLORER);
      this.setSteps(Collections.emptyList());
      this.pack();
   }

   private void pick(Shortcut shortcut) {
      if (this.presser != null || shortcut == this.shortcut) {
         return;
      }
      this.shortcut = shortcut;
      this.countField.setText(String.valueOf(shortcut.times));
      this.board.pick(shortcut);
      this.planChanged();
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
      for (JButton b : new JButton[] {this.sideBySideButton, this.addButton, this.clearButton, this.previewButton}) {
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
      this.previewButton.setEnabled(true);
      this.idle();
   }

   /** The buttons that depend on the list, as it is now. */
   private void idle() {
      this.addButton.setEnabled(this.steps.size() < MAX_STEPS);
      this.clearButton.setEnabled(!this.steps.isEmpty());
      this.doItButton.getAccessibleContext().setAccessibleName((this.steps.isEmpty() ? "DO IT!!!"
         : "DO IT!!! presses the " + this.steps.size() + " steps: " + labels(this.steps)) + ". After: " + this.board.afterSummary());
   }

   static String labels(List<Step> steps) {
      StringBuilder s = new StringBuilder();
      for (Step step : steps) {
         s.append(s.length() == 0 ? "" : ", ").append(step.label());
      }
      return s.toString();
   }
}
