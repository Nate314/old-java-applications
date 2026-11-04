package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Date;

/**
 * The Focus tab: a ring that drains as a focus session (red) or a break (green) runs, four dots for the sessions of
 * a set, and a week of bars for the minutes focused each day.
 */
final class FocusView implements Tab {
   static final Color BREAK = new Color(0, 150, 70);
   private static final String[] DAY_LETTERS = {"S", "M", "T", "W", "T", "F", "S"};
   private static final long DAY = 24 * 60 * Countdown.MINUTE;

   private final Main clock;
   private Rectangle[] buttons;

   FocusView(Main clock) {
      this.clock = clock;
   }

   private Focus focus() {
      return this.clock.state.focus;
   }

   @Override
   public String label(long now) {
      Focus focus = this.focus();
      if (focus.isRunning() || focus.isPaused()) {
         return (focus.onBreak() ? "Break " : "Focus ") + Ui.countdown(focus.remaining(now));
      }
      return "Focus";
   }

   @Override
   public String hint() {
      return "Space: start/pause   N: skip   R: reset   L: lengths";
   }

   @Override
   public void paint(Graphics2D g, Rectangle area, long now) {
      Focus focus = this.focus();
      int gap = Math.max(4, area.height / 50);
      int buttonHeight = clamp(area.height / 9, 24, 44);
      int chartHeight = clamp(area.height / 4, 50, 120);
      Rectangle buttonRow = new Rectangle(area.x, area.y + area.height - buttonHeight, area.width, buttonHeight);
      Rectangle chart = new Rectangle(area.x, buttonRow.y - gap - chartHeight, area.width, chartHeight);
      Rectangle ringBox = new Rectangle(area.x, area.y, area.width, chart.y - gap * 2 - area.y);

      this.paintRing(g, ringBox, now);
      this.paintWeek(g, chart, now);

      this.buttons = Ui.row(buttonRow, 3, gap);
      String go = focus.isRunning() ? "Pause" : focus.isPaused() ? "Resume" : focus.onBreak() ? "Start break" : "Start";
      Ui.button(g, this.buttons[0], go, true);
      Ui.button(g, this.buttons[1], focus.onBreak() ? "Skip break" : "Skip", false);
      int[] plan = Focus.PLANS[focus.plan()];
      Ui.button(g, this.buttons[2], plan[0] + " / " + plan[1] + " min", false);
   }

   private void paintRing(Graphics2D g, Rectangle box, long now) {
      Focus focus = this.focus();
      int r = Math.max(20, Math.min(box.width, box.height) / 2);
      int cx = box.x + box.width / 2;
      int cy = box.y + box.height / 2;
      int band = Math.max(6, r / 6);
      int stroke = Math.max(2, r / 40);
      Color color = focus.onBreak() ? BREAK : Ui.WEDGE;

      g.setColor(Ui.FACE);
      g.fillOval(cx - r, cy - r, 2 * r, 2 * r);

      // The band left of the phase, from 12 o'clock clockwise, draining back to 12 as it runs.
      long length = focus.length(focus.phase());
      double fraction = length <= 0 ? 0 : Math.min(1.0, focus.remaining(now) / (double) length);
      g.setColor(color);
      Ui.band(g, cx, cy, r - stroke, r - band, 0, fraction);
      g.setColor(Ui.INK);
      g.setStroke(new BasicStroke(stroke));
      g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
      int inner = r - band;
      g.setStroke(new BasicStroke(Math.max(1, stroke / 2)));
      g.drawOval(cx - inner, cy - inner, 2 * inner, 2 * inner);

      // Inside: what is running, the time left, and when it ends.
      int textWidth = inner * 3 / 2;
      String phase = focus.phase() == Focus.FOCUS ? "Focus " + (focus.done() + 1) + " of " + Focus.SET
            : focus.phase() == Focus.SHORT ? "Short break" : "Long break";
      g.setColor(color);
      Ui.line(g, phase, Ui.LABELS, new Rectangle(cx - textWidth / 2, cy - inner * 62 / 100, textWidth, inner * 18 / 100));
      g.setColor(Ui.INK);
      Ui.line(g, Ui.countdown(focus.remaining(now)), Ui.DIGITS,
            new Rectangle(cx - textWidth / 2, cy - inner * 38 / 100, textWidth, inner * 48 / 100));
      String status = focus.isRunning() ? "Ends at " + Ui.wallTime(now + focus.remaining(now), this.clock.state.twentyFour)
            : focus.isPaused() ? "Paused" : focus.onBreak() ? "Time for a break" : "Ready";
      g.setColor(Ui.FAINT);
      Ui.line(g, status, Ui.LABELS, new Rectangle(cx - textWidth / 2, cy + inner * 14 / 100, textWidth, inner * 15 / 100));

      // A dot per session of the set: filled once done, ringed for the one under way.
      int dot = Math.max(3, inner / 14);
      int spacing = dot * 3;
      int dotY = cy + inner * 50 / 100;
      for (int i = 0; i < Focus.SET; i++) {
         int x = cx + (2 * i - (Focus.SET - 1)) * spacing / 2;
         boolean filled = i < focus.done();
         g.setColor(filled ? Ui.WEDGE : Ui.FACE);
         g.fillOval(x - dot, dotY - dot, 2 * dot, 2 * dot);
         g.setColor(i == focus.done() && focus.phase() == Focus.FOCUS ? Ui.WEDGE : Ui.INK);
         g.setStroke(new BasicStroke(Math.max(1, dot / 3f)));
         g.drawOval(x - dot, dotY - dot, 2 * dot, 2 * dot);
      }
   }

   /** Today's total, and a bar per day for the last seven days, today on the right. */
   @SuppressWarnings("deprecation")
   private void paintWeek(Graphics2D g, Rectangle box, long now) {
      Focus focus = this.focus();
      int today = Focus.day(now);
      long noon = Focus.noon(now);
      int[] days = new int[7];
      int[] minutes = new int[7];
      int week = 0;
      int most = 60;
      for (int i = 0; i < 7; i++) {
         days[i] = Focus.day(noon - (6 - i) * DAY);
         minutes[i] = focus.minutesOn(days[i]);
         week += minutes[i];
         most = Math.max(most, minutes[i]);
      }

      int titleHeight = Math.max(11, box.height * 22 / 100);
      int sessions = focus.sessionsOn(today);
      String title = minutes[6] == 0 && sessions == 0 ? "Nothing focused yet today"
            : "Today " + amount(minutes[6]) + ", " + sessions + (sessions == 1 ? " session" : " sessions");
      if (week > minutes[6]) {
         title += "   7 days " + amount(week);
      }
      g.setColor(Ui.INK);
      Ui.line(g, title, Ui.LABELS, new Rectangle(box.x, box.y, box.width, titleHeight));

      int letterHeight = Math.max(10, box.height * 18 / 100);
      Rectangle bars = new Rectangle(box.x, box.y + titleHeight + 2, box.width, box.height - titleHeight - letterHeight - 4);
      Rectangle[] columns = Ui.row(new Rectangle(bars.x + bars.width / 10, bars.y, bars.width * 8 / 10, bars.height), 7, Math.max(3, bars.width / 60));
      g.setColor(Ui.FAINT);
      g.setStroke(new BasicStroke(1));
      g.drawLine(columns[0].x, bars.y + bars.height, columns[6].x + columns[6].width, bars.y + bars.height);
      Font letters = new Font(Ui.LABELS, Font.BOLD, letterHeight);
      for (int i = 0; i < 7; i++) {
         Rectangle column = columns[i];
         int height = (int) Math.round(bars.height * minutes[i] / (double) most);
         if (minutes[i] > 0) {
            g.setColor(i == 6 ? Ui.WEDGE : Ui.INK);
            g.fillRect(column.x, bars.y + bars.height - Math.max(2, height), column.width, Math.max(2, height));
         }
         g.setColor(i == 6 ? Ui.INK : Ui.FAINT);
         int weekday = new Date(noon - (6 - i) * DAY).getDay();
         Ui.center(g, DAY_LETTERS[weekday], letters,
               new Rectangle(column.x, bars.y + bars.height + 2, column.width, letterHeight));
      }
   }

   /** Minutes as "45 min" or "1 h 15 min". */
   static String amount(int minutes) {
      if (minutes < 60) {
         return minutes + " min";
      }
      return minutes / 60 + " h" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " min");
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      if (this.clock.chime.isRinging()) {
         this.clock.chime.stop();
      }
      Rectangle button = Ui.hit(this.buttons, x, y);
      int which = Ui.indexOf(this.buttons, button);
      if (which == 0) {
         this.focus().toggle(now);
      } else if (which == 1) {
         this.focus().skip(now);
      } else if (which == 2) {
         this.focus().nextPlan();
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
      this.clock.chime.stop();
      switch (keyCode) {
         case KeyEvent.VK_SPACE:
         case KeyEvent.VK_ENTER:
            this.focus().toggle(now);
            return true;
         case KeyEvent.VK_N:
            this.focus().skip(now);
            return true;
         case KeyEvent.VK_R:
            this.focus().reset();
            return true;
         case KeyEvent.VK_L:
            this.focus().nextPlan();
            return true;
         default:
            return false;
      }
   }
}
