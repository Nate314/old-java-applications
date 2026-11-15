package test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.function.Supplier;

/**
 * Presses the Windows key once, as it always has, and then, instead of quitting, leaves a Windows key on the screen:
 * a small window that stays on top without taking the keyboard, so the key works on whatever window is in front.
 */
public class Test {
   static final String TITLE = "Windows key";
   static final int WIDTH = 116;
   static final int HEIGHT = 138;
   private static final Color BAR = new Color(36, 38, 46);
   static final String SHOW_SHORTCUTS = "Show shortcuts";
   static final String HIDE_SHORTCUTS = "Hide shortcuts";
   static final String KEY_SIZE = "Key size: ";

   public static void main(String[] args) throws Exception {
      Robot robot = new Robot();
      robot.keyPress(KeyEvent.VK_WINDOWS);
      robot.keyRelease(KeyEvent.VK_WINDOWS);
      Thread.sleep(100L);
      SwingUtilities.invokeLater(() -> open(new WindowsKey.Keys() {
         @Override
         public void press(int keyCode) {
            robot.keyPress(keyCode);
         }

         @Override
         public void release(int keyCode) {
            robot.keyRelease(keyCode);
         }
      }, Placement.FILE).setVisible(true));
   }

   /**
    * A button in the strip, drawn by hand so it is as narrow as its sign on the desktop too (a JButton's margins
    * would push the name out of the strip).
    */
   private static JButton stripButton(Supplier<String> sign) {
      JButton button = new JButton() {
         @Override
         protected void paintComponent(Graphics g) {
            g.setColor(BAR);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.setColor(new Color(236, 239, 244));
            // The sign grows with the strip, which grows with the key's size.
            g.setFont(new Font("SansSerif", Font.PLAIN, Math.round(13f * this.getHeight() / 24)));
            String s = sign.get();
            FontMetrics m = g.getFontMetrics();
            g.drawString(s, (this.getWidth() - m.stringWidth(s)) / 2, (this.getHeight() + m.getAscent()) / 2 - 2);
            if (this.isFocusOwner()) {
               g.drawRect(2, 3, this.getWidth() - 5, this.getHeight() - 7);
            }
         }
      };
      button.setPreferredSize(new Dimension(22, 24));
      button.setContentAreaFilled(false);
      button.setBorderPainted(false);
      button.setFocusPainted(false);
      return button;
   }

   /**
    * The key's window, not yet shown: where the user left it, at the size they picked and with the shelf as it was
    * (from that file, which every move, resize and shelf change rewrites), or else small at the lower right of the
    * screen above the taskbar.
    */
   static JFrame open(WindowsKey.Keys keys, File file) {
      JFrame frame = new JFrame(TITLE);
      // After the first component: in the browser, that is what puts the saved files in place.
      Placement place = Placement.load(file);
      WindowsKey key = new WindowsKey(keys);
      JPanel grip = new JPanel(new BorderLayout());
      JLabel name = new JLabel(" Windows");
      JButton close = stripButton(() -> "×");
      grip.setBackground(BAR);
      name.setForeground(new Color(170, 176, 190));
      name.setFont(new Font("SansSerif", Font.PLAIN, 11));
      close.setToolTipText("Close the Windows key");
      close.getAccessibleContext().setAccessibleName("Close the Windows key");
      close.addActionListener(e -> {
         key.setDown(false);
         System.exit(0);
      });
      ShortcutShelf shelf = new ShortcutShelf(keys);
      shelf.setVisible(place.shelf);
      JButton more = stripButton(() -> shelf.isVisible() ? "▾" : "▴");
      more.setToolTipText("Shortcuts");
      more.getAccessibleContext().setAccessibleName(place.shelf ? HIDE_SHORTCUTS : SHOW_SHORTCUTS);
      // + makes the key bigger; at the largest, - takes it back to the smallest.
      JButton size = stripButton(() -> place.size + 1 < Placement.SCALES.length ? "+" : "−");
      Runnable nameSize = () -> {
         String now = Placement.SIZE_NAMES[place.size];
         String next = Placement.SIZE_NAMES[(place.size + 1) % Placement.SCALES.length];
         size.setToolTipText("Key size: " + now + ". Click for " + next);
         size.getAccessibleContext().setAccessibleName(KEY_SIZE + now + ", click for " + next);
      };
      nameSize.run();
      JButton[] signs = {size, more, close};
      // Sizes everything for the scale picked and puts the window back on the screen, its bottom right corner kept.
      Runnable fit = () -> {
         double s = place.scale();
         int w = (int) Math.round(WIDTH * s);
         int h = (int) Math.round((shelf.isVisible() ? HEIGHT + ShortcutShelf.HEIGHT : HEIGHT) * s);
         grip.setPreferredSize(new Dimension(w, (int) Math.round(24 * s)));
         for (JButton b : signs) {
            b.setPreferredSize(new Dimension((int) Math.round(22 * s), (int) Math.round(24 * s)));
            // Their signs change with the shelf and the size, and the shim repaints a button only when asked.
            b.repaint();
         }
         name.setFont(new Font("SansSerif", Font.PLAIN, (int) Math.round(11 * s)));
         shelf.setPreferredSize(new Dimension(w, (int) Math.round(ShortcutShelf.HEIGHT * s)));
         Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
         int x = Math.max(0, Math.min(frame.getX() + frame.getWidth() - w, screen.width - w));
         int y = Math.max(0, Math.min(frame.getY() + frame.getHeight(), screen.height) - h);
         frame.setBounds(x, y, w, h);
         frame.validate();
         frame.repaint();
      };
      Runnable keep = () -> {
         place.x = frame.getX();
         place.bottom = frame.getY() + frame.getHeight();
         place.shelf = shelf.isVisible();
         place.save(file);
      };
      more.addActionListener(e -> {
         boolean show = !shelf.isVisible();
         shelf.setVisible(show);
         more.getAccessibleContext().setAccessibleName(show ? HIDE_SHORTCUTS : SHOW_SHORTCUTS);
         // The shelf opens upward, so the key stays where it was, above the taskbar.
         fit.run();
         keep.run();
      });
      size.addActionListener(e -> {
         place.size = (place.size + 1) % Placement.SCALES.length;
         nameSize.run();
         fit.run();
         keep.run();
      });
      JPanel buttons = new JPanel(new BorderLayout());
      buttons.setBackground(BAR);
      buttons.add(size, BorderLayout.WEST);
      buttons.add(more, BorderLayout.CENTER);
      buttons.add(close, BorderLayout.EAST);
      grip.add(name, BorderLayout.CENTER);
      grip.add(buttons, BorderLayout.EAST);
      JPanel top = new JPanel(new BorderLayout());
      top.add(grip, BorderLayout.NORTH);
      top.add(shelf, BorderLayout.CENTER);
      // No title bar, so the strip above the key moves the window.
      MouseAdapter drag = new MouseAdapter() {
         private int x;
         private int y;

         @Override
         public void mousePressed(MouseEvent e) {
            this.x = e.getX();
            this.y = e.getY();
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            frame.setLocation(frame.getX() + e.getX() - this.x, frame.getY() + e.getY() - this.y);
         }

         @Override
         public void mouseReleased(MouseEvent e) {
            // Dropped partly off the screen, it comes back whole; then it is remembered there.
            fit.run();
            keep.run();
         }
      };
      grip.addMouseListener(drag);
      grip.addMouseMotionListener(drag);
      name.addMouseListener(drag);
      name.addMouseMotionListener(drag);
      frame.setUndecorated(true);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(top, BorderLayout.NORTH);
      frame.add(key, BorderLayout.CENTER);
      frame.setAlwaysOnTop(true);
      // An on-screen key must not take the keyboard from the window it presses keys on.
      frame.setFocusableWindowState(false);
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      // shortcut: 64 px is a guess at the taskbar's height; GraphicsEnvironment's usable bounds would be exact on the desktop.
      // An empty window at the bottom right corner it will have; fit grows it from there, onto the screen.
      frame.setBounds(place.x >= 0 ? place.x + (int) Math.round(WIDTH * place.scale()) : screen.width - 24,
         place.bottom >= 0 ? place.bottom : screen.height - 64, 0, 0);
      fit.run();
      return frame;
   }
}
