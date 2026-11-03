package screensaver;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A painted aquarium whose fish are yours: they live in the Tank, so the same fish are there next time, a new one
 * moves in each day you look in, and they grow with the days and the flakes F drops. The water follows the clock:
 * sunlit by day, dark blue at night, when the fish slow down and rest low among the weed.
 */
class Aquarium implements Saver {
   /** How long a newcomer's name tag stays up, and everyone's when the aquarium is shown. */
   static final double NEW_TAG_MILLIS = 12_000;
   static final double TAG_MILLIS = 6_000;
   static final double FLAKE_REST_MILLIS = 6_000;
   private static final double BUBBLE_EVERY = 280;
   private static final int FLAKES_PER_FEED = 7;
   /** Body length as a share of the shorter screen side, for a fully grown fish of each species. */
   private static final double[] LENGTH = {0.10, 0.13, 0.08, 0.12, 0.11, 0.12};
   /** Cruising speed in body lengths per second. */
   private static final double[] CRUISE = {0.9, 0.8, 1.4, 0.55, 0.7, 0.75};

   private final Tank tank;
   private final Supplier<Date> now;
   private final Runnable changed;
   private Random random = new Random();
   private int width;
   private int height;
   private int unit;
   private int sandTop;
   private double time;
   private final List<Swimmer> swimmers = new ArrayList<>();
   private final List<double[]> flakes = new ArrayList<>();
   private final List<double[]> bubbles = new ArrayList<>();
   private double bubbleWait;
   private int[] sandX;
   private int[] sandY;
   private int[][] pebbles;
   private double[][] weeds;
   private int[][] rocks;
   private int bubblerX;
   private Font tagFont;

   /** One fish on screen, following a resident of the tank. */
   private final class Swimmer {
      final Tank.Fish fish;
      double x;
      double y;
      double vx;
      double vy;
      double targetX;
      double targetY;
      double wander;
      /** -1 facing left, 1 facing right; it passes through 0 as the fish turns, which narrows it. */
      double facing;
      double wag;
      double tagLeft;
      boolean isNew;

      Swimmer(Tank.Fish fish) {
         this.fish = fish;
      }

      double length() {
         double grown = Tank.growth(this.fish, Aquarium.this.today());
         return Math.max(14, Aquarium.this.unit * LENGTH[this.fish.species] * (0.6 + 0.4 * grown));
      }
   }

   Aquarium(Tank tank, Supplier<Date> now, Runnable changed) {
      this.tank = tank;
      this.now = now;
      this.changed = changed;
   }

   @Override
   public String name() {
      return "Aquarium";
   }

   int today() {
      return Tank.dayNumber(this.now.get());
   }

   /** The caption line about the tank. */
   String status() {
      int count = this.tank.fish.size();
      String fish = count + " fish";
      return count >= Tank.MAX_FISH ? "Your tank: " + fish + ", full. F: feed them"
         : "Your tank: " + fish + ", a new one each day you look in. F: feed them";
   }

   @Override
   public void reset(int width, int height, Random random) {
      this.random = random;
      this.width = width;
      this.height = height;
      this.unit = Math.min(width, height);
      this.sandTop = (int)(height * 0.84);
      this.tagFont = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(11, Math.min(20, this.unit / 40)));
      this.flakes.clear();
      this.bubbles.clear();
      this.buildScenery();
      List<Tank.Fish> newcomers = this.tank.arrive(this.today(), random);
      this.swimmers.clear();
      for (Tank.Fish fish : this.tank.fish) {
         Swimmer s = new Swimmer(fish);
         s.isNew = newcomers.contains(fish);
         this.place(s);
         this.swimmers.add(s);
      }
      if (!newcomers.isEmpty()) {
         this.changed.run();
      }
   }

   /** Puts a fish in the water, or just outside the glass for a newcomer, which swims in. */
   private void place(Swimmer s) {
      double len = s.length();
      if (s.isNew) {
         boolean left = this.random.nextBoolean();
         s.x = left ? -len : this.width + len;
         s.y = this.height * (0.25 + 0.4 * this.random.nextDouble());
         s.facing = left ? 1 : -1;
         s.targetX = this.width * (0.3 + 0.4 * this.random.nextDouble());
         s.targetY = s.y;
         s.wander = 5000;
         s.tagLeft = NEW_TAG_MILLIS;
      } else {
         s.x = len + this.random.nextDouble() * Math.max(1, this.width - 2 * len);
         s.y = this.top() + this.random.nextDouble() * Math.max(1, this.bottom() - this.top());
         s.facing = this.random.nextBoolean() ? 1 : -1;
         this.newTarget(s);
         s.tagLeft = TAG_MILLIS;
      }
      s.wag = this.random.nextDouble() * 10;
   }

   private double top() {
      return this.height * 0.12;
   }

   private double bottom() {
      return this.sandTop - this.unit * 0.05;
   }

   private void newTarget(Swimmer s) {
      double len = s.length();
      double low = this.top();
      double high = this.bottom();
      if (this.daylight() < 0.4) {
         // Resting: low in the tank, near the weed.
         low = this.top() + (high - this.top()) * 0.55;
      }
      s.targetX = len + this.random.nextDouble() * Math.max(1, this.width - 2 * len);
      s.targetY = low + this.random.nextDouble() * Math.max(1, high - low);
      // Tetras keep together: the second and later ones head near the first one's target.
      if (s.fish.species == 2) {
         for (Swimmer leader : this.swimmers) {
            if (leader.fish.species == 2) {
               if (leader != s) {
                  s.targetX = leader.targetX + (this.random.nextDouble() - 0.5) * len * 4;
                  s.targetY = leader.targetY + (this.random.nextDouble() - 0.5) * len * 2;
               }
               break;
            }
         }
      }
      s.wander = 2500 + this.random.nextDouble() * 5000;
   }

   private void buildScenery() {
      int points = 24;
      this.sandX = new int[points + 2];
      this.sandY = new int[points + 2];
      double phase = this.random.nextDouble() * 6;
      for (int i = 0; i < points; i++) {
         this.sandX[i] = (int)Math.round(this.width * i / (double)(points - 1));
         this.sandY[i] = (int)(this.sandTop + this.unit * 0.025 * Math.sin(phase + i * 0.55) + this.unit * 0.012 * Math.sin(i * 1.7));
      }
      this.sandX[points] = this.width;
      this.sandY[points] = this.height;
      this.sandX[points + 1] = 0;
      this.sandY[points + 1] = this.height;
      this.pebbles = new int[36][4];
      for (int[] p : this.pebbles) {
         int size = (int)(this.unit * (0.006 + 0.012 * this.random.nextDouble()));
         p[0] = this.random.nextInt(Math.max(1, this.width));
         p[1] = this.sandTop + (int)(this.random.nextDouble() * (this.height - this.sandTop)) + this.unit / 60;
         p[2] = Math.max(2, size);
         p[3] = this.random.nextInt(3);
      }
      this.rocks = new int[2][];
      for (int r = 0; r < this.rocks.length; r++) {
         int cx = (int)(this.width * (r == 0 ? 0.18 + 0.1 * this.random.nextDouble() : 0.68 + 0.12 * this.random.nextDouble()));
         int size = (int)(this.unit * (0.07 + 0.05 * this.random.nextDouble()));
         this.rocks[r] = new int[] {cx, size};
      }
      int weedCount = Math.max(5, Math.min(11, this.width / Math.max(1, this.unit / 6)));
      this.weeds = new double[weedCount][];
      for (int i = 0; i < weedCount; i++) {
         double x = this.width * (i + 0.2 + 0.6 * this.random.nextDouble()) / weedCount;
         double tall = this.height * (0.22 + 0.25 * this.random.nextDouble());
         this.weeds[i] = new double[] {x, tall, this.random.nextDouble() * 6, 0.6 + 0.4 * this.random.nextDouble()};
      }
      this.bubblerX = (int)(this.width * (0.4 + 0.2 * this.random.nextDouble()));
   }

   /** 1 in full daylight, 0 at night, easing between at dawn (5:30 to 7:00) and dusk (19:00 to 20:30). */
   double daylight() {
      Date date = this.now.get();
      double hour = date.getHours() + date.getMinutes() / 60.0;
      if (hour < 5.5 || hour >= 20.5) {
         return 0;
      }
      if (hour < 7) {
         return (hour - 5.5) / 1.5;
      }
      if (hour < 19) {
         return 1;
      }
      return (20.5 - hour) / 1.5;
   }

   /** Drops a pinch of flakes near the top at a random spot; every fish goes for the nearest one. */
   void feed() {
      double x = this.width * (0.2 + 0.6 * this.random.nextDouble());
      for (int i = 0; i < FLAKES_PER_FEED; i++) {
         this.flakes.add(new double[] {x + (this.random.nextDouble() - 0.5) * this.unit * 0.12,
            this.top() * 0.5 + this.random.nextDouble() * this.unit * 0.03, this.random.nextDouble() * 6, 0, this.random.nextInt(3)});
      }
      for (Swimmer s : this.swimmers) {
         s.tagLeft = Math.max(s.tagLeft, 1500);
      }
   }

   void showNames() {
      for (Swimmer s : this.swimmers) {
         s.tagLeft = Math.max(s.tagLeft, TAG_MILLIS);
      }
   }

   int flakeCount() {
      return this.flakes.size();
   }

   @Override
   public void tick(double millis) {
      this.time += millis;
      double night = 1 - this.daylight();
      this.tickFlakes(millis);
      boolean ate = false;
      for (Swimmer s : this.swimmers) {
         ate |= this.tickFish(s, millis, night);
      }
      if (ate) {
         this.changed.run();
      }
      this.tickBubbles(millis);
   }

   private void tickFlakes(double millis) {
      Iterator<double[]> it = this.flakes.iterator();
      while (it.hasNext()) {
         double[] f = it.next();
         double floor = this.sandTop + this.unit * 0.01;
         if (f[1] < floor) {
            f[1] = Math.min(floor, f[1] + millis * this.unit * 0.00005);
            f[0] += Math.sin(this.time * 0.003 + f[2]) * millis * this.unit * 0.00002;
         } else {
            f[3] += millis;
            if (f[3] > FLAKE_REST_MILLIS) {
               it.remove();
            }
         }
      }
   }

   /** Swims one fish a step; returns true if it ate a flake. */
   private boolean tickFish(Swimmer s, double millis, double night) {
      double len = s.length();
      double[] food = this.nearestFlake(s);
      double tx = food != null ? food[0] - s.facing * len * 0.45 : s.targetX;
      double ty = food != null ? food[1] : s.targetY;
      s.wander -= millis;
      double dx = tx - s.x;
      double dy = ty - s.y;
      double distance = Math.hypot(dx, dy);
      if (food == null && (distance < len * 0.6 || s.wander <= 0)) {
         this.newTarget(s);
      }
      double cruise = CRUISE[s.fish.species] * len / 1000 * (food != null ? 2.4 : 1 - 0.65 * night);
      double want = Math.min(cruise, distance / 400);
      double wantX = distance > 0 ? dx / distance * want : 0;
      double wantY = distance > 0 ? dy / distance * want * (food != null ? 0.8 : 0.45) : 0;
      double ease = Math.min(1, millis / (food != null ? 250 : 700));
      s.vx += (wantX - s.vx) * ease;
      s.vy += (wantY - s.vy) * ease;
      s.x += s.vx * millis;
      s.y += s.vy * millis;
      double wantFacing = Math.abs(s.vx) < cruise * 0.08 ? s.facing : Math.signum(s.vx);
      double turn = millis / 260;
      s.facing = s.facing < wantFacing ? Math.min(wantFacing, s.facing + turn) : Math.max(wantFacing, s.facing - turn);
      double pace = Math.hypot(s.vx, s.vy) / Math.max(1e-6, cruise);
      s.wag += millis * (0.004 + 0.012 * Math.min(2, pace));
      s.tagLeft = Math.max(0, s.tagLeft - millis);
      if (food != null) {
         double mouthX = s.x + Math.signum(s.facing == 0 ? 1 : s.facing) * len * 0.5;
         if (Math.hypot(food[0] - mouthX, food[1] - s.y) < len * 0.3) {
            this.flakes.remove(food);
            s.fish.meals++;
            this.bubbles.add(new double[] {mouthX, s.y - len * 0.1, Math.max(2, len * 0.06), this.random.nextDouble() * 6});
            return true;
         }
      }
      return false;
   }

   /** The nearest flake still sinking or lying on the sand, or null. */
   private double[] nearestFlake(Swimmer s) {
      double[] best = null;
      double bestDistance = Double.MAX_VALUE;
      for (double[] f : this.flakes) {
         double d = Math.hypot(f[0] - s.x, f[1] - s.y);
         if (d < bestDistance) {
            bestDistance = d;
            best = f;
         }
      }
      return best;
   }

   private void tickBubbles(double millis) {
      this.bubbleWait -= millis;
      while (this.bubbleWait <= 0) {
         this.bubbleWait += BUBBLE_EVERY * (0.5 + this.random.nextDouble());
         this.bubbles.add(new double[] {this.bubblerX + (this.random.nextDouble() - 0.5) * this.unit * 0.01, this.sandTop,
            this.unit * (0.004 + 0.006 * this.random.nextDouble()), this.random.nextDouble() * 6});
      }
      Iterator<double[]> it = this.bubbles.iterator();
      while (it.hasNext()) {
         double[] b = it.next();
         b[1] -= millis * this.unit * (0.00012 + b[2] / this.unit * 0.01);
         b[0] += Math.sin(this.time * 0.004 + b[3]) * millis * this.unit * 0.00003;
         b[2] += millis * this.unit * 0.0000015;
         if (b[1] < this.top() * 0.35) {
            it.remove();
         }
      }
   }

   // ---- painting ----

   @Override
   public void paint(Graphics2D g) {
      double day = this.daylight();
      this.paintWater(g);
      this.paintRays(g, day);
      this.paintSand(g);
      for (int i = 0; i < this.weeds.length; i += 2) {
         this.paintWeed(g, this.weeds[i], true);
      }
      for (int[] rock : this.rocks) {
         this.paintRock(g, rock);
      }
      for (Swimmer s : this.swimmers) {
         this.paintFish(g, s);
      }
      for (int i = 1; i < this.weeds.length; i += 2) {
         this.paintWeed(g, this.weeds[i], false);
      }
      this.paintFlakes(g);
      this.paintBubbles(g);
      if (day < 1) {
         g.setColor(new Color(0, 6, 30, (int)(150 * (1 - day))));
         g.fillRect(0, 0, this.width, this.height);
      }
      this.paintTags(g);
   }

   private static int mix(int a, int b, double t) {
      return (int)Math.round(a + (b - a) * Math.max(0, Math.min(1, t)));
   }

   private void paintWater(Graphics2D g) {
      int bands = 14;
      for (int i = 0; i < bands; i++) {
         double t = i / (double)(bands - 1);
         g.setColor(new Color(mix(40, 6, t), mix(160, 70, t), mix(210, 120, t)));
         int y0 = this.height * i / bands;
         g.fillRect(0, y0, this.width, this.height * (i + 1) / bands - y0 + 1);
      }
      // The surface: a bright rippling line.
      g.setColor(new Color(200, 240, 255, 110));
      int step = Math.max(8, this.width / 60);
      int surface = (int)(this.top() * 0.3);
      int lastY = surface;
      for (int x = 0; x <= this.width; x += step) {
         int y = surface + (int)(Math.sin(this.time * 0.002 + x * 0.02) * this.unit * 0.004);
         if (x > 0) {
            g.drawLine(x - step, lastY, x, y);
         }
         lastY = y;
      }
      g.setColor(new Color(255, 255, 255, 30));
      g.fillRect(0, 0, this.width, surface);
   }

   private void paintRays(Graphics2D g, double day) {
      int alpha = (int)(26 * (0.25 + 0.75 * day));
      for (int i = 0; i < 5; i++) {
         double sway = Math.sin(this.time * 0.00025 + i * 1.9) * this.width * 0.03;
         double x = this.width * (0.1 + i * 0.2) + sway;
         double top = this.width * 0.025 * (1 + i % 2);
         double spread = this.width * 0.08;
         g.setColor(new Color(220, 245, 255, alpha));
         g.fillPolygon(new int[] {(int)(x - top), (int)(x + top), (int)(x + top + spread + this.width * 0.05), (int)(x - top + spread)},
            new int[] {0, 0, this.sandTop, this.sandTop}, 4);
      }
   }

   private void paintSand(Graphics2D g) {
      g.setColor(new Color(196, 172, 120));
      g.fillPolygon(this.sandX, this.sandY, this.sandX.length);
      int[] shadowY = new int[this.sandY.length];
      for (int i = 0; i < shadowY.length; i++) {
         shadowY[i] = i < shadowY.length - 2 ? this.sandY[i] + this.unit / 30 : this.sandY[i];
      }
      g.setColor(new Color(170, 146, 98));
      g.fillPolygon(this.sandX, shadowY, this.sandX.length);
      Color[] stone = {new Color(140, 120, 100), new Color(210, 200, 180), new Color(110, 105, 100)};
      for (int[] p : this.pebbles) {
         g.setColor(stone[p[3]]);
         g.fillOval(p[0], p[1], p[2] * 3 / 2, p[2]);
      }
      // The bubbler, a little stone with a hole in it.
      int s = Math.max(6, this.unit / 40);
      g.setColor(new Color(90, 90, 100));
      g.fillOval(this.bubblerX - s, this.sandTop - s / 2, s * 2, s);
   }

   private void paintRock(Graphics2D g, int[] rock) {
      int cx = rock[0];
      int r = rock[1];
      int base = this.sandTop + r / 5;
      g.setColor(new Color(92, 98, 108));
      g.fillPolygon(new int[] {cx - r, cx - r * 3 / 4, cx - r / 4, cx + r / 3, cx + r * 4 / 5, cx + r},
         new int[] {base, base - r * 2 / 3, base - r, base - r * 4 / 5, base - r / 2, base}, 6);
      g.setColor(new Color(128, 136, 146));
      g.fillPolygon(new int[] {cx - r * 3 / 4, cx - r / 4, cx + r / 3, cx + r / 6, cx - r / 2},
         new int[] {base - r * 2 / 3, base - r, base - r * 4 / 5, base - r / 2, base - r / 2}, 5);
   }

   /** A strand of weed: a tapering band whose sway grows toward the tip. */
   private void paintWeed(Graphics2D g, double[] weed, boolean back) {
      int segments = 9;
      int[] xs = new int[segments * 2 + 2];
      int[] ys = new int[segments * 2 + 2];
      double base = weed[0];
      double tall = weed[1];
      double baseWidth = this.unit * 0.022 * weed[3];
      for (int i = 0; i <= segments; i++) {
         double t = i / (double)segments;
         double y = this.sandTop + this.unit * 0.01 - tall * t;
         double x = base + Math.sin(this.time * 0.0011 + weed[2] + t * 2.2) * t * t * this.unit * 0.06;
         double half = baseWidth * (1 - t * 0.85) / 2;
         xs[i] = (int)(x - half);
         ys[i] = (int)y;
         xs[segments * 2 + 1 - i] = (int)(x + half);
         ys[segments * 2 + 1 - i] = (int)y;
      }
      g.setColor(back ? new Color(30, 110, 70) : new Color(50, 150, 70));
      g.fillPolygon(xs, ys, xs.length);
   }

   private void paintFlakes(Graphics2D g) {
      Color[] colors = {new Color(230, 120, 40), new Color(200, 70, 50), new Color(120, 170, 60)};
      int s = Math.max(3, this.unit / 140);
      for (double[] f : this.flakes) {
         int alpha = f[3] > FLAKE_REST_MILLIS - 1000 ? (int)(255 * (FLAKE_REST_MILLIS - f[3]) / 1000) : 255;
         Color c = colors[(int)f[4]];
         g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, alpha)));
         g.fillRect((int)f[0] - s / 2, (int)f[1] - s / 2, s, s);
      }
   }

   private void paintBubbles(Graphics2D g) {
      for (double[] b : this.bubbles) {
         int d = (int)Math.max(3, b[2] * 2);
         g.setColor(new Color(220, 245, 255, 150));
         g.drawOval((int)(b[0] - d / 2.0), (int)(b[1] - d / 2.0), d, d);
         g.setColor(new Color(255, 255, 255, 170));
         g.fillOval((int)(b[0] - d / 4.0), (int)(b[1] - d / 3.0), Math.max(1, d / 4), Math.max(1, d / 4));
      }
   }

   /** A fish drawn 100 units long, nose to the right, scaled and turned to where it is going. */
   private void paintFish(Graphics2D g, Swimmer s) {
      double len = s.length();
      Graphics2D f = (Graphics2D)g.create();
      f.translate(s.x, s.y);
      double heading = Math.atan2(s.vy, Math.max(1e-6, Math.abs(s.vx)));
      heading = Math.max(-0.3, Math.min(0.3, heading * 0.6));
      double facing = Math.abs(s.facing) < 0.12 ? Math.copySign(0.12, s.facing == 0 ? 1 : s.facing) : s.facing;
      f.rotate(facing > 0 ? heading : -heading);
      f.scale(facing * len / 100, len / 100);
      int tail = (int)(Math.sin(s.wag) * 9);
      switch (s.fish.species) {
         case 0:
            this.clownfish(f, tail);
            break;
         case 1:
            this.blueTang(f, tail);
            break;
         case 2:
            this.neonTetra(f, tail);
            break;
         case 3:
            this.angelfish(f, tail);
            break;
         case 4:
            this.goldfish(f, tail);
            break;
         default:
            this.yellowTang(f, tail);
            break;
      }
      f.dispose();
   }

   private static void eye(Graphics2D g, int x, int y, int r) {
      g.setColor(Color.WHITE);
      g.fillOval(x - r, y - r, r * 2, r * 2);
      g.setColor(new Color(20, 20, 30));
      g.fillOval(x - r / 2, y - r / 2 - 1, r + 1, r + 1);
   }

   private static void shine(Graphics2D g, int x, int y, int w, int h) {
      g.setColor(new Color(255, 255, 255, 55));
      g.fillOval(x, y, w, h);
   }

   private static void poly(Graphics2D g, Color c, int[] xs, int[] ys) {
      g.setColor(c);
      g.fillPolygon(xs, ys, xs.length);
   }

   private void clownfish(Graphics2D g, int tail) {
      Color orange = new Color(255, 118, 26);
      Color black = new Color(30, 20, 20);
      poly(g, black, new int[] {-38, -62, -64, -62}, new int[] {0, -18 + tail, tail, 18 + tail});
      poly(g, orange, new int[] {-40, -58, -60, -58}, new int[] {0, -15 + tail, tail, 15 + tail});
      poly(g, black, new int[] {-20, -8, 14, 4}, new int[] {-18, -30, -22, -16});
      poly(g, orange, new int[] {-18, -8, 11, 4}, new int[] {-17, -26, -20, -16});
      g.setColor(black);
      g.fillOval(-48, -22, 98, 44);
      g.setColor(orange);
      g.fillOval(-46, -20, 94, 40);
      int[][] bands = {{20, 9, 34}, {-6, 10, 40}, {-32, 7, 26}};
      for (int[] band : bands) {
         g.setColor(black);
         g.fillOval(band[0] - band[1] / 2 - 2, -band[2] / 2 - 1, band[1] + 4, band[2] + 2);
         g.setColor(Color.WHITE);
         g.fillOval(band[0] - band[1] / 2, -band[2] / 2, band[1], band[2]);
      }
      g.setColor(orange.darker());
      g.fillOval(4, 4, 16, 9);
      shine(g, -20, -17, 50, 10);
      eye(g, 34, -5, 5);
   }

   private void blueTang(Graphics2D g, int tail) {
      Color blue = new Color(34, 86, 225);
      Color black = new Color(16, 20, 40);
      Color yellow = new Color(255, 214, 40);
      poly(g, yellow, new int[] {-40, -66, -58, -66}, new int[] {0, -22 + tail, tail, 22 + tail});
      poly(g, black, new int[] {-40, -30, 20, 40, 10}, new int[] {-20, -30, -32, -22, -22});
      poly(g, black, new int[] {-36, -20, 18, 0}, new int[] {18, 28, 26, 18});
      g.setColor(blue);
      g.fillOval(-46, -27, 98, 54);
      poly(g, black, new int[] {-38, -16, 10, 26, 10, -12, -30}, new int[] {-4, -16, -14, -4, -8, -6, 6});
      g.setColor(yellow);
      g.fillOval(-44, -4, 10, 8);
      shine(g, -14, -22, 48, 10);
      eye(g, 32, -6, 5);
   }

   private void neonTetra(Graphics2D g, int tail) {
      Color body = new Color(196, 210, 214, 230);
      poly(g, new Color(190, 200, 205, 170), new int[] {-36, -64, -56, -64}, new int[] {0, -16 + tail, tail, 16 + tail});
      poly(g, body, new int[] {-10, 0, 10}, new int[] {-12, -24, -12});
      g.setColor(body);
      g.fillOval(-46, -14, 96, 28);
      poly(g, new Color(215, 30, 55), new int[] {-42, -8, 10, -6, -36}, new int[] {2, 0, 4, 12, 9});
      poly(g, new Color(40, 210, 255), new int[] {-36, -10, 20, 40, 18, -8}, new int[] {-3, -7, -7, -4, -1, -1});
      eye(g, 34, -2, 5);
   }

   private void angelfish(Graphics2D g, int tail) {
      Color silver = new Color(224, 224, 210);
      Color stripe = new Color(40, 36, 30);
      Color fin = new Color(232, 232, 220, 190);
      poly(g, fin, new int[] {-30, -54, -48, -54}, new int[] {0, -20 + tail, tail, 20 + tail});
      // Tall fins swept back from the top and bottom of the body, the angelfish's arrowhead outline.
      poly(g, fin, new int[] {16, -4, -34 + tail / 2, -30, -22}, new int[] {-22, -50, -88, -40, -20});
      poly(g, fin, new int[] {16, -4, -34 + tail / 2, -30, -22}, new int[] {22, 50, 88, 40, 20});
      g.setColor(silver);
      g.fillOval(-36, -30, 80, 60);
      poly(g, silver, new int[] {34, 50, 34}, new int[] {-12, 0, 12});
      poly(g, stripe, new int[] {18, 24, 24, 18}, new int[] {-26, -24, 24, 26});
      poly(g, stripe, new int[] {-6, 0, -16, -22}, new int[] {-30, -30, -78, -76});
      g.fillRect(-6, -30, 6, 60);
      poly(g, stripe, new int[] {-6, 0, -16, -22}, new int[] {30, 30, 78, 76});
      poly(g, stripe, new int[] {-28, -23, -23, -28}, new int[] {-18, -20, 20, 18});
      shine(g, -10, -24, 40, 10);
      eye(g, 32, -6, 5);
   }

   private void goldfish(Graphics2D g, int tail) {
      Color gold = new Color(250, 152, 30);
      Color fin = new Color(255, 180, 70, 170);
      poly(g, fin, new int[] {-34, -78, -68, -50}, new int[] {0, -30 + tail * 2, -6 + tail, 0});
      poly(g, fin, new int[] {-34, -50, -68, -78}, new int[] {0, 0, 6 + tail, 30 + tail * 2});
      poly(g, fin, new int[] {-14, 0, 18, 10}, new int[] {-18, -36, -20, -14});
      g.setColor(gold);
      g.fillOval(-42, -24, 90, 48);
      g.setColor(new Color(255, 205, 110));
      g.fillOval(-20, 4, 52, 16);
      poly(g, fin, new int[] {8, 0, 20}, new int[] {14, 30, 16});
      shine(g, -12, -19, 44, 10);
      eye(g, 30, -6, 6);
   }

   private void yellowTang(Graphics2D g, int tail) {
      Color yellow = new Color(255, 222, 34);
      poly(g, yellow, new int[] {-36, -60, -54, -60}, new int[] {0, -22 + tail, tail, 22 + tail});
      poly(g, yellow, new int[] {-30, -10, 24, 26}, new int[] {-14, -40, -30, -14});
      poly(g, yellow, new int[] {-30, -10, 24, 26}, new int[] {14, 40, 30, 14});
      g.setColor(yellow);
      g.fillOval(-40, -30, 84, 60);
      poly(g, yellow, new int[] {36, 54, 54, 36}, new int[] {-8, -3, 2, 6});
      g.setColor(Color.WHITE);
      g.fillOval(-36, -3, 7, 6);
      shine(g, -14, -25, 42, 12);
      eye(g, 26, -10, 5);
   }

   /** Names above the fish: everyone's for a moment when the aquarium is shown, longer for a newcomer. */
   private void paintTags(Graphics2D g) {
      g.setFont(this.tagFont);
      FontMetrics metrics = g.getFontMetrics();
      for (Swimmer s : this.swimmers) {
         if (s.tagLeft <= 0) {
            continue;
         }
         double alpha = Math.min(1, s.tagLeft / 800);
         String text = s.fish.name;
         if (s.fish.born == this.today()) {
            text = s.fish.title() + ", new today";
         }
         int w = metrics.stringWidth(text);
         int h = metrics.getHeight();
         int x = (int)Math.max(4, Math.min(this.width - w - 12, s.x - w / 2.0 - 4));
         int y = (int)Math.max(4, s.y - s.length() * 0.55 - h - 6);
         boolean fresh = s.fish.born == this.today();
         g.setColor(new Color(0, 20, 40, (int)(150 * alpha)));
         g.fillRoundRect(x, y, w + 10, h + 2, h, h);
         g.setColor(fresh ? new Color(255, 225, 120, (int)(255 * alpha)) : new Color(235, 245, 255, (int)(235 * alpha)));
         g.drawString(text, x + 5, y + 1 + metrics.getAscent());
      }
   }
}
