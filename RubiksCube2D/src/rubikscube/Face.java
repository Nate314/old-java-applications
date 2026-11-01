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
   /** How far a marked sticker shrinks on each side, so the black face shows around it as a ring. */
   private static final int MARK_INSET = 7;
   private int marked = 0;

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

   public final String colorName() {
      return this.colorName;
   }

   /** Paints all nine stickers this face's own color again. */
   final void reset() {
      for (int index = 0; index < 9; index++) {
         this.stickers[index] = this.colorName;
      }
   }

   /** Shrinks square {@code cubie} (1 to 9) inside a black ring to show it is selected; 0 unmarks. */
   final void mark(int cubie) {
      if (cubie == this.marked) {
         return;
      }
      if (this.marked > 0) {
         this.inset(this.buttons[this.marked - 1], -MARK_INSET);
      }
      if (cubie > 0) {
         this.inset(this.buttons[cubie - 1], MARK_INSET);
      }
      this.marked = cubie;
   }

   private void inset(JButton button, int by) {
      button.setBounds(button.getX() + by, button.getY() + by, button.getWidth() - 2 * by, button.getHeight() - 2 * by);
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
            System.out.println(this.colorName + " " + (index + 1));
            this.cube.frame.select(this.colorName, index + 1);
         }
      }
   }
}
