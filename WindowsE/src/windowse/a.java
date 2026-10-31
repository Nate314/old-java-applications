package windowse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class a extends JFrame implements ActionListener {
   private JPanel a = new JPanel();
   private JTextField b = new JTextField("10");
   private JButton c = new JButton("DO IT!!!");

   public a() {
      this.setSize(350, 75);
      this.setTitle("Windows+E");
      this.setDefaultCloseOperation(3);
      this.setResizable(false);
      this.c.addActionListener(this);
      this.b.setColumns(10);
      this.add(this.a);
      this.a.add(this.b);
      this.a.add(this.c);
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if (var1.getSource() == this.c) {
         new b(Integer.parseInt(this.b.getText()));
      }
   }
}
