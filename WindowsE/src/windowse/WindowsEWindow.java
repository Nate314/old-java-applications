package windowse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class WindowsEWindow extends JFrame implements ActionListener {
   private JPanel panel = new JPanel();
   private JTextField countField = new JTextField("10");
   private JButton doItButton = new JButton("DO IT!!!");

   public WindowsEWindow() {
      this.setSize(350, 75);
      this.setTitle("Windows+E");
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      this.doItButton.addActionListener(this);
      this.countField.setColumns(10);
      this.add(this.panel);
      this.panel.add(this.countField);
      this.panel.add(this.doItButton);
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      if (event.getSource() == this.doItButton) {
         new WindowsEPresser(Integer.parseInt(this.countField.getText()));
      }
   }
}
