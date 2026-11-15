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
      }).setVisible(true));
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
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
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

   /** The key's window, at the lower right of the screen above the taskbar, not yet shown. */
   static JFrame open(WindowsKey.Keys keys) {
      JFrame frame = new JFrame(TITLE);
      WindowsKey key = new WindowsKey(keys);
      JPanel grip = new JPanel(new BorderLayout());
      JLabel name = new JLabel("  " + TITLE);
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
      shelf.setVisible(false);
      JButton more = stripButton(() -> shelf.isVisible() ? "▾" : "▴");
      more.setToolTipText("Shortcuts");
      more.getAccessibleContext().setAccessibleName(SHOW_SHORTCUTS);
      more.addActionListener(e -> {
         boolean show = !shelf.isVisible();
         shelf.setVisible(show);
         more.getAccessibleContext().setAccessibleName(show ? "Hide shortcuts" : SHOW_SHORTCUTS);
         // The shelf opens upward, so the key stays where it was, above the taskbar.
         int h = show ? HEIGHT + ShortcutShelf.HEIGHT : HEIGHT;
         frame.setBounds(frame.getX(), Math.max(0, frame.getY() + frame.getHeight() - h), WIDTH, h);
         frame.validate();
         frame.repaint();
         more.repaint();
      });
      JPanel buttons = new JPanel(new BorderLayout());
      buttons.setBackground(BAR);
      buttons.add(more, BorderLayout.WEST);
      buttons.add(close, BorderLayout.EAST);
      grip.add(name, BorderLayout.CENTER);
      grip.add(buttons, BorderLayout.EAST);
      JPanel top = new JPanel(new BorderLayout());
      top.add(grip, BorderLayout.NORTH);
      top.add(shelf, BorderLayout.CENTER);
      grip.setPreferredSize(new Dimension(WIDTH, 24));
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
      };
      grip.addMouseListener(drag);
      grip.addMouseMotionListener(drag);
      name.addMouseListener(drag);
      name.addMouseMotionListener(drag);
      frame.setUndecorated(true);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.add(top, BorderLayout.NORTH);
      frame.add(key, BorderLayout.CENTER);
      frame.setSize(WIDTH, HEIGHT);
      frame.setAlwaysOnTop(true);
      // An on-screen key must not take the keyboard from the window it presses keys on.
      frame.setFocusableWindowState(false);
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      // shortcut: 64 px is a guess at the taskbar's height; GraphicsEnvironment's usable bounds would be exact on the desktop.
      frame.setLocation(screen.width - WIDTH - 24, screen.height - HEIGHT - 64);
      return frame;
   }
}
