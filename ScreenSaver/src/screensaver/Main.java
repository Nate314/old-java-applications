package screensaver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
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
 * Pipes, a bouncing logo, a dot clock, an aquarium of fish that stay yours between runs and a brick maze walked in first person. It moves on to the next one
 * every 30 seconds unless one was picked, and closes when the mouse moves. M plays music made by the saver on screen.
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
   /** Calm (the system asks for less motion): the moving savers go no faster than this... */
   static final double CALM_MOTION_SPEED = 0.35;
   /** ...and the tiles no faster than this, so a fast setting never makes the grid flicker. */
   static final double CALM_TILE_SPEED = 4;

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
   /** All the savers at once (G), or null. */
   private Gallery gallery;
   /** A saver picked from the gallery grows from its little screen to the whole one. */
   private Rectangle zoomFrom;
   private double zoomLeft;
   /** When a saver was last picked from the gallery: the mouse that picked it needs a moment to come to rest. */
   private long pickedMillis;
   /** Play (T): the mouse (or a finger) plays with the saver instead of closing it; Esc still closes. */
   private boolean play;
   /** Music (M): off at every start and never saved, so the site's idle screen saver stays silent. */
   private boolean music;

   public Main() {
      this.savers = Arrays.asList(new Tiles(Tiles.GREEN_CIRCLES), new Tiles(Tiles.GREEN_SQUARES), new Tiles(Tiles.BRIGHT_CIRCLES),
         new Starfield(), new Mystify(), new Pipes(), new BouncingLogo(), new DotClock(Date::new),
         new Aquarium(this.settings.tank, Date::new, this.settings::save), new Maze());
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
      if (this.saver != null) {
         this.saver.touch(null, false);
      }
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
      // The gallery's ten savers at once would be a din: music only plays for the one on screen.
      Music.live(this.music && this.gallery == null);
      Music.tick(millis);
      if (this.gallery != null) {
         for (Saver s : this.savers) {
            s.tick(millis * this.speedFor(s));
         }
         this.screen.repaint();
         return;
      }
      this.zoomLeft = Math.max(0, this.zoomLeft - millis);
      this.saver.tick(millis * this.speedFor(this.saver));
      this.shownFor += millis;
      this.fadeLeft = Math.max(0, this.fadeLeft - millis);
      this.captionLeft = Math.max(0, this.captionLeft - millis);
      if (this.settings.cycle && this.shownFor >= CYCLE_MILLIS) {
         this.show((this.index() + 1) % this.savers.size(), true);
      }
      this.screen.repaint();
   }

   private double speedFor(Saver s) {
      return speedFor(s, this.settings.speed, calm());
   }

   /** The speed saver s runs at when speed is set; calm slows the moving savers right down and keeps tiles from flickering. */
   static double speedFor(Saver s, double speed, boolean calm) {
      if (s instanceof Tiles) {
         return calm ? Math.min(CALM_TILE_SPEED, speed) : speed;
      }
      return Math.min(calm ? CALM_MOTION_SPEED : MAX_MOTION_SPEED, speed);
   }

   /** The visitor's system asks for less motion (in the browser; the desktop JDK answers null, so never there). */
   static boolean calm() {
      return Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion"));
   }

   /** G: every saver at once. The ones not on screen start over at full size, to be painted small. */
   private void openGallery() {
      this.saver.touch(null, false);
      for (Saver s : this.savers) {
         if (s != this.saver) {
            s.reset(this.SCREEN_WIDTH, this.SCREEN_HEIGHT, this.random);
         }
      }
      this.gallery = new Gallery(this.savers, this.index());
      this.captionLeft = 0;
      this.fadeLeft = 0;
   }

   /** Leaves the gallery on saver index, growing it from its cell; picking one (stay) stops the cycling, as 1-9 do. */
   private void pick(int index, boolean stay) {
      this.zoomFrom = this.gallery.cell(index);
      // Calm: the picked saver is simply there, without growing out of its cell.
      this.zoomLeft = this.zoomFrom == null || calm() ? 0 : Gallery.ZOOM_MILLIS;
      this.gallery = null;
      this.saver = this.savers.get(index);
      this.settings.saver = index;
      if (stay) {
         this.settings.cycle = false;
      }
      this.shownFor = 0;
      this.captionLeft = CAPTION_MILLIS;
      this.pickedMillis = System.currentTimeMillis();
      this.firstMouse = null;
      this.settings.save();
   }

   private boolean justPicked() {
      return System.currentTimeMillis() - this.pickedMillis < GRACE_MILLIS;
   }

   private void paintScreen(Graphics2D g, int width, int height) {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, width, height);
      if (this.gallery != null) {
         this.gallery.paint(g, width, height, this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
         return;
      }
      if (this.zoomLeft > 0) {
         double t = 1 - this.zoomLeft / Gallery.ZOOM_MILLIS;
         t = t * t * (3 - 2 * t);
         Rectangle from = this.zoomFrom;
         Graphics2D z = (Graphics2D)g.create();
         z.translate(from.x * (1 - t), from.y * (1 - t));
         double w = from.width + (width - from.width) * t;
         double h = from.height + (height - from.height) * t;
         z.scale(w / this.SCREEN_WIDTH, h / this.SCREEN_HEIGHT);
         this.saver.paint(z);
         z.dispose();
      } else {
         this.saver.paint(g);
      }
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
      String speed = speedText(this.settings.speed, this.speedFor(this.saver), calm());
      String cycle = this.settings.cycle ? "C: stop cycling (next in " + (int)Math.ceil((CYCLE_MILLIS - this.shownFor) / 1000) + " s)"
         : "C: cycle every 30 s";
      String head = this.saver.name() + "   " + (this.index() + 1) + " of " + this.savers.size();
      String keys = "N / P: next, previous   1-9, 0: pick   G: see all   " + cycle;
      if (this.saver instanceof Aquarium) {
         return new String[] {head, ((Aquarium)this.saver).status(), keys,
            "Left / Right: speed " + speed + "   " + this.musicKey() + "   H: names and these keys", this.closeLine()};
      }
      return new String[] {
         head,
         keys,
         "Left / Right: speed " + speed + (this.saver instanceof Tiles ? "   Up / Down: tile size" : "") + "   " + this.musicKey() + "   H: these keys",
         this.closeLine(),
      };
   }

   /** The speed set, and when calm holds this saver slower, the speed it really runs at. */
   static String speedText(double set, double runs, boolean calm) {
      String text = Math.round(set * 10) / 10.0 + "x";
      return calm && runs < set ? text + " (calm, for less motion: " + Math.round(runs * 100) / 100.0 + "x)" : text;
   }

   private String musicKey() {
      return this.music ? "M: stop the music" : "M: music";
   }

   /** The last caption line: how to close, or in play what the mouse does to this saver. */
   private String closeLine() {
      if (!this.play) {
         return "Esc or move the mouse to close   T: play with it";
      }
      return "Playing: " + playHint(this.saver) + "   T: hands off   Esc: close";
   }

   /** What the mouse does to each saver in play (T). */
   static String playHint(Saver s) {
      if (s instanceof Tiles) {
         return "the mouse pushes the " + (s.name().endsWith("squares") ? "squares" : "circles") + " aside, harder when held";
      } else if (s instanceof Starfield) {
         return "point to steer, hold for warp speed";
      } else if (s instanceof Mystify) {
         return "hold to pull the shapes in";
      } else if (s instanceof Pipes) {
         return "press to start a pipe, hold and it follows";
      } else if (s instanceof BouncingLogo) {
         return "grab the logo and throw it at a corner";
      } else if (s instanceof DotClock) {
         return "the mouse scatters the dots";
      } else if (s instanceof Aquarium) {
         return "click to drop flakes there";
      }
      return "hold to hurry the walker";
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
      Music.live(false);
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
      } else if (this.gallery != null) {
         this.galleryKey(keyCode);
      } else if (keyCode == KeyEvent.VK_G) {
         this.openGallery();
      } else if (keyCode == KeyEvent.VK_N || keyCode == KeyEvent.VK_SPACE) {
         this.show((this.index() + 1) % count, true);
      } else if (keyCode == KeyEvent.VK_P) {
         this.show((this.index() + count - 1) % count, true);
      } else if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
         // Picking a saver keeps it on screen; 0 is the tenth.
         this.settings.cycle = false;
         this.show(keyCode == KeyEvent.VK_0 ? 9 : keyCode - KeyEvent.VK_1, true);
      } else if (keyCode == KeyEvent.VK_T) {
         this.play = !this.play;
         this.saver.touch(null, false);
         this.captionLeft = CAPTION_MILLIS;
         // Coming out of play, the hand still on the mouse gets a moment before a move closes the saver.
         this.pickedMillis = System.currentTimeMillis();
         this.firstMouse = null;
      } else if (keyCode == KeyEvent.VK_M) {
         this.music = !this.music;
         this.captionLeft = CAPTION_MILLIS;
      } else if (keyCode == KeyEvent.VK_C) {
         this.settings.cycle = !this.settings.cycle;
         this.shownFor = 0;
         this.captionLeft = CAPTION_MILLIS;
         this.settings.save();
      } else if (keyCode == KeyEvent.VK_H) {
         this.captionLeft = CAPTION_MILLIS;
         if (this.saver instanceof Aquarium) {
            ((Aquarium)this.saver).showNames();
         }
      } else if (keyCode == KeyEvent.VK_F && this.saver instanceof Aquarium) {
         ((Aquarium)this.saver).feed();
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

   private void galleryKey(int keyCode) {
      Gallery g = this.gallery;
      if (keyCode == KeyEvent.VK_G) {
         this.pick(this.index(), false);
      } else if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_SPACE) {
         this.pick(g.selected, true);
      } else if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
         this.pick(keyCode == KeyEvent.VK_0 ? 9 : keyCode - KeyEvent.VK_1, true);
      } else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_P) {
         g.move(-1, 0);
      } else if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_N) {
         g.move(1, 0);
      } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
         g.move(0, keyCode == KeyEvent.VK_UP ? -1 : 1);
      }
   }

   @Override
   public void keyTyped(KeyEvent e) {
   }

   /** Any click closes the saver, except in the gallery (where letting go picks) and just after a pick. */
   private void clicked(MouseEvent e, boolean release) {
      if (this.playing()) {
         this.saver.touch(e.getPoint(), !release);
      } else if (this.gallery != null) {
         int index = release ? this.gallery.at(e.getX(), e.getY()) : -1;
         if (index >= 0) {
            this.pick(index, true);
         }
      } else if (!this.justPicked()) {
         this.kill();
      }
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      if (!this.playing()) {
         this.clicked(e, false);
      }
   }

   /** Play is on and a saver (not the gallery) is showing. */
   private boolean playing() {
      return this.play && this.gallery == null;
   }

   @Override
   public void mousePressed(MouseEvent e) {
      this.clicked(e, false);
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      this.clicked(e, true);
   }

   @Override
   public void mouseEntered(MouseEvent e) {
   }

   @Override
   public void mouseExited(MouseEvent e) {
      if (this.playing()) {
         this.saver.touch(null, false);
      } else if (!this.inGrace() && this.gallery == null) {
         this.kill();
      }
   }

   private boolean inGrace() {
      return System.currentTimeMillis() - this.startMillis < GRACE_MILLIS || this.justPicked();
   }

   /** Closes once the mouse has really moved after the grace period. */
   private void mouseMovedTo(MouseEvent e, boolean down) {
      Point point = e.getLocationOnScreen();
      if (this.playing()) {
         this.saver.touch(e.getPoint(), down);
      } else if (this.gallery != null) {
         int index = this.gallery.at(e.getX(), e.getY());
         if (index >= 0) {
            this.gallery.selected = index;
         }
      } else if (this.firstMouse == null || this.inGrace()) {
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
      this.mouseMovedTo(e, true);
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      this.mouseMovedTo(e, false);
   }
}
