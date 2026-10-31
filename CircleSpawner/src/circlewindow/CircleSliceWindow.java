package circlewindow;

import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;

public final class CircleSliceWindow extends JFrame {
   public CircleSliceWindow(int x, int y, double sliceWidth, double sliceHeight, String title, boolean onTop) {
      Toolkit.getDefaultToolkit().getScreenSize();
      JPanel panel = new JPanel();
      this.setTitle(title);
      this.setSize((int)sliceWidth, (int)sliceHeight);
      this.add(panel);
      this.setUndecorated(true);
      this.setVisible(true);
      this.setLocation(x, y);
      this.setAlwaysOnTop(onTop);
      panel.setBackground(new Color(0, 0, 255));
   }
}
