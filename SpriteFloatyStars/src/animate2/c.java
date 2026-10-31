package animate2;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class c extends JFrame implements ActionListener {
   private JPanel a = new JPanel();
   private String[] b = new String[]{"Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5"};
   private JComboBox c = new JComboBox<>(this.b);
   private JLabel[] d = new JLabel[]{new JLabel("Mode:"), new JLabel("X/Y:"), new JLabel("Number:"), new JLabel("Size:")};
   private JTextField[] e = new JTextField[]{new JTextField("0"), new JTextField("800"), new JTextField("800"), new JTextField("1000"), new JTextField("20")};
   private JButton f = new JButton("OK");

   public c() {
      this.setTitle("Animation Test");
      this.setDefaultCloseOperation(3);
      this.setSize(new Dimension(300, 140));
      this.f.addActionListener(this);
      this.add(this.a);
      this.a.add(this.d[0]);
      this.a.add(this.c);
      this.e[1].setColumns(4);
      this.e[2].setColumns(4);
      this.a.add(this.d[1]);
      this.a.add(this.e[1]);
      this.a.add(this.e[2]);
      this.e[3].setColumns(8);
      this.a.add(this.d[2]);
      this.a.add(this.e[3]);
      this.e[4].setColumns(8);
      this.a.add(this.d[3]);
      this.a.add(this.e[4]);
      this.a.add(this.f);
   }

   @Override
   public final void actionPerformed(ActionEvent var1) {
      if (var1.getSource() == this.f) {
         this.e[0].setText("" + this.c.getSelectedIndex());
         System.out.printf("%d, %d", this.getWidth(), this.getHeight());
         int var6 = Integer.parseInt(this.e[0].getText());
         int var2 = Integer.parseInt(this.e[1].getText());
         int var3 = Integer.parseInt(this.e[2].getText());
         int var4 = Integer.parseInt(this.e[3].getText());
         int var5 = Integer.parseInt(this.e[4].getText());
         new b(var6, new Dimension(var2, var3), var4, var5).setVisible(true);
      }
   }
}
