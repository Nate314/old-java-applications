package animate2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class SettingsFrame extends JFrame implements ActionListener {
   private JPanel panel = new JPanel();
   private String[] modeNames = new String[]{"Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5"};
   private JComboBox<String> modeComboBox = new JComboBox<>(this.modeNames);
   private JLabel[] labels = new JLabel[]{new JLabel("Mode:"), new JLabel("X/Y:"), new JLabel("Number:"), new JLabel("Size:")};
   private JTextField[] textFields = new JTextField[]{new JTextField("0"), new JTextField("800"), new JTextField("800"), new JTextField("1000"), new JTextField("20")};
   private JButton okButton = new JButton("OK");
   private JButton catchButton = new JButton("Play Star Catcher");

   public SettingsFrame() {
      this.setTitle("Animation Test");
      this.setDefaultCloseOperation(3);
      this.setSize(new Dimension(300, 180));
      this.okButton.addActionListener(this);
      this.catchButton.setFont(this.catchButton.getFont().deriveFont(Font.BOLD, 15f));
      this.catchButton.addActionListener(event -> {
         new CatchFrame(this).setVisible(true);
         this.setVisible(false);
      });
      this.add(this.catchButton, BorderLayout.NORTH);
      this.add(this.panel, BorderLayout.CENTER);
      this.panel.add(this.labels[0]);
      this.panel.add(this.modeComboBox);
      this.textFields[1].setColumns(4);
      this.textFields[2].setColumns(4);
      this.panel.add(this.labels[1]);
      this.panel.add(this.textFields[1]);
      this.panel.add(this.textFields[2]);
      this.textFields[3].setColumns(8);
      this.panel.add(this.labels[2]);
      this.panel.add(this.textFields[3]);
      this.textFields[4].setColumns(8);
      this.panel.add(this.labels[3]);
      this.panel.add(this.textFields[4]);
      this.panel.add(this.okButton);
   }

   @Override
   public final void actionPerformed(ActionEvent e) {
      if (e.getSource() == this.okButton) {
         this.textFields[0].setText("" + this.modeComboBox.getSelectedIndex());
         try {
            int mode = Integer.parseInt(this.textFields[0].getText());
            int width = Integer.parseInt(this.textFields[1].getText().trim());
            int height = Integer.parseInt(this.textFields[2].getText().trim());
            int starCount = Integer.parseInt(this.textFields[3].getText().trim());
            int starSize = Integer.parseInt(this.textFields[4].getText().trim());
            if (width <= 0 || height <= 0 || starCount < 0 || starSize <= 0) {
               throw new NumberFormatException();
            }
            this.setTitle("Animation Test");
            new AnimationFrame(mode, new Dimension(width, height), starCount, starSize).setVisible(true);
         } catch (NumberFormatException exception) {
            this.setTitle("Use positive whole numbers");
         }
      }
   }
}
