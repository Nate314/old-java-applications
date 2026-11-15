package windowse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
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
 * instead, and DO IT!!! then presses the whole list in order. While it runs the button is Stop.
 */
public final class WindowsEWindow extends JFrame {
   private static final String TITLE = "Windows+E";
   static final int MAX_STEPS = 8;
   /** Two File Explorer windows, snapped to the two halves of the screen. */
   static final List<Step> SIDE_BY_SIDE = Arrays.asList(new Step(Shortcut.EXPLORER, 1), new Step(Shortcut.LEFT, 1),
      new Step(Shortcut.EXPLORER, 1), new Step(Shortcut.RIGHT, 1));
   private final Keys keys;
   private final ShortcutBoard board = new ShortcutBoard(this::pick, this::pickAndPress);
   private final JTextField countField = new JTextField("10");
   private final JButton sideBySideButton = new JButton("Side by side");
   private final JButton addButton = new JButton("Add step");
   private final JButton clearButton = new JButton("Clear");
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
      this.countField.addActionListener(event -> this.doIt());
      this.sideBySideButton.setToolTipText("Lists Win+E, Win+Left, Win+E, Win+Right: two File Explorer windows, one on each half");
      this.sideBySideButton.addActionListener(event -> this.setSteps(SIDE_BY_SIDE));
      this.addButton.setToolTipText("Puts the picked key on the list; DO IT!!! then presses the list in order");
      this.addButton.addActionListener(event -> this.addStep());
      this.clearButton.addActionListener(event -> this.setSteps(Collections.emptyList()));
      this.doItButton.addActionListener(event -> this.doIt());

      JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
      row.setBackground(new Color(243, 245, 249));
      row.add(this.sideBySideButton);
      row.add(new JLabel("Press it"));
      row.add(this.countField);
      row.add(new JLabel("times"));
      row.add(this.addButton);
      row.add(this.clearButton);
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
   }

   /** A double-clicked key: picked, then pressed as many times as the field says. */
   private void pickAndPress(Shortcut shortcut) {
      if (this.presser == null) {
         this.pick(shortcut);
         this.doIt();
      }
   }

   /** The field's count, or 0 when it is not a whole number of 1 or more (the title then says so). */
   private int count() {
      int count;
      try {
         count = Integer.parseInt(this.countField.getText().trim());
      } catch (NumberFormatException e) {
         this.setTitle(TITLE + ": enter a whole number");
         return 0;
      }
      if (count < 1) {
         this.setTitle(TITLE + ": enter 1 or more");
         return 0;
      }
      this.setTitle(TITLE);
      return count;
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
      this.idle();
   }

   /** DO IT!!! presses the list, or the picked key when the list is empty; while that runs, the same button stops it. */
   private void doIt() {
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
      for (JButton b : new JButton[] {this.sideBySideButton, this.addButton, this.clearButton}) {
         b.setEnabled(false);
      }
      this.countField.setEnabled(false);
      this.board.started(run);
      this.presser = new WindowsEPresser(this.keys, run, this.board::tapped, this::finished);
      this.presser.start();
   }

   private void finished() {
      this.presser = null;
      this.board.stopped();
      this.doItButton.setText("DO IT!!!");
      this.countField.setEnabled(true);
      this.sideBySideButton.setEnabled(true);
      this.idle();
   }

   /** The buttons that depend on the list, as it is now. */
   private void idle() {
      this.addButton.setEnabled(this.steps.size() < MAX_STEPS);
      this.clearButton.setEnabled(!this.steps.isEmpty());
      this.doItButton.getAccessibleContext().setAccessibleName(this.steps.isEmpty() ? "DO IT!!!"
         : "DO IT!!! presses the " + this.steps.size() + " steps: " + labels(this.steps));
   }

   static String labels(List<Step> steps) {
      StringBuilder s = new StringBuilder();
      for (Step step : steps) {
         s.append(s.length() == 0 ? "" : ", ").append(step.label());
      }
      return s.toString();
   }
}
