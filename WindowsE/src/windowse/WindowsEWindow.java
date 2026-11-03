package windowse;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A keyboard of Windows shortcuts: pick a key (Win+E, as ever, or Win+D, Win+R, Win+Tab, the Windows key alone or the
 * snapping arrows), say how many times, and DO IT!!! presses it that many times. While it runs the button is Stop.
 */
public final class WindowsEWindow extends JFrame {
   private static final String TITLE = "Windows+E";
   private final Keys keys;
   private final ShortcutBoard board = new ShortcutBoard(this::pick, this::pickAndPress);
   private final JTextField countField = new JTextField("10");
   private final JButton doItButton = new JButton("DO IT!!!");
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
      this.doItButton.addActionListener(event -> this.doIt());

      JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
      row.setBackground(new Color(243, 245, 249));
      row.add(new JLabel("Press it"));
      row.add(this.countField);
      row.add(new JLabel("times"));
      row.add(this.doItButton);
      this.add(row, BorderLayout.SOUTH);   // added first, so the field has the keyboard when the window opens
      this.add(this.board, BorderLayout.CENTER);
      this.pick(Shortcut.EXPLORER);
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

   /** DO IT!!! presses the shortcut; while that runs, the same button stops it. */
   private void doIt() {
      if (this.presser != null) {
         this.presser.stop();
         return;
      }
      int count;
      try {
         count = Integer.parseInt(this.countField.getText().trim());
      } catch (NumberFormatException e) {
         this.setTitle(TITLE + ": enter a whole number");
         return;
      }
      if (count < 1) {
         this.setTitle(TITLE + ": enter 1 or more");
         return;
      }
      this.setTitle(TITLE);
      this.doItButton.setText("Stop");
      this.countField.setEnabled(false);
      this.board.started(count);
      this.presser = new WindowsEPresser(this.keys, this.shortcut, count, this.board::tapped, this::finished);
      this.presser.start();
   }

   private void finished() {
      this.presser = null;
      this.board.stopped();
      this.doItButton.setText("DO IT!!!");
      this.countField.setEnabled(true);
   }
}
