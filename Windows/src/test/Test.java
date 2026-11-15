package test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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

/**
 * Presses the Windows key once, as it always has, and then, instead of quitting, leaves a Windows key on the screen:
 * a small window that stays on top without taking the keyboard, so the key works on whatever window is in front.
 */
public class Test {
   static final String TITLE = "Windows key";
   static final int WIDTH = 116;
   static final int HEIGHT = 138;

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

   /** The key's window, at the lower right of the screen above the taskbar, not yet shown. */
   static JFrame open(WindowsKey.Keys keys) {
      JFrame frame = new JFrame(TITLE);
      WindowsKey key = new WindowsKey(keys);
      JPanel grip = new JPanel(new BorderLayout());
      JLabel name = new JLabel("  " + TITLE);
      JButton close = new JButton("×");
      Color bar = new Color(36, 38, 46);
      grip.setBackground(bar);
      name.setForeground(new Color(170, 176, 190));
      name.setFont(new Font("SansSerif", Font.PLAIN, 11));
      close.setForeground(new Color(236, 239, 244));
      close.setBackground(bar);
      close.setBorderPainted(false);
      close.setFocusPainted(false);
      close.setToolTipText("Close the Windows key");
      close.getAccessibleContext().setAccessibleName("Close the Windows key");
      close.addActionListener(e -> {
         key.setDown(false);
         System.exit(0);
      });
      grip.add(name, BorderLayout.CENTER);
      grip.add(close, BorderLayout.EAST);
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
      frame.add(grip, BorderLayout.NORTH);
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
