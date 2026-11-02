package groupcreator;

import java.awt.BorderLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * The window: the original names, min and max fields and button across the top, and the {@link GroupBoard} under
 * them. Each round is mixed by {@link FreshMix} against every round before it, kept with the class in
 * {@code ~/.groupcreator-class.txt}, and written to {@code output.txt} as before.
 */
public class Main extends JFrame implements GroupBoard.Actions {
   private final JTextField textField = new JTextField();
   private final JTextField minTextField = new JTextField();
   private final JTextField maxTextField = new JTextField();
   private final Roster roster;
   private final GroupBoard board;
   private final Random random = new Random();

   public Main(Boolean visibility) {
      this.setDefaultCloseOperation(3);
      this.setTitle("Group Creator");
      this.setResizable(false);
      // Read after the window exists, so the browser build has put the saved file back.
      roster = Roster.load(new File(System.getProperty("user.home"), ".groupcreator-class.txt"));
      board = new GroupBoard(roster.history, this);

      JPanel names = new JPanel();
      names.add(new JLabel("Names, separated by commas:"));
      textField.setColumns(44);
      textField.setText(roster.names);
      names.add(textField);
      JPanel sizes = new JPanel();
      minTextField.setColumns(3);
      minTextField.setText(roster.min);
      maxTextField.setColumns(3);
      maxTextField.setText(roster.max);
      JButton button = new JButton("Make groups");
      button.addActionListener(event -> newRound());
      // Enter in any field makes groups too.
      textField.addActionListener(event -> newRound());
      minTextField.addActionListener(event -> newRound());
      maxTextField.addActionListener(event -> newRound());
      sizes.add(new JLabel("per group, min: "));
      sizes.add(minTextField);
      sizes.add(new JLabel("max: "));
      sizes.add(maxTextField);
      sizes.add(button);
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(names, BorderLayout.NORTH);
      panel.add(sizes, BorderLayout.SOUTH);
      this.add(panel, BorderLayout.NORTH);
      this.add(board, BorderLayout.CENTER);
      board.showRound(GroupMaker.parseNames(roster.names), false);
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(visibility);
   }

   @Override
   public void newRound() {
      make(false);
   }

   @Override
   public void reshuffle() {
      make(!roster.history.isEmpty());
   }

   /** Mixes a round against the rounds before it: a new one, or (replace) one in place of the newest. */
   private void make(boolean replace) {
      int min;
      int max;
      try {
         min = Integer.parseInt(minTextField.getText().trim());
         max = Integer.parseInt(maxTextField.getText().trim());
      } catch (NumberFormatException ex) {
         board.showMessage("min and max must be whole numbers.");
         return;
      }
      List<String> names = GroupMaker.parseNames(textField.getText());
      PartnerHistory past = replace ? roster.history.first(roster.history.size() - 1) : roster.history;
      List<List<String>> groups;
      try {
         groups = FreshMix.mix(names, min, max, past, random);
      } catch (IllegalArgumentException ex) {
         board.showMessage(ex.getMessage());
         return;
      }
      if (replace) {
         roster.history.replaceLast(groups);
      } else {
         roster.history.add(groups);
      }
      roster.names = textField.getText().trim();
      roster.min = String.valueOf(min);
      roster.max = String.valueOf(max);
      String problem = save(groups);
      board.showRound(names, true);
      if (problem != null) {
         board.showMessage(problem);
      }
      board.requestFocusInWindow();
   }

   @Override
   public void edited(List<List<String>> groups) {
      roster.history.replaceLast(groups);
      String problem = save(groups);
      board.showRound(GroupMaker.parseNames(roster.names), false);
      if (problem != null) {
         board.showMessage(problem);
      }
   }

   @Override
   public void clearHistory() {
      roster.history.clear();
      save(null);
      board.showRound(GroupMaker.parseNames(roster.names), false);
   }

   /** Saves the class, and the groups (when there are some) to output.txt. Returns what went wrong, or null. */
   private String save(List<List<String>> groups) {
      try {
         roster.save();
      } catch (IOException ex) {
         return "Could not save the class: " + ex.getMessage();
      }
      if (groups != null) {
         try {
            writeOutput(GroupMaker.describe(groups));
         } catch (IOException ex) {
            return "Could not write output.txt: " + ex.getMessage();
         }
      }
      return null;
   }

   private static void writeOutput(List<String> lines) throws IOException {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt"), "utf-8"))) {
         writer.write(String.join("\r\n", lines) + "\r\n");
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
