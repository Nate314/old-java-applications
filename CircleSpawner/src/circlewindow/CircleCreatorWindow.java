package circlewindow;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class CircleCreatorWindow extends JFrame implements ActionListener {
   private JPanel panel = new JPanel();
   private JButton createButton = new JButton("Create a circle with that diameter");
   private JTextField diameterField = new JTextField("100");
   private List<CircleSliceWindow> slices = new ArrayList<>();

   public CircleCreatorWindow() {
      this.setTitle("Circle Creator");
      this.setSize(250, 110);
      this.add(this.panel);
      this.setDefaultCloseOperation(3);
      this.createButton.addActionListener(this);
      this.diameterField.setColumns(10);
      this.panel.add(this.diameterField);
      this.panel.add(this.createButton);
      this.setVisible(true);
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      if (event.getSource() == this.createButton) {
         int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
         int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
         int maxDiameter = Math.min(screenWidth, screenHeight);
         int diameter;
         try {
            diameter = Integer.parseInt(this.diameterField.getText().trim());
         } catch (NumberFormatException e) {
            diameter = -1;
         }
         if (diameter < 2 || diameter > maxDiameter) {
            this.setTitle("Use 2 to " + maxDiameter);
            return;
         }
         this.setTitle("Circle Creator");

         // Replace the previous circle instead of stacking a new one on top.
         for (CircleSliceWindow slice : this.slices) {
            slice.dispose();
         }
         this.slices.clear();

         double radius = diameter / 2.0;
         int centerX = screenWidth / 2;
         int centerY = screenHeight / 2;

         // Slice i is a rectangle whose corners sit on the circle: half-width i, half-height sqrt(r^2 - i^2).
         for (int sliceIndex = 1; sliceIndex <= (int)radius; sliceIndex++) {
            double sliceWidth = 2.0 * sliceIndex;
            double sliceHeight = 2.0 * Math.sqrt(radius * radius - (double)sliceIndex * sliceIndex);
            if (sliceHeight < 1.0) {
               continue;
            }
            this.slices.add(new CircleSliceWindow(
               (int)(centerX - sliceWidth / 2.0),
               (int)(centerY - sliceHeight / 2.0),
               sliceWidth,
               sliceHeight,
               String.valueOf(sliceIndex),
               sliceIndex % 10 == 0
            ));
         }
      }
   }
}
