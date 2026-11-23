package dinosaurgameplayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * T after a crash: the whole run in one picture, like a long exposure. The run is played again from its course and
 * keys (a Ghost), and every step leaves the dinosaur's outline at its place on the course, so the picture is a ribbon
 * that rises over each cactus, dips under each bird, and stops at the crash. The course wraps into rows like lines of
 * text, shrunk to fit. Nights are shaded, and with a ghost raced its run is drawn too, see-through.
 */
final class RunPicture {
   static final Color TRAIL = new Color(0x2F6FAE);
   private static final Color NIGHT = new Color(0xDEDFE8);
   private static final Color CRASH = new Color(0xC62828);
   private static final int LEFT = 40;
   private static final int RIGHT = Runner.WIDTH - 10;
   private static final int TOP = 30;
   private static final int BOTTOM = RunnerPanel.STRIP - 4;
   /** The highest a dinosaur's head ever gets above the ground, held jump and all, plus room for the label. */
   private static final int TALLEST = 150;

   /** One run played again: per step its place on the course, its top and bottom above the ground, and night. */
   static final class Trail {
      final List<double[]> steps = new ArrayList<>();
      /** Every obstacle the run met, at its place on the course: x, width, bottom and top above the ground, bird. */
      final List<int[]> obstacles = new ArrayList<>();
      final int score;
      int jumps;
      int ducks;

      Trail(Ghost run) {
         run.start();
         Runner r = run.runner();
         Sky sky = new Sky();
         IdentityHashMap<Runner.Obstacle, Boolean> seen = new IdentityHashMap<>();
         boolean wasJumping = false;
         boolean wasDucking = false;
         while (r.state() == Runner.State.RUNNING) {
            run.step();
            sky.step(r.score());
            int bottom = Runner.GROUND - r.dinoTop() - (r.isDucking() ? Runner.DUCK_H : Runner.DINO_H);
            this.steps.add(new double[] { r.distance() + r.dinoX(), bottom, Runner.GROUND - r.dinoTop(), sky.darkness() });
            for (Runner.Obstacle o : r.obstacles()) {
               if (seen.put(o, Boolean.TRUE) == null) {
                  this.obstacles.add(new int[] { (int) Math.round(o.x + r.distance()), o.w, o.lift, o.lift + o.h,
                     o.kind == Runner.Kind.BIRD ? 1 : 0 });
               }
            }
            this.jumps += r.isJumping() && !wasJumping ? 1 : 0;
            this.ducks += r.isDucking() && !wasDucking ? 1 : 0;
            wasJumping = r.isJumping();
            wasDucking = r.isDucking();
         }
         this.score = r.score();
      }

      /** Where the dinosaur's front was on the course at the end. */
      double end() {
         return this.steps.isEmpty() ? 0 : this.steps.get(this.steps.size() - 1)[0];
      }
   }

   final Trail you;
   /** The ghost raced, or null. */
   final Trail ghost;
   /** Course pixels per picture pixel, and rows. */
   final int shrink;
   final int rows;

   RunPicture(Ghost you, Ghost ghost) {
      this.you = new Trail(you);
      this.ghost = ghost == null ? null : new Trail(ghost);
      double length = Math.max(this.you.end(), this.ghost == null ? 0 : this.ghost.end()) + Runner.DINO_W;
      int k = 1;
      // As little shrinking as lets the rows, each as tall as the tallest jump, fit the height.
      while ((BOTTOM - TOP) / Math.max(1, rowsAt(length, k)) < TALLEST / k + 4) {
         k++;
      }
      this.shrink = k;
      this.rows = rowsAt(length, k);
   }

   private static int rowsAt(double length, int k) {
      return (int) Math.ceil(length / ((RIGHT - LEFT) * (double) k));
   }

   int rowHeight() {
      return (BOTTOM - TOP) / this.rows;
   }

   void paint(Graphics2D g) {
      g.setColor(RunnerPanel.PAPER);
      g.fillRect(0, 0, Runner.WIDTH, RunnerPanel.STRIP);
      g.setFont(RunnerPanel.TEXT);
      g.setColor(RunnerPanel.INK);
      g.drawString("The whole run: " + this.you.score + " points, " + this.you.jumps + (this.you.jumps == 1 ? " jump" : " jumps")
         + (this.you.ducks > 0 ? ", " + this.you.ducks + (this.you.ducks == 1 ? " duck" : " ducks") : ""), 12, 19);
      g.setFont(RunnerPanel.TINY);
      g.setColor(RunnerPanel.MUTED);
      boolean night = false;
      for (double[] s : this.you.steps) {
         night |= s[3] > 0;
      }
      String help = (this.ghost != null ? "gray: your ghost    " : "") + (night ? "shaded: night    " : "") + "T or Space: go on";
      g.drawString(help, Runner.WIDTH - 12 - g.getFontMetrics().stringWidth(help), 18);
      int h = this.rowHeight();
      double rowLength = (RIGHT - LEFT) * (double) this.shrink;
      for (int row = 0; row < this.rows; row++) {
         int ground = TOP + (row + 1) * h - 2;
         g.setColor(RunnerPanel.MUTED);
         String at = String.valueOf((int) (row * rowLength * 0.025));
         g.drawString(at, LEFT - 6 - g.getFontMetrics().stringWidth(at), ground);
         this.paintNights(g, row, ground, h);
         g.setColor(RunnerPanel.INK);
         g.fillRect(LEFT, ground, RIGHT - LEFT, 1);
      }
      if (this.ghost != null) {
         // Fallen at about the same place, the ghost's label goes above yours.
         int lift = Math.abs(this.ghost.end() - this.you.end()) / this.shrink < 60 ? 11 : 0;
         this.paintTrail(g, this.ghost, new Color(83, 83, 83, 70), new Color(83, 83, 83, 160), "ghost", lift);
      }
      for (int[] o : this.you.obstacles) {
         this.paintObstacle(g, o);
      }
      if (this.ghost != null) {
         for (int[] o : this.ghost.obstacles) {
            if (o[0] > this.you.end()) {
               this.paintObstacle(g, o);   // the course past your crash, which only the ghost reached
            }
         }
      }
      this.paintTrail(g, this.you, TRAIL, CRASH, "crash", 0);
   }

   /** Night on row: a shaded column for each step it was dark at. */
   private void paintNights(Graphics2D g, int row, int ground, int h) {
      for (double[] s : this.you.steps) {
         if (s[3] > 0) {
            int[] p = this.at(s[0]);
            if (p[1] == row) {
               g.setColor(Sky.mix(RunnerPanel.PAPER, NIGHT, s[3]));
               g.fillRect(p[0], ground - h + 3, 2, h - 3);
            }
         }
      }
   }

   private void paintObstacle(Graphics2D g, int[] o) {
      int[] p = this.at(o[0]);
      int ground = TOP + (p[1] + 1) * this.rowHeight() - 2;
      int w = Math.max(1, o[1] / this.shrink);
      int top = ground - o[3] / this.shrink;
      int tall = Math.max(1, (o[3] - o[2]) / this.shrink);
      g.setColor(o[4] == 1 ? RunnerPanel.BIRD : RunnerPanel.CACTUS);
      if (o[4] == 1) {
         g.fillOval(p[0], top, w, tall);
      } else {
         g.fillRect(p[0], top, w, tall);
      }
   }

   /** One run's ribbon: the dinosaur's outline at every step, and where it crashed. */
   private void paintTrail(Graphics2D g, Trail t, Color color, Color end, String label, int lift) {
      g.setColor(color);
      int lastX = Integer.MIN_VALUE;
      for (double[] s : t.steps) {
         int[] p = this.at(s[0] - Runner.DINO_W / 2.0);
         int ground = TOP + (p[1] + 1) * this.rowHeight() - 2;
         int top = ground - (int) Math.round(s[2] / this.shrink);
         // Each step fills the columns since the last one, so the ribbon has no gaps (and a see-through one no overlaps).
         int x = p[0] > lastX && p[0] - lastX < 8 ? lastX + 1 : p[0];
         if (p[0] > lastX || p[0] < lastX - 8) {
            g.fillRect(x, top, p[0] - x + 1, Math.max(1, (int) Math.round((s[2] - s[1]) / this.shrink)));
            lastX = p[0];
         }
      }
      if (t.steps.isEmpty()) {
         return;
      }
      double[] last = t.steps.get(t.steps.size() - 1);
      int[] p = this.at(last[0]);
      int ground = TOP + (p[1] + 1) * this.rowHeight() - 2;
      int y = ground - (int) Math.round((last[1] + last[2]) / 2 / this.shrink);
      g.setColor(end);
      g.setStroke(new BasicStroke(2f));
      g.drawLine(p[0] - 3, y - 3, p[0] + 3, y + 3);
      g.drawLine(p[0] - 3, y + 3, p[0] + 3, y - 3);
      g.setStroke(new BasicStroke(1f));
      g.setFont(RunnerPanel.TINY);
      String s = label + " " + t.score;
      int x = Math.min(p[0] + 6, RIGHT - g.getFontMetrics().stringWidth(s));
      g.drawString(s, x, Math.max(TOP + 8 - lift, ground - (int) Math.round(last[2] / this.shrink) - 3 - lift));
   }

   /** A place on the course as a picture x and its row. */
   int[] at(double x) {
      double rowLength = (RIGHT - LEFT) * (double) this.shrink;
      int row = (int) Math.max(0, Math.min(this.rows - 1, Math.floor(x / rowLength)));
      return new int[] { LEFT + (int) Math.round((x - row * rowLength) / this.shrink), row };
   }
}
