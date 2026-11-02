package dinosaurgameplayer;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Draws an evolution: the course where the race is, every dinosaur still running as a colored ghost with its two
 * eyes (red jumps, blue ducks), the best-ranked one still running solid with its eyes labeled, the generation
 * badge, the record and a chart of each generation's best.
 */
final class EvolveView {
   /** A see-through color for each dinosaur of a generation, round the color wheel. */
   private static final Color[] GHOSTS = new Color[Evolution.SIZE];

   static {
      for (int i = 0; i < GHOSTS.length; i++) {
         Color c = Color.getHSBColor(i / (float) GHOSTS.length, 0.6f, 0.8f);
         GHOSTS[i] = new Color(c.getRed(), c.getGreen(), c.getBlue(), 80);
      }
   }
   private static final Color JUMP_EYE = new Color(230, 40, 40);
   private static final Color JUMP_EYE_FAINT = new Color(230, 40, 40, 110);
   private static final Color DUCK_EYE = new Color(30, 110, 230);
   private static final Color DUCK_EYE_FAINT = new Color(30, 110, 230, 110);
   private static final Color RECORD = new Color(0x2E7D32);
   private static final Color BAR = new Color(0x53, 0x53, 0x53, 90);
   private static final int CHART_W = 150;
   private static final int CHART_H = 26;
   private static final String ORIGINAL = Brain.original().encode();

   private EvolveView() {
   }

   static void paint(Graphics2D g, Evolution ev, int speed) {
      Evolution.Dino lead = ev.leader();
      RunnerPanel.paintWorld(g, lead.runner);
      List<Evolution.Dino> dinos = ev.dinos();
      for (int i = dinos.size() - 1; i >= 0; i--) {
         Evolution.Dino d = dinos.get(i);
         if (d != lead && d.running()) {
            RunnerPanel.paintDino(g, d.runner, GHOSTS[i % GHOSTS.length]);
            eye(g, d.brain.jumpX, d.brain.jumpY, 4, JUMP_EYE_FAINT);
            if (d.brain.ducks()) {
               eye(g, d.brain.duckX, d.brain.duckY, 4, DUCK_EYE_FAINT);
            }
         }
      }
      RunnerPanel.paintDino(g, lead.runner, RunnerPanel.INK);
      paintLead(g, lead);
      paintBadge(g, ev, speed);
      paintScore(g, ev, lead);
      paintChart(g, ev);
      String[] result = ev.result();
      if (result != null) {
         g.setColor(ev.newRecord() ? RECORD : RunnerPanel.INK);
         g.setFont(RunnerPanel.TITLE);
         RunnerPanel.centered(g, ev.newRecord() ? "N E W   R E C O R D" : "G E N E R A T I O N   " + ev.generation(), 82);
         g.setFont(RunnerPanel.TEXT);
         g.setColor(RunnerPanel.MUTED);
         for (int i = 0; i < result.length; i++) {
            RunnerPanel.centered(g, result[i], 104 + i * 18);
         }
      }
   }

   /** The lead dinosaur's eyes, labeled, and under the ground what it is. */
   private static void paintLead(Graphics2D g, Evolution.Dino lead) {
      Brain b = lead.brain;
      g.setFont(RunnerPanel.TINY);
      eye(g, b.jumpX, b.jumpY, 8, JUMP_EYE);
      g.drawString("jump", b.jumpX - 10, b.jumpY - 6);
      if (b.ducks()) {
         eye(g, b.duckX, b.duckY, 8, DUCK_EYE);
         g.drawString("duck", b.duckX - 10, b.duckY - 6);
      }
      String who = b.encode().equals(ORIGINAL) ? "the original bot" : "the best so far";
      g.setColor(RunnerPanel.MUTED);
      g.drawString("▲ " + who, Runner.DINO_X - 4, Runner.GROUND + 24);
   }

   /** A square of size centered on the pixel an eye reads. */
   private static void eye(Graphics2D g, int x, int y, int size, Color color) {
      g.setColor(color);
      g.fillRect(x - size / 2, y - size / 2, size, size);
   }

   private static void paintBadge(Graphics2D g, Evolution ev, int speed) {
      String text = "EVOLVING   GEN " + ev.generation() + "   " + ev.running() + " OF " + Evolution.SIZE + " RUNNING"
         + (speed > 1 ? "   " + speed + "x" : "");
      g.setFont(RunnerPanel.TINY);
      int w = g.getFontMetrics().stringWidth(text) + 20;
      g.setColor(RunnerPanel.INK);
      g.fillRoundRect(12, 10, w, 22, 10, 10);
      g.setColor(Color.white);
      g.drawString(text, 22, 25);
   }

   /** Top right: the record in green, then how far the race has got. */
   private static void paintScore(Graphics2D g, Evolution ev, Evolution.Dino lead) {
      g.setFont(RunnerPanel.SCORE);
      FontMetrics fm = g.getFontMetrics();
      String now = RunnerPanel.pad(lead.score());
      int x = Runner.WIDTH - 14 - fm.stringWidth(now);
      g.setColor(RunnerPanel.INK);
      g.drawString(now, x, 24);
      String record = "REC " + RunnerPanel.pad(ev.championScore()) + "  ";
      g.setColor(RECORD);
      g.drawString(record, x - fm.stringWidth(record), 24);
   }

   /** Under the score: a bar for each generation's best, the record's bars green. */
   private static void paintChart(Graphics2D g, Evolution ev) {
      List<Integer> history = ev.history();
      if (history.isEmpty()) {
         return;
      }
      int max = 1;
      for (int n : history) {
         max = Math.max(max, n);
      }
      int x0 = Runner.WIDTH - 14 - CHART_W;
      int y0 = 34;
      int bars = Math.min(history.size(), CHART_W / 3);
      List<Integer> shown = history.subList(history.size() - bars, history.size());
      g.setColor(RunnerPanel.MUTED);
      g.setFont(RunnerPanel.TINY);
      String label = "best per generation";
      int left = x0 + CHART_W - shown.size() * 3;
      g.drawString(label, left - 6 - g.getFontMetrics().stringWidth(label), y0 + CHART_H - 2);
      for (int i = 0; i < shown.size(); i++) {
         int n = shown.get(i);
         int h = Math.max(1, n * CHART_H / max);
         g.setColor(n == max ? RECORD : BAR);
         g.fillRect(x0 + CHART_W - (shown.size() - i) * 3, y0 + CHART_H - h, 2, h);
      }
   }
}
