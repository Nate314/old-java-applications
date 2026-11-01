package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/** The start menu: the NathaNOS logo, every app in assets/, and Shut Down. Opens above the MENU button. */
public class StartMenu extends JInternalFrame {
   private static final Color BACKGROUND = new Color(26, 35, 54);
   private static final int HEADER = 56;
   private static final int ROW = 34;
   private static final int MAX_COLUMN = 220;
   private static final int FOOTER = ROW + 9;

   public StartMenu(Main screen, Launcher launcher) {
      super("Start Menu", false, false, false);
      this.setVisible(false);
      this.setBorder(null);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);

      int room = screen.getHeight() - screen.launcherWindow.getHeight() - HEADER - FOOTER - 8;
      int rows = Math.max(1, Math.min(launcher.count(), room / ROW));
      int columns = Math.max(1, (launcher.count() + rows - 1) / rows);
      // Columns narrow to fit a small screen; long names are then cut with "...".
      int column = Math.min(MAX_COLUMN, (screen.getWidth() - 8) / columns);
      int width = columns * column + 8;
      int height = HEADER + rows * ROW + FOOTER + 4;
      this.setSize(width, height);
      this.setLocation(0, screen.getHeight() - screen.launcherWindow.getHeight() - height);

      JPanel panel = new JPanel();
      panel.setLayout(null);
      panel.setBackground(BACKGROUND);
      this.add(panel);

      Image logo = new ImageIcon("assets/NathaNOS.png").getImage();
      JComponent header = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            AppIcon.smooth(g);
            g.setColor(new Color(0, 120, 215));
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            AppIcon.paint(g, logo, "NathaNOS", 12, 8, HEADER - 16);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            g.drawString("NathaNOS", HEADER + 4, HEADER / 2 + 2);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString(launcher.count() + " apps", HEADER + 4, HEADER / 2 + 18);
         }
      };
      header.setBounds(0, 0, width, HEADER);
      panel.add(header);

      for (int i = 0; i < launcher.count(); i++) {
         int app = i;
         FlatButton row = new FlatButton(launcher.icon(i), launcher.name(i), launcher.name(i), null, LauncherWindow.HOVER, null, () -> {
            this.hideMenu();
            launcher.launch(app);
         });
         row.setBounds(4 + i / rows * column, HEADER + 4 + i % rows * ROW, column, ROW);
         panel.add(row);
      }

      JComponent divider = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            g.setColor(LauncherWindow.HOVER);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
         }
      };
      divider.setBounds(8, height - FOOTER, width - 16, 1);
      panel.add(divider);
      FlatButton shutDown = new FlatButton(null, null, "Shut Down", null, new Color(160, 40, 50), null, () -> System.exit(0));
      shutDown.setBounds(4, height - ROW - 4, column, ROW);
      panel.add(shutDown);
   }

   public void toggleVisibility() {
      this.setVisible(!this.isVisible());
      if (this.isVisible()) {
         this.moveToFront();
      }
   }

   public void hideMenu() {
      this.setVisible(false);
   }
}
