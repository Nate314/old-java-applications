package groupcreator;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Main extends JFrame {
   private static final int FRAME_WIDTH = 375;
   private static final int FRAME_HEIGHT = 140;
   private static final int LINE_HEIGHT = 18;

   public Main(Boolean visibility) {
      this.setDefaultCloseOperation(3);
      this.setTitle("Group Creator");
      this.setResizable(false);
      JPanel panel = new JPanel();
      JLabel label = new JLabel(
         "<html>Please enter a list of Names separated by commas:<br>(ex: <b>Alice,Bob,Carol,Dave</b>)<html>"
      );
      final JTextField textField = new JTextField();
      textField.setColumns(30);
      JLabel label2 = new JLabel("per group:");
      JLabel minLabel = new JLabel("min: ");
      final JTextField minTextField = new JTextField("1");
      minTextField.setColumns(6);
      JLabel maxLabel = new JLabel("max: ");
      final JTextField maxTextField = new JTextField("3");
      maxTextField.setColumns(6);
      JButton button = new JButton("OK");
      final ResultsPanel results = new ResultsPanel();
      button.addActionListener(
         new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               List<String> lines;
               try {
                  int min = Integer.parseInt(minTextField.getText().trim());
                  int max = Integer.parseInt(maxTextField.getText().trim());
                  lines = GroupMaker.describe(GroupMaker.makeGroups(textField.getText(), min, max, new Random()));
                  Main.writeOutput(lines);
               } catch (NumberFormatException ex) {
                  lines = Collections.singletonList("min and max must be whole numbers.");
               } catch (IllegalArgumentException ex) {
                  lines = Collections.singletonList(ex.getMessage());
               } catch (IOException ex) {
                  lines = Collections.singletonList("Could not write output.txt: " + ex.getMessage());
               }
               results.setLines(lines);
               Main.this.setSize(FRAME_WIDTH, FRAME_HEIGHT + 4 + lines.size() * LINE_HEIGHT);
               Main.this.revalidate();
               Main.this.repaint();
            }
         }
      );
      panel.add(label);
      panel.add(textField);
      panel.add(label2);
      panel.add(minLabel);
      panel.add(minTextField);
      panel.add(maxLabel);
      panel.add(maxTextField);
      panel.add(button);
      panel.add(results);
      this.add(panel);
      this.setSize(FRAME_WIDTH, FRAME_HEIGHT);
      this.setVisible(visibility);
   }

   private static void writeOutput(List<String> lines) throws IOException {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt"), "utf-8"))) {
         writer.write(String.join("\r\n", lines) + "\r\n");
      }
   }

   /** Paints one line per group under the inputs. */
   private static class ResultsPanel extends JPanel {
      private List<String> lines = new ArrayList<>();

      ResultsPanel() {
         this.setPreferredSize(new Dimension(FRAME_WIDTH - 25, 0));
      }

      void setLines(List<String> lines) {
         this.lines = lines;
         this.setPreferredSize(new Dimension(FRAME_WIDTH - 25, lines.size() * LINE_HEIGHT));
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D)g;
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         // shortcut: lines wider than the window are cut off, wrap them if long groups become common.
         for (int i = 0; i < this.lines.size(); i++) {
            g2.drawString(this.lines.get(i), 0, (i + 1) * LINE_HEIGHT - 5);
         }
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
