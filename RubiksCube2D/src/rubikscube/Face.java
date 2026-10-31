package rubikscube;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public final class Face implements ActionListener {
   private Cube cube = null;
   private String colorName = null;
   private JButton[] buttons = null;
   protected Panel panel = new Panel();
   protected String[] stickers = new String[9];

   public Face(Cube cube, String colorName, JButton[] buttons, int[] layout, int cellWidth, int cellHeight) {
      this.cube = cube;
      this.colorName = colorName;
      this.buttons = buttons;

      for (int index = 0; index < 9; index++) {
         this.stickers[index] = colorName;
      }

      this.panel.setLayout(null);
      this.panel.setBounds(0, 0, cellWidth * 3, cellHeight * 3);

      for (int index = 0; index < 9; index++) {
         this.buttons[index] = new JButton();
         this.buttons[index].addActionListener(this);
         this.buttons[index].setVisible(true);
         int position = layout[index] - 1;
         this.buttons[index].setBounds(position % 3 * cellWidth + 2, position / 3 * cellHeight + 2, cellWidth - 4, cellHeight - 4);
         this.buttons[index].setBackground(this.cube.palette.colorFor(this.stickers[index]));
         this.panel.add(this.buttons[index]);
      }
   }

   public final void refresh() {
      for (int index = 0; index < 9; index++) {
         this.buttons[index].setBackground(this.cube.palette.colorFor(this.stickers[index]));
      }
   }

   /** True when all nine stickers are the same color. */
   public final boolean isSolid() {
      for (String sticker : this.stickers) {
         if (!sticker.equals(this.stickers[0])) {
            return false;
         }
      }
      return true;
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      for (int index = 0; index < 9; index++) {
         if (event.getSource() == this.buttons[index]) {
            this.cube.selectedFaceColor = this.colorName;
            System.out.println(this.colorName + " " + (index + 1));
            this.cube.frame.selectedCubie = index + 1;
         }
      }
   }
}
