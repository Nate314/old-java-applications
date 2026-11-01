package screensaver;

import java.awt.Color;
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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main extends JFrame implements KeyListener, MouseListener, MouseMotionListener {
   /** Mode 0: green circles (the original). Mode 1: green squares. Mode 2: circles in random hues. */
   private static final int MODE_COUNT = 3;
   private static final int MIN_TILE_SIZE = 10;
   public int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
   public int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
   private double timeToWait = 250.0;
   private List<Point> positions = new ArrayList<>();
   private List<Color> colors = new ArrayList<>();
   private List<Color> hueColors = new ArrayList<>();
   private double tileSize = this.GCF(this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
   /** Tile indexes in the random order they appear; the first revealedCount are on screen. */
   private List<Integer> order = new ArrayList<>();
   private int revealedCount = 0;
   private boolean adding = true;
   private int mode = 0;
   private Random random = new Random();
   private Timer timer;
   private long startMillis = System.currentTimeMillis();
   /** Where the mouse was first seen; showing the window can report a move without the mouse moving. */
   private Point firstMouse = null;

   public Main() {
      this.setUndecorated(true);
      this.setSize(this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
      this.setDefaultCloseOperation(3);
      this.addKeyListener(this);
      this.addMouseListener(this);
      // The original jar used a custom cursor.png, which did not ship; use it only if one is present.
      if (new File("cursor.png").isFile()) {
         this.setCursor(this.getToolkit().createCustomCursor(new ImageIcon("cursor.png").getImage(), new Point(), "defaultCursor"));
      }
      this.addMouseMotionListener(this);
      this.buildGrid();
      this.timer = new Timer((int)this.timeToWait, event -> {
         this.step();
         this.repaint();
      });
      this.timer.start();
      this.setVisible(true);
   }

   /** Lays out tiles of the current size to cover the screen and starts a new reveal cycle. */
   private void buildGrid() {
      int size = (int)this.tileSize;
      this.positions.clear();
      this.colors.clear();
      this.hueColors.clear();
      this.order.clear();
      for (int x = 0; x < this.SCREEN_WIDTH; x += size) {
         for (int y = 0; y < this.SCREEN_HEIGHT; y += size) {
            this.order.add(this.positions.size());
            this.positions.add(new Point(x, y));
            int shade = this.random.nextInt(255);
            this.colors.add(new Color(shade, 255, shade));
            this.hueColors.add(Color.getHSBColor(this.random.nextFloat(), 0.8F, 1.0F));
         }
      }
      Collections.shuffle(this.order, this.random);
      this.revealedCount = 0;
      this.adding = true;
   }

   private void kill() {
      System.exit(0);
   }

   public int GCF(int a, int b) {
      return b == 0 ? a : this.GCF(b, a % b);
   }

   /** Adds one tile until the grid is full, then removes them in reverse order, then starts over in a new order. */
   private void step() {
      if (this.adding) {
         this.revealedCount++;
         if (this.revealedCount >= this.order.size()) {
            this.adding = false;
         }
      } else {
         this.revealedCount--;
         if (this.revealedCount <= 0) {
            this.adding = true;
            Collections.shuffle(this.order, this.random);
         }
      }
   }

   @Override
   public void paint(Graphics g1) {
      Graphics2D g = (Graphics2D)g1;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(new Color(0, 0, 0));
      g.fillRect(0, 0, this.getWidth(), this.getHeight());
      int size = (int)this.tileSize;

      for (int i = 0; i < this.revealedCount; i++) {
         Point position = this.positions.get(this.order.get(i));
         g.setColor(this.mode == 2 ? this.hueColors.get(i) : this.colors.get(i));
         if (this.mode == 1) {
            g.fillRect(position.x, position.y, size, size);
         } else {
            g.fillOval(position.x, position.y, size, size);
         }
      }
   }

   @Override
   public void keyPressed(KeyEvent e) {
   }

   @Override
   public void keyReleased(KeyEvent e) {
      int keyCode = e.getKeyCode();
      if (keyCode == KeyEvent.VK_ESCAPE) {
         System.exit(0);
      } else if (keyCode == KeyEvent.VK_LEFT) {
         this.timeToWait = Math.min(5000.0, Math.max(this.timeToWait + 1.0, this.timeToWait * 1.1));
         this.timer.setDelay((int)this.timeToWait);
      } else if (keyCode == KeyEvent.VK_RIGHT) {
         this.timeToWait = Math.max(1.0, this.timeToWait / 1.1);
         this.timer.setDelay((int)this.timeToWait);
      } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
         double size = keyCode == KeyEvent.VK_UP ? this.tileSize / 1.1 : Math.max(this.tileSize + 1.0, this.tileSize * 1.1);
         this.tileSize = Math.max(MIN_TILE_SIZE, Math.min(Math.max(this.SCREEN_WIDTH, this.SCREEN_HEIGHT), size));
         this.buildGrid();
      } else if (keyCode >= KeyEvent.VK_0 && keyCode < KeyEvent.VK_0 + MODE_COUNT) {
         this.mode = keyCode - KeyEvent.VK_0;
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
      if (System.currentTimeMillis() - this.startMillis > 1000L) {
         this.kill();
      }
   }

   /** Exits once the mouse has really moved, ignoring the synthetic event when the window first appears under it. */
   private void mouseMovedTo(Point point) {
      if (this.firstMouse == null) {
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
