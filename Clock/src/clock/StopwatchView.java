package clock;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/** The Stopwatch tab: the time to the hundredth, a ring sweeping the seconds, and laps with the fastest and slowest marked. */
final class StopwatchView implements Tab {
   private final Main clock;
   private Rectangle[] buttons;

   StopwatchView(Main clock) {
      this.clock = clock;
   }

   private Stopwatch stopwatch() {
      return this.clock.state.stopwatch;
   }

   @Override
   public String label(long now) {
      Stopwatch watch = this.stopwatch();
      return watch.isRunning() ? "Stopwatch " + Ui.minutesSeconds(watch.elapsed(now) / 1000) : "Stopwatch";
   }

   @Override
   public String hint() {
      return "Space: start/stop   L: lap   R: reset";
   }

   @Override
   public void paint(Graphics2D g, Rectangle area, long now) {
      Stopwatch watch = this.stopwatch();
      int gap = Math.max(4, area.height / 50);
      int buttonHeight = Math.max(24, Math.min(44, area.height / 9));
      Rectangle buttonRow = new Rectangle(area.x, area.y + area.height - buttonHeight, area.width, buttonHeight);
      int ringSize = Math.min(area.width, (area.height - buttonHeight - gap) * 11 / 20);
      Rectangle ring = new Rectangle(area.x + (area.width - ringSize) / 2, area.y, ringSize, ringSize);
      Rectangle list = new Rectangle(area.x, ring.y + ring.height + gap, area.width, buttonRow.y - gap - ring.y - ring.height - gap);

      long elapsed = watch.elapsed(now);
      this.paintRing(g, ring, elapsed);
      g.setColor(Ui.INK);
      int textWidth = ring.width * 7 / 10;
      Ui.line(g, Ui.stopwatch(elapsed), Ui.DIGITS,
            new Rectangle(ring.x + (ring.width - textWidth) / 2, ring.y + ring.height * 2 / 5, textWidth, ring.height / 5));
      g.setColor(Ui.FAINT);
      String status = watch.lapCount() > 0 || watch.isRunning()
            ? "Lap " + (watch.lapCount() + 1) + "  " + Ui.stopwatch(watch.currentLap(now)) : "Ready";
      Ui.line(g, status, Ui.LABELS, new Rectangle(ring.x + (ring.width - textWidth) / 2, ring.y + ring.height * 3 / 5 + 2, textWidth, ring.height / 10));

      this.paintLaps(g, list);

      this.buttons = Ui.row(buttonRow, 2, gap);
      Ui.button(g, this.buttons[0], watch.isRunning() ? "Stop" : elapsed > 0 ? "Resume" : "Start", true);
      Ui.button(g, this.buttons[1], watch.isRunning() ? "Lap" : "Reset", false);
   }

   /** A white dial whose red arc sweeps once a minute, with a dot at the second. */
   private void paintRing(Graphics2D g, Rectangle box, long elapsed) {
      int r = box.width / 2;
      int cx = box.x + r;
      int cy = box.y + r;
      int stroke = Math.max(2, r / 30);
      g.setColor(Ui.FACE);
      g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
      g.setColor(Ui.INK);
      for (int second = 0; second < 60; second++) {
         double angle = second * Math.PI / 30;
         double inner = second % 5 == 0 ? 0.86 : 0.91;
         g.setStroke(new BasicStroke(second % 5 == 0 ? stroke : Math.max(1, stroke / 2)));
         g.drawLine(cx + (int) (r * inner * Math.sin(angle)), cy - (int) (r * inner * Math.cos(angle)),
               cx + (int) (r * 0.97 * Math.sin(angle)), cy - (int) (r * 0.97 * Math.cos(angle)));
      }
      g.setStroke(new BasicStroke(stroke));
      g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
      double sweep = (elapsed % 60_000) / 60_000.0 * 2 * Math.PI;
      // Only the rim end of the hand, so the digits in the middle stay clear.
      double sin = Math.sin(sweep);
      double cos = Math.cos(sweep);
      g.setColor(Ui.HAND);
      g.setStroke(new BasicStroke(Math.max(2, r / 25)));
      g.drawLine(cx + (int) (r * 0.72 * sin), cy - (int) (r * 0.72 * cos), cx + (int) (r * 0.95 * sin), cy - (int) (r * 0.95 * cos));
      int dot = Math.max(3, r / 18);
      g.fillOval(cx + (int) (r * 0.72 * sin) - dot, cy - (int) (r * 0.72 * cos) - dot, 2 * dot, 2 * dot);
   }

   /** The laps, newest first, as many as fit: number, lap time and total, the fastest green and the slowest red. */
   private void paintLaps(Graphics2D g, Rectangle box) {
      Stopwatch watch = this.stopwatch();
      if (box.height < 12) {
         return;
      }
      int rowHeight = Math.max(14, Math.min(26, box.height / 4));
      Font font = new Font(Ui.LABELS, Font.BOLD, rowHeight * 2 / 3);
      if (watch.lapCount() == 0) {
         g.setColor(Ui.FAINT);
         Ui.center(g, watch.isRunning() ? "Press L or Lap to split" : "Laps show here", font, new Rectangle(box.x, box.y, box.width, rowHeight));
         return;
      }
      int fastest = watch.extremeLap(false);
      int slowest = watch.extremeLap(true);
      int rows = Math.max(1, box.height / rowHeight);
      int[] columns = {box.x + box.width / 20, box.x + box.width * 3 / 10, box.x + box.width * 3 / 5};
      for (int row = 0; row < rows && row < watch.lapCount(); row++) {
         int lap = watch.lapCount() - 1 - row;
         int y = box.y + row * rowHeight;
         if (row % 2 == 0) {
            g.setColor(Ui.FACE);
            g.fillRoundRect(box.x, y, box.width, rowHeight, rowHeight / 2, rowHeight / 2);
         }
         g.setColor(lap == fastest ? Ui.FASTEST : lap == slowest ? Ui.SLOWEST : Ui.INK);
         g.setFont(font);
         int baseline = y + (rowHeight + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
         String tag = lap == fastest ? "  fastest" : lap == slowest ? "  slowest" : "";
         g.drawString("Lap " + (lap + 1), columns[0], baseline);
         g.drawString(Ui.stopwatch(watch.lapTime(lap)) + tag, columns[1], baseline);
         g.drawString(Ui.stopwatch(watch.lapEnd(lap)), columns[2] + box.width / 6, baseline);
      }
   }

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      int button = Ui.indexOf(this.buttons, Ui.hit(this.buttons, x, y));
      if (button == 0) {
         this.stopwatch().toggle(now);
      } else if (button == 1) {
         this.lapOrReset(now);
      }
   }

   @Override
   public void drag(int x, int y, long now) {
   }

   @Override
   public void release(int x, int y, long now) {
   }

   @Override
   public boolean key(int keyCode, long now) {
      switch (keyCode) {
         case KeyEvent.VK_SPACE:
         case KeyEvent.VK_ENTER:
            this.stopwatch().toggle(now);
            return true;
         case KeyEvent.VK_L:
            this.stopwatch().lap(now);
            return true;
         case KeyEvent.VK_R:
            this.stopwatch().reset();
            return true;
         default:
            return false;
      }
   }

   private void lapOrReset(long now) {
      if (this.stopwatch().isRunning()) {
         this.stopwatch().lap(now);
      } else {
         this.stopwatch().reset();
      }
   }
}
