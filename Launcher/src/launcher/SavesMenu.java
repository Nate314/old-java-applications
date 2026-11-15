package launcher;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * The launcher's Saves button: moves the apps' records between this computer and the web site's "Your records"
 * through one file (see {@link SaveBundle}). Save writes the desktop's records to a file the site's Load reads;
 * Load reads a file from the site's Download (or from another computer) and asks which apps' records to take.
 */
final class SavesMenu {
   static final String FILE_NAME = "oldjava-saves.json";

   private SavesMenu() {
   }

   static void open(Component parent, Path home, PlayLog log, Function<String, String> nameOf, Consumer<String> status) {
      Object[] options = {"Load a file...", "Save a file...", "Cancel"};
      int pick = JOptionPane.showOptionDialog(parent,
         "<html><body style='width:340px'>Take your records (best times, scores, stars, the timer, the trading desk) "
            + "between these desktop apps and the web site.<br><br><b>Load</b> a file from the site's <i>Your records</i> "
            + "&gt; Download (or drop it on the launcher window), and pick which apps take its records.<br><b>Save</b> this computer's records as "
            + FILE_NAME + ", then use <i>Your records</i> &gt; Load on the site.</body></html>",
         "Saves", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
      if (pick == 0) load(parent, home, nameOf, status);
      else if (pick == 1) save(parent, home, log, status);
   }

   private static JFileChooser chooser(Path home) {
      Path downloads = home.resolve("Downloads");
      JFileChooser c = new JFileChooser(Files.isDirectory(downloads) ? downloads.toFile() : home.toFile());
      c.setFileFilter(new FileNameExtensionFilter("Old Java saves (*.json)", "json"));
      return c;
   }

   private static void save(Component parent, Path home, PlayLog log, Consumer<String> status) {
      SaveBundle bundle = SaveBundle.collect(home, log, Instant.now());
      if (bundle.saves.isEmpty()) {
         status.accept("No records to save yet: the desktop apps have not saved any in " + home + ".");
         return;
      }
      JFileChooser c = chooser(home);
      c.setSelectedFile(new File(c.getCurrentDirectory(), FILE_NAME));
      if (c.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
      Path file = c.getSelectedFile().toPath();
      try {
         Files.write(file, bundle.toJson().getBytes(StandardCharsets.UTF_8));
         status.accept("Saved the records of " + bundle.saves.size() + " apps to " + file
            + ". On the site, Your records > Load reads it.");
      } catch (IOException e) {
         status.accept("Could not save " + file + ": " + e.getMessage());
      }
   }

   /** Takes a saves file dropped on the window (from the site's Download, say) the way Load a file... does. */
   static TransferHandler drops(Component parent, Path home, Function<String, String> nameOf, Consumer<String> status) {
      return new TransferHandler() {
         @Override
         public boolean canImport(TransferSupport s) {
            return s.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
         }

         @Override
         public boolean importData(TransferSupport s) {
            if (!canImport(s)) return false;
            try {
               List<?> files = (List<?>) s.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
               if (files.isEmpty()) return false;
               Path file = ((File) files.get(0)).toPath();
               // Later, so the drag source (Explorer) is not held while the dialog waits.
               SwingUtilities.invokeLater(() -> load(parent, file, home, nameOf, status));
               return true;
            } catch (Exception e) {
               status.accept("Could not take the dropped file: " + e.getMessage());
               return false;
            }
         }
      };
   }

   private static void load(Component parent, Path home, Function<String, String> nameOf, Consumer<String> status) {
      JFileChooser c = chooser(home);
      if (c.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return;
      load(parent, c.getSelectedFile().toPath(), home, nameOf, status);
   }

   static void load(Component parent, Path file, Path home, Function<String, String> nameOf, Consumer<String> status) {
      SaveBundle bundle;
      try {
         bundle = SaveBundle.parse(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
      } catch (IOException | IllegalArgumentException e) {
         status.accept("Could not read " + file.getFileName() + ": " + e.getMessage());
         return;
      }
      Map<String, List<String>> usable = bundle.usable();
      if (usable.isEmpty()) {
         status.accept(file.getFileName() + " has no records the desktop apps keep.");
         return;
      }
      JPanel list = new JPanel();
      list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
      list.add(new JLabel("<html><body style='width:340px'>Each app you tick takes the records in " + file.getFileName()
         + " in place of the ones on this computer.</body></html>"));
      list.add(Box.createVerticalStrut(8));
      List<JCheckBox> boxes = new ArrayList<>();
      for (Map.Entry<String, List<String>> app : usable.entrySet()) {
         boolean differs = bundle.differs(app.getKey(), home);
         JCheckBox box = new JCheckBox(nameOf.apply(app.getKey()) + (differs ? "" : " (already the same here)"), differs);
         box.setEnabled(differs);
         box.setActionCommand(app.getKey());
         box.setToolTipText(String.join(", ", app.getValue()));
         boxes.add(box);
         list.add(box);
      }
      int ok = JOptionPane.showConfirmDialog(parent, list, "Bring these records here?", JOptionPane.OK_CANCEL_OPTION,
         JOptionPane.PLAIN_MESSAGE);
      if (ok != JOptionPane.OK_OPTION) return;
      List<String> chosen = new ArrayList<>();
      for (JCheckBox b : boxes) if (b.isSelected()) chosen.add(b.getActionCommand());
      try {
         List<String> written = bundle.apply(home, chosen);
         status.accept(written.isEmpty() ? "Nothing brought in."
            : "Brought in the records of " + chosen.size() + (chosen.size() == 1 ? " app" : " apps")
               + ". An app already running keeps its old ones until it is started again.");
      } catch (IOException e) {
         status.accept("Could not write the records: " + e.getMessage());
      }
   }
}
