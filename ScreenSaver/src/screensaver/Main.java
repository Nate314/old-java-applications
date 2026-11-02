package screensaver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A full-screen gallery of screen savers: the original green circles and its two variants, then Starfield, Mystify,
 * Pipes, a bouncing logo and a dot clock. It moves on to the next one every 30 seconds unless one was picked, and
 * closes when the mouse moves.
 */
public class Main extends JFrame implements KeyListener, MouseListener, MouseMotionListener {
   static final double CYCLE_MILLIS = 30_000;
   static final double CAPTION_MILLIS = 5_000;
   static final double FADE_MILLIS = 450;
   /** Mouse moves in the first moments only set where the mouse rests, since showing the window can report one. */
   static final long GRACE_MILLIS = 1_500;
   static final double MIN_SPEED = 0.05;
   /** 250 times faster is the original's fastest: one tile per millisecond. */
   static final double MAX_SPEED = 250;
   /** The moving savers stop speeding up here; past it they are only a blur. */
   static final double MAX_MOTION_SPEED = 8;

   public int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
   public int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
   private final Random random = new Random();
   private final Settings settings = new Settings(Settings.defaultFile());
   private final List<Saver> savers;
   private Saver saver;
   private final Timer timer;
   /** The picture; it is what each frame repaints (in the browser, repainting the frame would not reach it). */
   private final JPanel screen;
   private long lastTick = System.nanoTime();
   private final long startMillis = System.currentTimeMillis();
   private double shownFor;
   private double fadeLeft;
   private double captionLeft = CAPTION_MILLIS;
   /** Where the mouse was first seen after the grace period. */
   private Point firstMouse = null;

   public Main() {
      this.savers = Arrays.asList(new Tiles(Tiles.GREEN_CIRCLES), new Tiles(Tiles.GREEN_SQUARES), new Tiles(Tiles.BRIGHT_CIRCLES),
         new Starfield(), new Mystify(), new Pipes(), new BouncingLogo(), new DotClock(Date::new));
      this.setUndecorated(true);
      this.setSize(this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
      this.setDefaultCloseOperation(3);
      JPanel panel = this.screen = new JPanel() {
         @Override
         protected void paintComponent(Graphics g) {
            Main.this.paintScreen((Graphics2D)g, this.getWidth(), this.getHeight());
         }
      };
      panel.setFocusable(false);
      panel.addMouseListener(this);
      panel.addMouseMotionListener(this);
      this.setContentPane(panel);
      this.addKeyListener(this);
      // The original jar used a custom cursor.png, which did not ship; use it only if one is present.
      if (new File("cursor.png").isFile()) {
         this.setCursor(this.getToolkit().createCustomCursor(new ImageIcon("cursor.png").getImage(), new Point(), "defaultCursor"));
      }
      // Read after the window exists: in the browser, saved files come back when the first window is made.
      this.settings.load();
      for (Saver s : this.savers) {
         if (s instanceof Tiles && this.settings.tileSize > 0) {
            ((Tiles)s).setSize(this.settings.tileSize);
         }
      }
      this.show(Math.max(0, Math.min(this.savers.size() - 1, this.settings.saver)), false);
      this.fadeLeft = 0;
      this.timer = new Timer(16, event -> this.tick());
      this.timer.start();
      this.setVisible(true);
   }

   /** Switches to saver number index, fading in from black, and shows the caption. */
   private void show(int index, boolean save) {
      this.saver = this.savers.get(index);
      this.saver.reset(this.SCREEN_WIDTH, this.SCREEN_HEIGHT, this.random);
      this.settings.saver = index;
      this.shownFor = 0;
      this.fadeLeft = FADE_MILLIS;
      this.captionLeft = CAPTION_MILLIS;
      if (save) {
         this.settings.save();
      }
   }

   private int index() {
      return this.savers.indexOf(this.saver);
   }

   private void tick() {
      long now = System.nanoTime();
      double millis = Math.min(100, (now - this.lastTick) / 1e6);
      this.lastTick = now;
      double speed = this.saver instanceof Tiles ? this.settings.speed : Math.min(MAX_MOTION_SPEED, this.settings.speed);
      this.saver.tick(millis * speed);
      this.shownFor += millis;
      this.fadeLeft = Math.max(0, this.fadeLeft - millis);
      this.captionLeft = Math.max(0, this.captionLeft - millis);
      if (this.settings.cycle && this.shownFor >= CYCLE_MILLIS) {
         this.show((this.index() + 1) % this.savers.size(), true);
      }
      this.screen.repaint();
   }

   private void paintScreen(Graphics2D g, int width, int height) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, width, height);
      this.saver.paint(g);
      if (this.fadeLeft > 0) {
         g.setColor(new Color(0, 0, 0, (int)(255 * this.fadeLeft / FADE_MILLIS)));
         g.fillRect(0, 0, width, height);
      }
      if (this.captionLeft > 0) {
         this.paintCaption(g, width, height);
      }
   }

   /** The caption lines: the saver's name and number, then the keys. */
   String[] caption() {
      String speed = Math.round(this.settings.speed * 10) / 10.0 + "x";
      String cycle = this.settings.cycle ? "C: stop cycling (next in " + (int)Math.ceil((CYCLE_MILLIS - this.shownFor) / 1000) + " s)"
         : "C: cycle every 30 s";
      return new String[] {
         this.saver.name() + "   " + (this.index() + 1) + " of " + this.savers.size(),
         "N / P: next, previous   1-" + this.savers.size() + ": pick   " + cycle,
         "Left / Right: speed " + speed + (this.saver instanceof Tiles ? "   Up / Down: tile size" : "") + "   H: these keys",
         "Esc or move the mouse to close",
      };
   }

   /** Bottom left, fading out over its last second, scaled down until its widest line fits the screen. */
   private void paintCaption(Graphics2D g, int width, int height) {
      String[] lines = this.caption();
      int size = Math.max(10, Math.min(22, height / 28));
      int margin = Math.max(12, width / 60);
      Font font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
      g.setFont(font);
      FontMetrics metrics = g.getFontMetrics();
      int widest = 0;
      for (String line : lines) {
         widest = Math.max(widest, metrics.stringWidth(line));
      }
      while (size > 9 && widest + margin * 3 > width) {
         size--;
         font = new Font(Font.SANS_SERIF, Font.PLAIN, size);
         g.setFont(font);
         metrics = g.getFontMetrics();
         widest = 0;
         for (String line : lines) {
            widest = Math.max(widest, metrics.stringWidth(line));
         }
      }
      double alpha = Math.min(1, this.captionLeft / 1000);
      int lineHeight = metrics.getHeight();
      int boxHeight = lineHeight * lines.length + lineHeight / 2 + size / 2;
      int boxTop = height - margin - boxHeight;
      g.setColor(new Color(0, 0, 0, (int)(170 * alpha)));
      g.fillRoundRect(margin, boxTop, widest + margin * 2, boxHeight, size, size);
      for (int i = 0; i < lines.length; i++) {
         g.setFont(i == 0 ? new Font(Font.SANS_SERIF, Font.BOLD, size) : font);
         g.setColor(i == 0 ? new Color(140, 255, 140, (int)(255 * alpha)) : new Color(220, 220, 220, (int)(230 * alpha)));
         g.drawString(lines[i], margin * 2, boxTop + lineHeight / 2 + metrics.getAscent() + i * lineHeight);
      }
   }

   /** Closes the window as its close button would: exits when run alone, and only closes itself inside NathaNOS. */
   private void kill() {
      this.timer.stop();
      this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
   }

   @Override
   public void keyPressed(KeyEvent e) {
   }

   @Override
   public void keyReleased(KeyEvent e) {
      int keyCode = e.getKeyCode();
      int count = this.savers.size();
      if (keyCode == KeyEvent.VK_ESCAPE) {
         this.kill();
      } else if (keyCode == KeyEvent.VK_N || keyCode == KeyEvent.VK_SPACE) {
         this.show((this.index() + 1) % count, true);
      } else if (keyCode == KeyEvent.VK_P) {
         this.show((this.index() + count - 1) % count, true);
      } else if (keyCode >= KeyEvent.VK_1 && keyCode < KeyEvent.VK_1 + count) {
         // Picking a saver keeps it on screen.
         this.settings.cycle = false;
         this.show(keyCode - KeyEvent.VK_1, true);
      } else if (keyCode == KeyEvent.VK_C) {
         this.settings.cycle = !this.settings.cycle;
         this.shownFor = 0;
         this.captionLeft = CAPTION_MILLIS;
         this.settings.save();
      } else if (keyCode == KeyEvent.VK_H) {
         this.captionLeft = CAPTION_MILLIS;
      } else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
         double factor = keyCode == KeyEvent.VK_RIGHT ? 1.1 : 1 / 1.1;
         this.settings.speed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, this.settings.speed * factor));
         this.captionLeft = CAPTION_MILLIS;
         this.settings.save();
      } else if ((keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) && this.saver instanceof Tiles) {
         double size = ((Tiles)this.saver).size();
         size = keyCode == KeyEvent.VK_UP ? size / 1.1 : Math.max(size + 1.0, size * 1.1);
         for (Saver s : this.savers) {
            if (s instanceof Tiles) {
               ((Tiles)s).setSize(size);
            }
         }
         this.settings.tileSize = ((Tiles)this.saver).size();
         this.settings.save();
      }
   }

   @Override
   public void keyTyped(KeyEvent e) {
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      this.kill();
   }

   @Override
   public void mousePressed(MouseEvent e) {
      this.kill();
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      this.kill();
   }

   @Override
   public void mouseEntered(MouseEvent e) {
   }

   @Override
   public void mouseExited(MouseEvent e) {
      if (!this.inGrace()) {
         this.kill();
      }
   }

   private boolean inGrace() {
      return System.currentTimeMillis() - this.startMillis < GRACE_MILLIS;
   }

   /** Closes once the mouse has really moved after the grace period. */
   private void mouseMovedTo(Point point) {
      if (this.firstMouse == null || this.inGrace()) {
         this.firstMouse = point;
      } else if (this.firstMouse.distance(point) > 5.0) {
         this.kill();
      }
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(Main::new);
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      this.mouseMovedTo(e.getLocationOnScreen());
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      this.mouseMovedTo(e.getLocationOnScreen());
   }
}
