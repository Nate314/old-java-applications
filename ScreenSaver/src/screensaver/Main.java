package screensaver;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class Main extends JFrame implements KeyListener, MouseListener, MouseMotionListener {
   public int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
   public int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
   private int timeToWait = 250;
   private ArrayList<ArrayList<Integer>> positions = new ArrayList<>();
   private ArrayList<Color> colors = new ArrayList<>();
   private int tileSize = this.GCF(this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
   private int numberOfTiles = this.SCREEN_WIDTH / this.tileSize * (this.SCREEN_HEIGHT / this.tileSize);
   private ArrayList<Integer> revealed = new ArrayList<>();
   private boolean adding = true;
   private int mode = 0;

   public Main(Boolean visibility) {
      this.setUndecorated(true);
      this.setVisible(visibility);
      this.setSize(this.SCREEN_WIDTH, this.SCREEN_HEIGHT);
      this.setDefaultCloseOperation(3);
      this.addKeyListener(this);
      this.addMouseListener(this);
      this.setCursor(this.getToolkit().createCustomCursor(new ImageIcon("cursor.png").getImage(), new Point(), "defaultCursor"));
      this.addMouseMotionListener(this);

      for (int i = 0; i < this.SCREEN_WIDTH / this.tileSize; i++) {
         for (int j = 0; j < this.SCREEN_HEIGHT / this.tileSize; j++) {
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(i * this.tileSize);
            temp.add(j * this.tileSize);
            this.positions.add(temp);
            int shade = new Random().nextInt(255);
            this.colors.add(new Color(shade, 255, shade));
         }
      }

      Thread thread = new Thread(new Runnable() {
         @Override
         public void run() {
            Main.this.refreshRegulator();
         }
      });
      thread.start();
   }

   private void refreshRegulator() {
      long currentTime = System.currentTimeMillis();

      while (true) {
         while (System.currentTimeMillis() <= (long)this.timeToWait + currentTime) {
         }

         currentTime = System.currentTimeMillis();
         this.repaint();
      }
   }

   private void kill() {
      System.exit(0);
   }

   public int GCF(int a, int b) {
      return b == 0 ? a : this.GCF(b, a % b);
   }

   public int addNewRandom() {
      int newNumber = new Random().nextInt(this.numberOfTiles);

      for (int i = 0; i < this.revealed.size(); i++) {
         if (this.revealed.get(i) == newNumber) {
            return this.addNewRandom();
         }
      }

      return newNumber;
   }

   private void defaultMode(Graphics g) {
      g.setColor(new Color(0, 0, 0));
      g.fillRect(0, 0, this.getWidth(), this.getHeight());

      for (int i = 0; i < this.revealed.size(); i++) {
         g.setColor(this.colors.get(i));
         g.fillOval(this.positions.get(this.revealed.get(i)).get(0), this.positions.get(this.revealed.get(i)).get(1), this.tileSize, this.tileSize);
      }

      if (this.revealed.size() == this.numberOfTiles) {
         this.adding = false;
      } else if (this.revealed.size() == 0) {
         this.adding = true;
         this.revealed.clear();
      }

      if (this.adding) {
         this.revealed.add(this.addNewRandom());
      } else {
         this.revealed.remove(this.revealed.size() - 1);
      }
   }

   @Override
   public void paint(Graphics g1) {
      Graphics2D g = (Graphics2D)g1;
      if (this.mode == 0) {
         this.defaultMode(g);
      }
   }

   @Override
   public void keyPressed(KeyEvent e) {
   }

   @Override
   public void keyReleased(KeyEvent e) {
      switch (e.getKeyCode()) {
         case 27:
            System.exit(0);
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         default:
            break;
         case 37:
            this.timeToWait = (int)((double)this.timeToWait * 1.1);
            break;
         case 38:
            this.tileSize = (int)((double)this.tileSize / 1.1);
            break;
         case 39:
            this.timeToWait = (int)((double)this.timeToWait / 1.1);
            break;
         case 40:
            this.tileSize = (int)((double)this.tileSize * 1.1);
            break;
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 55:
         case 56:
         case 57:
            this.mode = e.getKeyCode() - 48;
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
      this.kill();
   }

   public static void main(String[] args) {
      new Main(true);
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      this.kill();
   }

   @Override
   public void mouseMoved(MouseEvent e) {
      this.kill();
   }
}
