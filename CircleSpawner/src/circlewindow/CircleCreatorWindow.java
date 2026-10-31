package circlewindow;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class CircleCreatorWindow extends JFrame implements ActionListener, KeyListener {
   private JPanel panel = new JPanel();
   private JButton createButton = new JButton("Create a circle with that diameter");
   private JTextField diameterField = new JTextField("100");

   public CircleCreatorWindow() {
      this.setTitle("Circle Creator");
      this.setSize(250, 110);
      this.add(this.panel);
      this.setVisible(true);
      this.addKeyListener(this);
      this.setDefaultCloseOperation(3);
      this.createButton.setVisible(true);
      this.createButton.addActionListener(this);
      this.diameterField.setColumns(10);
      this.panel.add(this.diameterField);
      this.panel.add(this.createButton);
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      if (event.getSource() == this.createButton) {
         int diameter = Integer.parseInt(this.diameterField.getText());
         int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
         int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
         double[] sliceWidths = new double[diameter];
         double[] sliceHeights = new double[diameter];
         int radius = diameter / 2;

         for (int i = 0; i < diameter; i++) {
            sliceWidths[i] = (double)(2 * i);
            sliceHeights[i] = 2.0 * Math.sqrt(Math.pow((double)radius, 2.0) - Math.pow((double)i, 2.0));
         }

         for (int sliceIndex = 0; sliceIndex < diameter; sliceIndex++) {
            if (sliceIndex < diameter) {
               new CircleSliceWindow(
                  (int)((double)(screenWidth / 2) - sliceWidths[sliceIndex] / 2.0),
                  (int)((double)(screenHeight / 2) + ((double)(diameter / 2) - sliceHeights[sliceIndex]) / 2.0),
                  sliceWidths[sliceIndex],
                  sliceHeights[sliceIndex],
                  String.valueOf(sliceIndex),
                  sliceIndex % 10 == 0
               );
            }
         }

         System.out.printf("%d, %d", this.getWidth(), this.getHeight());
      }
   }

   @Override
   public final void keyTyped(KeyEvent event) {
   }

   @Override
   public final void keyPressed(KeyEvent event) {
   }

   @Override
   public final void keyReleased(KeyEvent event) {
   }
}
