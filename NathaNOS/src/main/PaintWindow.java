package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;

/** Opens Paint in an internal window on the desktop (like the old Clock), with its taskbar button. */
final class PaintWindow {
   private PaintWindow() {
   }

   /** A Paint window on the picture, or on a new blank one when picture is null. */
   static void open(Main screen, TaskList tasks, Picture picture, Image icon) {
      Picture shown = picture != null ? picture : new Picture(screen.pictures.nextName());
      GenericAppFrame[] frame = new GenericAppFrame[1];
      PaintPanel[] made = new PaintPanel[1];
      PaintPanel panel = new PaintPanel(screen, shown, () -> {
         if (frame[0] != null) {
            frame[0].setTitle(made[0].title());
         }
      });
      made[0] = panel;
      frame[0] = new GenericAppFrame(panel.title(), panel, false, icon, screen);
      Dimension size = panel.getPreferredSize();
      // The frame's border and title bar take some room: measure them once laid out, and grow the frame by that much.
      frame[0].setSize(size.width, size.height);
      screen.desktopPane.add(frame[0]);
      frame[0].validate();
      Dimension inner = frame[0].getContentPane().getSize();
      boolean measured = inner.width > 0 && inner.height > 0;
      int width = measured ? 2 * size.width - inner.width : size.width + 12;
      int height = measured ? 2 * size.height - inner.height : size.height + 36;
      frame[0].setSize(width, height);
      int room = screen.getHeight() - screen.launcherWindow.getHeight();
      // Each further Paint window opens a little lower and to the right, so it does not hide the one before.
      int offset = 24 * (tasks.countNamed(Launcher.PAINT) % 6);
      frame[0].setPosition(Math.max(0, (screen.getWidth() - width) / 2 + offset), Math.max(0, (room - height) / 2 + offset));
      tasks.add(frame[0], Launcher.PAINT, icon);
      frame[0].moveToFront();
      try {
         frame[0].setSelected(true);
      } catch (PropertyVetoException ignored) {
         // It still opens, just not selected.
      }
      panel.requestFocus();
   }

   /** Paint's icon: an artist's palette with blobs of paint and a brush. */
   static Image icon() {
      BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      AppIcon.smooth(g);
      g.setColor(new Color(214, 170, 120));
      g.fillOval(4, 10, 54, 44);
      g.setColor(new Color(244, 247, 251));
      g.fillOval(34, 34, 12, 10); // the thumb hole
      Color[] blobs = {Picture.PALETTE[3], Picture.PALETTE[5], Picture.PALETTE[6], Picture.PALETTE[7], Picture.PALETTE[15]};
      int[][] at = {{12, 20}, {24, 14}, {38, 16}, {12, 34}, {20, 44}};
      for (int i = 0; i < blobs.length; i++) {
         g.setColor(blobs[i]);
         g.fillOval(at[i][0], at[i][1], 11, 10);
      }
      g.setColor(new Color(90, 60, 40));
      g.setStroke(new java.awt.BasicStroke(5f));
      g.drawLine(60, 4, 42, 28);
      g.setColor(Picture.PALETTE[14]);
      g.fillOval(37, 25, 9, 9);
      g.dispose();
      return image;
   }
}
