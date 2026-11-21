package groupcreator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The window: the original names, min and max fields and button across the top, and the {@link GroupBoard} under
 * them. Each round is mixed by {@link FreshMix} against every round before it, kept with the class in
 * {@code ~/.groupcreator-class.txt}, and written to {@code output.txt} as before. Names marked away in the roll call
 * sit a round out, keep-apart rules hold whenever the group sizes allow, and {@link Roles} hands out the roles.
 */
public class Main extends JFrame implements GroupBoard.Actions {
   private final JTextField textField = new JTextField();
   private final JTextField minTextField = new JTextField();
   private final JTextField maxTextField = new JTextField();
   private final Roster roster;
   private final GroupBoard board;
   private final Random random = new Random();
   private final JPanel inputs = new JPanel(new BorderLayout());
   /** Screens narrower than this get the phone layout: the window as wide as the screen, the board laid out to fit. */
   static final int NARROW = 600;
   /** Where the window was before Present filled the screen. */
   private Rectangle windowed;

   public Main(Boolean visibility) {
      this.setDefaultCloseOperation(3);
      this.setTitle("Group Creator");
      this.setResizable(false);
      // Read after the window exists, so the browser build has put the saved file back.
      roster = Roster.load(new File(System.getProperty("user.home"), ".groupcreator-class.txt"));
      board = new GroupBoard(roster.history, roster.picks, roster.rules, this);
      board.roles = roster.roles;
      board.work = roster.work;

      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      // A phone held upright (in the browser): the window takes the screen's width instead of being scaled down.
      boolean narrow = screen.width < NARROW;
      JPanel names = new JPanel();
      JLabel namesLabel = new JLabel("Names, separated by commas:");
      textField.setColumns(44);
      textField.setText(roster.names);
      if (narrow) {
         // The label over the field, and the field as wide as the screen leaves.
         names.setLayout(new BorderLayout());
         JPanel label = new JPanel(new FlowLayout(FlowLayout.LEFT));
         label.add(namesLabel);
         names.add(label, BorderLayout.NORTH);
         while (textField.getColumns() > 10 && textField.getPreferredSize().width > screen.width - 24) {
            textField.setColumns(textField.getColumns() - 1);
         }
         JPanel field = new JPanel();
         field.add(textField);
         names.add(field, BorderLayout.CENTER);
      } else {
         names.add(namesLabel);
         names.add(textField);
      }
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
      // The board previews round 1 from the fields, so it follows every edit in them: keys, a paste or cut with the mouse.
      DocumentListener edited = new DocumentListener() {
         @Override
         public void insertUpdate(DocumentEvent event) {
            board.repaint();
         }

         @Override
         public void removeUpdate(DocumentEvent event) {
            board.repaint();
         }

         @Override
         public void changedUpdate(DocumentEvent event) {
            board.repaint();
         }
      };
      textField.getDocument().addDocumentListener(edited);
      minTextField.getDocument().addDocumentListener(edited);
      maxTextField.getDocument().addDocumentListener(edited);
      sizes.add(new JLabel("per group, min: "));
      sizes.add(minTextField);
      sizes.add(new JLabel("max: "));
      sizes.add(maxTextField);
      sizes.add(button);
      inputs.add(names, BorderLayout.NORTH);
      inputs.add(sizes, BorderLayout.SOUTH);
      this.add(inputs, BorderLayout.NORTH);
      this.add(board, BorderLayout.CENTER);
      board.showRound(GroupMaker.parseNames(roster.names), false);
      board.dealtAgain(roster.again);
      this.pack();
      if (narrow) {
         // At least 340 x 560: a smaller screen scales the window down as before.
         setBounds(0, 0, Math.max(340, screen.width), Math.max(560, screen.height));
      } else {
         this.setLocationRelativeTo(null);
      }
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
      List<String> typed = GroupMaker.parseNames(textField.getText());
      // Names marked away in the roll call sit this round out.
      List<String> names = roster.rules.present(typed);
      if (names.isEmpty() && !typed.isEmpty()) {
         board.showMessage("Everyone is marked away. Mark who is here in Roll call (A).");
         return;
      }
      PartnerHistory past = replace ? roster.history.first(roster.history.size() - 1) : roster.history;
      List<List<String>> groups;
      try {
         groups = Roles.rotate(FreshMix.mix(names, min, max, past, roster.rules, random), past, random);
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
      roster.again = 0;
      String problem = save(groups);
      board.showRound(typed, true);
      if (problem != null) {
         board.showMessage(problem);
      }
      board.requestFocusInWindow();
   }

   @Override
   public void reuse(int from) {
      List<String> typed = GroupMaker.parseNames(textField.getText());
      List<String> here = roster.rules.present(typed);
      if (here.isEmpty()) {
         board.showMessage(typed.isEmpty() ? "Type the class in the names field first."
            : "Everyone is marked away. Mark who is here in Roll call (A).");
         return;
      }
      // The same groups, with the roles rotated against every round so far.
      List<List<String>> groups = Roles.rotate(GroupMaker.again(roster.history.round(from), here), roster.history, random);
      roster.history.add(groups);
      roster.names = textField.getText().trim();
      roster.again = from;
      String problem = save(groups);
      board.showRound(typed, true);
      board.dealtAgain(from);
      if (problem != null) {
         board.showMessage(problem);
      }
      board.requestFocusInWindow();
   }

   @Override
   public void edited(List<List<String>> groups) {
      roster.history.replaceLast(groups);
      roster.again = 0;
      String problem = save(groups);
      board.showRound(GroupMaker.parseNames(roster.names), false);
      if (problem != null) {
         board.showMessage(problem);
      }
   }

   @Override
   public void clearHistory() {
      roster.history.clear();
      roster.again = 0;
      save(null);
      board.showRound(GroupMaker.parseNames(roster.names), false);
   }

   @Override
   public int[] sizes() {
      try {
         return new int[] {Integer.parseInt(minTextField.getText().trim()), Integer.parseInt(maxTextField.getText().trim())};
      } catch (NumberFormatException ex) {
         return null;
      }
   }

   @Override
   public List<String> classNames() {
      return GroupMaker.parseNames(textField.getText());
   }

   @Override
   public void rolesShown(boolean shown) {
      roster.roles = shown;
      picksChanged();
   }

   @Override
   public void workChanged() {
      picksChanged();
   }

   @Override
   public void present(boolean on) {
      inputs.setVisible(!on);
      if (on) {
         windowed = getBounds();
         Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
         setBounds(0, 0, screen.width, screen.height);
      } else if (windowed != null) {
         setBounds(windowed);
      }
      validate();
      board.requestFocusInWindow();
   }

   @Override
   public void rulesChanged() {
      picksChanged();
   }

   @Override
   public void picksChanged() {
      roster.names = textField.getText().trim();
      String problem = save(null);
      if (problem != null) {
         board.showMessage(problem);
      }
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
            writeOutput(GroupMaker.describe(groups, roster.roles));
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
