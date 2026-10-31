package rubikscube;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

public final class Face implements ActionListener {
   private Cube cube = null;
   private String colorName = null;
   private JButton[] buttons = null;
   private int cellWidth = 0;
   private int cellHeight = 0;
   protected Panel panel = new Panel();
   protected String[] stickers = new String[9];
   private int[] stickerX = new int[9];
   private int[] stickerY = new int[9];

   public Face(Cube cube, String colorName, JButton[] buttons, int[] layout, int cellWidth, int cellHeight) {
      this.cube = cube;
      this.colorName = colorName;
      this.buttons = buttons;
      this.cellWidth = cellWidth;
      this.cellHeight = cellHeight;

      for (int index = 0; index < 9; index++) {
         this.stickers[index] = colorName;
      }

      this.panel.setLayout(null);
      this.panel.setBounds(0, 0, cellWidth * 3, cellHeight * 3);

      for (int index = 0; index < 9; index++) {
         this.stickerX[index] = index % 3 * 50 + 2;
         this.stickerY[index] = index / 3 * 50 + 2;
      }

      for (int index = 0; index < 9; index++) {
         this.buttons[index] = new JButton();
         this.buttons[index].addActionListener(this);
         this.buttons[index].setVisible(true);
         int position = layout[index] - 1;
         this.buttons[index].setBounds(this.stickerX[position], this.stickerY[position], 46, 46);
         this.buttons[index].setBackground(this.cube.palette.colorFor(this.stickers[index]));
         this.panel.add(this.buttons[index]);
      }
   }

   public final void refresh() {
      for (int index = 0; index < 9; index++) {
         this.stickerX[index] = index % 3 * this.cellWidth + 2;
         this.stickerY[index] = index / 3 * this.cellHeight + 2;
      }

      for (int index = 0; index < 9; index++) {
         this.buttons[index].setBackground(this.cube.palette.colorFor(this.stickers[index]));
      }
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      if (event.getSource() == this.buttons[0]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 1");
         this.cube.frame.selectedCubie = 1;
      }

      if (event.getSource() == this.buttons[1]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 2");
         this.cube.frame.selectedCubie = 2;
      }

      if (event.getSource() == this.buttons[2]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 3");
         this.cube.frame.selectedCubie = 3;
      }

      if (event.getSource() == this.buttons[3]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 4");
         this.cube.frame.selectedCubie = 4;
      }

      if (event.getSource() == this.buttons[4]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 5");
         this.cube.frame.selectedCubie = 5;
      }

      if (event.getSource() == this.buttons[5]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 6");
         this.cube.frame.selectedCubie = 6;
      }

      if (event.getSource() == this.buttons[6]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 7");
         this.cube.frame.selectedCubie = 7;
      }

      if (event.getSource() == this.buttons[7]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 8");
         this.cube.frame.selectedCubie = 8;
      }

      if (event.getSource() == this.buttons[8]) {
         this.cube.selectedFaceColor = this.colorName;
         System.out.println(this.colorName + " 9");
         this.cube.frame.selectedCubie = 9;
      }
   }
}
