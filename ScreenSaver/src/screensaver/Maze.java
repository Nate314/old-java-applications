package screensaver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Walking a brick maze in first person, keeping a hand on the left wall, toward a glowing orb at the far corner.
 * When the walker reaches it the maze is shown from above with the way it went, then a new maze is built.
 */
class Maze implements Saver {
   /** Rooms across and down; the block grid is twice that plus one, walls and rooms each a block. */
   static final int ROOMS = 9;
   static final int SIZE = ROOMS * 2 + 1;
   static final double MOVE_MILLIS = 380;
   static final double TURN_MILLIS = 330;
   static final double MAP_MILLIS = 4200;
   private static final int SHADES = 32;
   private static final int ROWS = 4;
   private static final int[] DX = {1, 0, -1, 0};
   private static final int[] DY = {0, 1, 0, -1};

   private static final Color[][][] BRICK = new Color[4][2][SHADES];
   private static final Color[][] MORTAR = new Color[2][SHADES];
   static {
      int[][] bricks = {{168, 70, 48}, {150, 60, 42}, {182, 86, 58}, {140, 66, 52}};
      for (int s = 0; s < SHADES; s++) {
         double f = s / (SHADES - 1.0);
         for (int side = 0; side < 2; side++) {
            double light = f * (side == 0 ? 1.0 : 0.72);
            for (int v = 0; v < bricks.length; v++) {
               BRICK[v][side][s] = new Color((int)(bricks[v][0] * light), (int)(bricks[v][1] * light), (int)(bricks[v][2] * light));
            }
            MORTAR[side][s] = new Color((int)(96 * light), (int)(88 * light), (int)(80 * light));
         }
      }
   }

   /** [y][x], true where one can walk. */
   boolean[][] open = new boolean[SIZE][SIZE];
   private boolean[][] walked = new boolean[SIZE][SIZE];
   /** The one way from the start to the orb. */
   boolean[][] route = new boolean[SIZE][SIZE];
   int bx;
   int by;
   int dir;
   private boolean mustForward;
   /** 0 none, 1 forward, 2 turn left, 3 turn right. */
   private int action;
   private double actionLeft;
   private double angleFrom;
   private double angle;
   private double px;
   private double py;
   private double travelled;
   int steps;
   private double mapLeft;
   private double time;
   private int width;
   private int height;
   private Random random;
   private double[] depth = new double[0];
   /** Play (T): held down, the walker hurries, three times as fast. */
   private boolean hurry;

   @Override
   public String name() {
      return "Maze";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.width = width;
      this.height = height;
      this.random = random;
      this.hurry = false;
      this.build();
   }

   /** A new maze (every room reachable by exactly one way: a random depth-first carve), and the walker at its start. */
   void build() {
      this.open = new boolean[SIZE][SIZE];
      this.walked = new boolean[SIZE][SIZE];
      List<int[]> stack = new ArrayList<>();
      this.open[1][1] = true;
      stack.add(new int[] {1, 1});
      while (!stack.isEmpty()) {
         int[] here = stack.get(stack.size() - 1);
         int[] choices = new int[4];
         int count = 0;
         for (int d = 0; d < 4; d++) {
            int nx = here[0] + DX[d] * 2;
            int ny = here[1] + DY[d] * 2;
            if (nx > 0 && ny > 0 && nx < SIZE - 1 && ny < SIZE - 1 && !this.open[ny][nx]) {
               choices[count++] = d;
            }
         }
         if (count == 0) {
            stack.remove(stack.size() - 1);
            continue;
         }
         int d = choices[this.random.nextInt(count)];
         this.open[here[1] + DY[d]][here[0] + DX[d]] = true;
         this.open[here[1] + DY[d] * 2][here[0] + DX[d] * 2] = true;
         stack.add(new int[] {here[0] + DX[d] * 2, here[1] + DY[d] * 2});
      }
      this.route = this.findRoute();
      this.bx = 1;
      this.by = 1;
      this.dir = this.open[1][2] ? 0 : 1;
      this.angle = this.dir * Math.PI / 2;
      this.px = 1.5;
      this.py = 1.5;
      this.walked[1][1] = true;
      this.action = 0;
      this.mustForward = false;
      this.steps = 0;
      this.mapLeft = 0;
   }

   /** A breadth-first search from the start, then back from the goal along where each block was reached from. */
   private boolean[][] findRoute() {
      int[][] from = new int[SIZE][SIZE];
      for (int[] row : from) {
         Arrays.fill(row, -1);
      }
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      queue.add(new int[] {1, 1});
      from[1][1] = 4;
      while (!queue.isEmpty()) {
         int[] here = queue.poll();
         for (int d = 0; d < 4; d++) {
            int nx = here[0] + DX[d];
            int ny = here[1] + DY[d];
            if (this.openAt(nx, ny) && from[ny][nx] < 0) {
               from[ny][nx] = d;
               queue.add(new int[] {nx, ny});
            }
         }
      }
      boolean[][] way = new boolean[SIZE][SIZE];
      int x = SIZE - 2;
      int y = SIZE - 2;
      while (from[y][x] >= 0 && from[y][x] < 4) {
         way[y][x] = true;
         int d = from[y][x];
         x -= DX[d];
         y -= DY[d];
      }
      way[y][x] = from[y][x] == 4;
      return way;
   }

   boolean atGoal() {
      return this.bx == SIZE - 2 && this.by == SIZE - 2;
   }

   boolean showingMap() {
      return this.mapLeft > 0;
   }

   private boolean openAt(int x, int y) {
      return x >= 0 && y >= 0 && x < SIZE && y < SIZE && this.open[y][x];
   }

   private boolean canGo(int d) {
      return this.openAt(this.bx + DX[d], this.by + DY[d]);
   }

   /** The left-hand rule: turn left where it opens, else go on, else turn right. In a maze with one way through, it always arrives. */
   private void nextAction() {
      int left = (this.dir + 3) % 4;
      if (!this.mustForward && this.canGo(left)) {
         this.startTurn(2, left);
      } else if (this.canGo(this.dir)) {
         this.action = 1;
         this.actionLeft = MOVE_MILLIS;
         this.mustForward = false;
      } else {
         this.startTurn(3, (this.dir + 1) % 4);
      }
   }

   private void startTurn(int kind, int to) {
      this.action = kind;
      this.actionLeft = TURN_MILLIS;
      this.angleFrom = this.angle;
      this.dir = to;
      this.mustForward = kind == 2;
   }

   @Override
   public void touch(Point at, boolean down) {
      this.hurry = at != null && down;
   }

   @Override
   public void tick(double millis) {
      millis *= this.hurry ? 3 : 1;
      this.time += millis;
      while (millis > 0) {
         if (this.mapLeft > 0) {
            double used = Math.min(millis, this.mapLeft);
            this.mapLeft -= used;
            millis -= used;
            if (this.mapLeft <= 0) {
               this.build();
            }
            continue;
         }
         if (this.action == 0) {
            if (this.atGoal()) {
               this.mapLeft = MAP_MILLIS;
               Music.chord(Music.MAZE, 0.6, 0.85);
               continue;
            }
            this.nextAction();
         }
         double total = this.action == 1 ? MOVE_MILLIS : TURN_MILLIS;
         double used = Math.min(millis, this.actionLeft);
         this.actionLeft -= used;
         millis -= used;
         double t = 1 - this.actionLeft / total;
         if (this.action == 1) {
            this.px = this.bx + 0.5 + DX[this.dir] * t;
            this.py = this.by + 0.5 + DY[this.dir] * t;
            this.travelled += used / MOVE_MILLIS;
         } else {
            double eased = t * t * (3 - 2 * t);
            this.angle = this.angleFrom + (this.action == 2 ? -1 : 1) * Math.PI / 2 * eased;
         }
         if (this.actionLeft <= 0) {
            if (this.action == 1) {
               this.bx += DX[this.dir];
               this.by += DY[this.dir];
               this.walked[this.by][this.bx] = true;
               this.steps++;
               // Music (M): each room a harp note, rising as the walker nears the orb's corner; off the way through, softer.
               Music.ring(Music.MAZE, (this.bx + this.by - 2) / (2.0 * (SIZE - 3)), this.route[this.by][this.bx] ? 0.55 : 0.3);
               this.px = this.bx + 0.5;
               this.py = this.by + 0.5;
            } else {
               this.angle = this.dir * Math.PI / 2;
            }
            this.action = 0;
         }
      }
   }

   private static int shade(double distance) {
      double f = 1 / (1 + 0.035 * distance * distance);
      return (int)Math.max(1, Math.min(SHADES - 1, f * (SHADES - 1)));
   }

   @Override
   public void paint(Graphics2D g) {
      int w = this.width;
      int h = this.height;
      double horizon = h / 2.0 + Math.sin(this.travelled * Math.PI * 2) * h * 0.006;
      // Projection: the larger side spans about 70 degrees, so a phone held upright still sees a corridor.
      double proj = Math.max(w, h) * 0.5 / Math.tan(Math.toRadians(35));
      this.paintSkyAndFloor(g, w, h, (int)horizon);
      double dirX = Math.cos(this.angle);
      double dirY = Math.sin(this.angle);
      double k = w / 2.0 / proj;
      double planeX = -dirY * k;
      double planeY = dirX * k;
      int colW = Math.max(1, w / 320);
      int cols = (w + colW - 1) / colW;
      if (this.depth.length != cols) {
         this.depth = new double[cols];
      }
      for (int c = 0; c < cols; c++) {
         double camX = 2.0 * (c * colW + colW / 2.0) / w - 1;
         double rx = dirX + planeX * camX;
         double ry = dirY + planeY * camX;
         int mx = (int)this.px;
         int my = (int)this.py;
         double ddx = rx == 0 ? 1e30 : Math.abs(1 / rx);
         double ddy = ry == 0 ? 1e30 : Math.abs(1 / ry);
         int stepX = rx < 0 ? -1 : 1;
         int stepY = ry < 0 ? -1 : 1;
         double sideX = (rx < 0 ? this.px - mx : mx + 1 - this.px) * ddx;
         double sideY = (ry < 0 ? this.py - my : my + 1 - this.py) * ddy;
         int side = 0;
         for (int guard = 0; guard < SIZE * 4 && this.openAt(mx, my); guard++) {
            if (sideX < sideY) {
               sideX += ddx;
               mx += stepX;
               side = 0;
            } else {
               sideY += ddy;
               my += stepY;
               side = 1;
            }
         }
         double dist = Math.max(0.05, side == 0 ? sideX - ddx : sideY - ddy);
         this.depth[c] = dist;
         // Where along the wall's whole line the ray hit, so bricks run on across blocks.
         double along = side == 0 ? this.py + dist * ry : this.px + dist * rx;
         this.paintWall(g, c * colW, colW, horizon, proj / dist, along, side, shade(dist), side == 0 ? mx : my);
      }
      this.paintOrb(g, w, horizon, proj, dirX, dirY, planeX, planeY, colW);
      if (this.mapLeft > 0) {
         this.paintMap(g, w, h);
      }
   }

   private void paintSkyAndFloor(Graphics2D g, int w, int h, int horizon) {
      int bands = 24;
      for (int i = 0; i < bands; i++) {
         double f = i / (double)bands;
         int top = (int)(horizon * f);
         int bottom = (int)(horizon * (f + 1.0 / bands)) + 1;
         g.setColor(new Color((int)(34 - 26 * f), (int)(38 - 28 * f), (int)(62 - 40 * f)));
         g.fillRect(0, top, w, bottom - top);
         int ftop = horizon + (int)((h - horizon) * f);
         int fbottom = horizon + (int)((h - horizon) * (f + 1.0 / bands)) + 1;
         g.setColor(new Color((int)(14 + 70 * f), (int)(12 + 56 * f), (int)(10 + 40 * f)));
         g.fillRect(0, ftop, w, fbottom - ftop);
      }
   }

   /** One column of brick: four courses, each brick a little different, with mortar between. */
   private void paintWall(Graphics2D g, int x, int colW, double horizon, double lineH, double u, int side, int shade, int line) {
      double top = horizon - lineH / 2;
      int mortar = (int)Math.max(1, lineH * 0.035);
      for (int r = 0; r < ROWS; r++) {
         int y0 = (int)(top + lineH * r / ROWS);
         int y1 = (int)(top + lineH * (r + 1) / ROWS);
         if (y1 < 0 || y0 > this.height) {
            continue;
         }
         double offset = r % 2 == 0 ? 0 : 0.25;
         double along = (u + offset) * 2;
         int brick = (int)Math.floor(along);
         double inBrick = along - brick;
         boolean joint = inBrick < 0.05 || inBrick > 0.95;
         int variant = Math.floorMod(line * 31 + r * 7 + brick * 13 + (brick >> 1) * 5, 4);
         g.setColor(joint ? MORTAR[side][shade] : BRICK[variant][side][shade]);
         int a = Math.max(0, y0);
         int b = Math.min(this.height, y1);
         g.fillRect(x, a, colW, b - a);
         if (!joint) {
            g.setColor(MORTAR[side][shade]);
            g.fillRect(x, y0, colW, mortar);
         }
      }
   }

   /** The goal: a gold orb hovering at the far corner, drawn in slices so walls in front hide it. */
   private void paintOrb(Graphics2D g, int w, double horizon, double proj, double dirX, double dirY, double planeX, double planeY, int colW) {
      double rx = SIZE - 1.5 - this.px;
      double ry = SIZE - 1.5 - this.py;
      double inv = 1 / (planeX * dirY - dirX * planeY);
      double tx = inv * (dirY * rx - dirX * ry);
      double tz = inv * (-planeY * rx + planeX * ry);
      if (tz < 0.2) {
         return;
      }
      double sx = w / 2.0 * (1 + tx / tz);
      double cy = horizon + Math.sin(this.time / 500) * proj * 0.05 / tz;
      double pulse = 1 + Math.sin(this.time / 260) * 0.06;
      double[] radii = {0.34 * pulse, 0.2 * pulse, 0.1};
      Color[] colors = {new Color(255, 210, 90, 50), new Color(255, 196, 60), new Color(255, 248, 200)};
      for (int layer = 0; layer < radii.length; layer++) {
         double r = proj * radii[layer] / tz;
         g.setColor(colors[layer]);
         int first = Math.max(0, (int)((sx - r) / colW));
         int last = Math.min(this.depth.length - 1, (int)((sx + r) / colW));
         for (int c = first; c <= last; c++) {
            if (this.depth[c] < tz) {
               continue;
            }
            double dx = c * colW + colW / 2.0 - sx;
            double half = Math.sqrt(Math.max(0, r * r - dx * dx));
            if (half > 0) {
               g.fillRect(c * colW, (int)(cy - half), colW, (int)(half * 2) + 1);
            }
         }
      }
   }

   /** The maze from above, with the way the walker went, fading in over the last view. */
   private void paintMap(Graphics2D g, int w, int h) {
      double shown = MAP_MILLIS - this.mapLeft;
      double alpha = Math.min(1, Math.min(shown, this.mapLeft) / 400);
      g.setColor(new Color(0, 0, 0, (int)(170 * alpha)));
      g.fillRect(0, 0, w, h);
      int cell = Math.max(2, (int)(Math.min(w, h) * 0.78 / SIZE));
      int left = (w - cell * SIZE) / 2;
      int top = (h - cell * SIZE) / 2;
      for (int y = 0; y < SIZE; y++) {
         for (int x = 0; x < SIZE; x++) {
            // Walls, then the way through in gold, the dead ends the walker tried in dim amber, and the rest dark.
            Color c = !this.open[y][x] ? new Color(168, 70, 48) : this.route[y][x] ? new Color(255, 196, 60)
               : this.walked[y][x] ? new Color(120, 92, 40) : new Color(40, 34, 30);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(255 * alpha)));
            g.fillRect(left + x * cell, top + y * cell, cell, cell);
         }
      }
      String line = "Through in " + this.steps + " steps";
      int size = Math.max(12, Math.min(28, cell));
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, size));
      FontMetrics metrics = g.getFontMetrics();
      g.setColor(new Color(255, 236, 170, (int)(255 * alpha)));
      g.drawString(line, (w - metrics.stringWidth(line)) / 2, Math.max(size, top - size / 2));
   }
}
