package clock;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * The Timer tab: a 60-minute dial whose red wedge is the time left, set by dragging its knob (or a preset), shrinking
 * back to 12 as it counts down, and a bell when it runs out.
 */
final class TimerView implements Tab {
   private static final int[] PRESETS = {1, 3, 5, 10, 25, 60};

   private final Main clock;
   private Rectangle[] presets;
   private Rectangle[] buttons;
   private int centerX;
   private int centerY;
   private int radius;
   private boolean dragging;
   private int dragMinutes;

   TimerView(Main clock) {
      this.clock = clock;
   }

   private Countdown countdown() {
      return this.clock.state.countdown;
   }

   @Override
   public String label(long now) {
      Countdown countdown = this.countdown();
      if (countdown.isDone()) {
         return "Time's up";
      }
      return countdown.isRunning() || countdown.isPaused() ? "Timer " + Ui.countdown(countdown.remaining(now)) : "Timer";
   }

   @Override
   public String hint() {
      return "Drag the dial   Space: start/pause   R: reset   Up/Down: +-1 min";
   }

   @Override
   public void paint(Graphics2D g, Rectangle area, long now) {
      Countdown countdown = this.countdown();
      int gap = Math.max(4, area.height / 50);
      int buttonHeight = clamp(area.height / 9, 24, 44);
      int presetHeight = clamp(area.height / 12, 20, 34);
      int readoutHeight = clamp(area.height / 6, 30, 90);
      Rectangle buttonRow = new Rectangle(area.x, area.y + area.height - buttonHeight, area.width, buttonHeight);
      Rectangle presetRow = new Rectangle(area.x, buttonRow.y - gap - presetHeight, area.width, presetHeight);
      Rectangle readout = new Rectangle(area.x, presetRow.y - gap - readoutHeight, area.width, readoutHeight);
      Rectangle dialBox = new Rectangle(area.x, area.y, area.width, readout.y - gap - area.y);

      if (countdown.isDone() && (!this.clock.chime.isRinging() || (now / 500) % 2 == 0)) {
         g.setColor(Ui.ALARM);
         g.fillRect(area.x - gap, area.y - gap, area.width + 2 * gap, area.height + 2 * gap);
      }

      this.paintDial(g, dialBox, countdown.remaining(now));

      // The time left, and below it what the timer is doing.
      long left = countdown.remaining(now);
      String big;
      String small;
      if (countdown.isDone()) {
         big = "Time's up!";
         small = "Rang at " + Ui.wallTime(countdown.finishedAt(), this.clock.state.twentyFour);
      } else if (countdown.isRunning()) {
         big = Ui.countdown(left);
         small = "Ends at " + Ui.wallTime(now + left, this.clock.state.twentyFour);
      } else if (countdown.length() == 0) {
         big = "0:00";
         small = "Drag the dial or pick a time";
      } else {
         big = Ui.countdown(left);
         small = countdown.isPaused() ? "Paused" : "Ready";
      }
      g.setColor(countdown.isDone() ? Ui.SLOWEST : Ui.INK);
      Ui.line(g, big, Ui.DIGITS, new Rectangle(readout.x, readout.y, readout.width, readout.height * 7 / 10));
      g.setColor(Ui.FAINT);
      Ui.line(g, small, Ui.LABELS, new Rectangle(readout.x, readout.y + readout.height * 7 / 10, readout.width, readout.height * 3 / 10));

      this.presets = Ui.row(presetRow, PRESETS.length, gap);
      for (int i = 0; i < PRESETS.length; i++) {
         boolean chosen = countdown.length() == PRESETS[i] * Countdown.MINUTE;
         Ui.button(g, this.presets[i], PRESETS[i] + " min", chosen);
      }

      this.buttons = Ui.row(buttonRow, 2, gap);
      String go = countdown.isDone() ? "Stop" : countdown.isRunning() ? "Pause" : countdown.isPaused() ? "Resume" : "Start";
      Ui.button(g, this.buttons[0], go, countdown.isDone() || countdown.remaining(now) > 0);
      Ui.button(g, this.buttons[1], countdown.isDone() ? "Again" : "Reset", false);
   }

   private void paintDial(Graphics2D g, Rectangle box, long left) {
      int diameter = Math.max(20, Math.min(box.width, box.height));
      this.radius = diameter / 2;
      this.centerX = box.x + box.width / 2;
      this.centerY = box.y + box.height / 2;
      int r = this.radius;
      int stroke = Math.max(2, r / 30);
      g.setColor(Ui.FACE);
      g.fillOval(this.centerX - r, this.centerY - r, 2 * r, 2 * r);

      // The wedge: from 12 o'clock clockwise, one degree per ten seconds left.
      double fraction = Math.min(1.0, left / (double) Countdown.MAX);
      int wedge = (int) (r * 0.84);
      if (fraction > 0) {
         int steps = Math.max(2, (int) Math.ceil(fraction * 180));
         int[] xs = new int[steps + 2];
         int[] ys = new int[steps + 2];
         xs[0] = this.centerX;
         ys[0] = this.centerY;
         for (int i = 0; i <= steps; i++) {
            double angle = fraction * 2 * Math.PI * i / steps;
            xs[i + 1] = this.centerX + (int) Math.round(wedge * Math.sin(angle));
            ys[i + 1] = this.centerY - (int) Math.round(wedge * Math.cos(angle));
         }
         g.setColor(Ui.WEDGE);
         g.fillPolygon(xs, ys, steps + 2);
      }

      // Minute ticks round the rim, longer every five, and the minutes every five (white over the wedge).
      g.setColor(Ui.INK);
      for (int minute = 0; minute < 60; minute++) {
         double angle = minute * Math.PI / 30;
         double inner = minute % 5 == 0 ? 0.87 : 0.92;
         g.setStroke(new BasicStroke(minute % 5 == 0 ? stroke : Math.max(1, stroke / 2)));
         g.drawLine(this.centerX + (int) (r * inner * Math.sin(angle)), this.centerY - (int) (r * inner * Math.cos(angle)),
               this.centerX + (int) (r * 0.97 * Math.sin(angle)), this.centerY - (int) (r * 0.97 * Math.cos(angle)));
      }
      int numberSize = Math.max(8, r / 8);
      Font numbers = new Font(Ui.LABELS, Font.BOLD, numberSize);
      boolean everyFive = r >= 70;
      for (int minute = 0; minute < 60; minute += everyFive ? 5 : 15) {
         double angle = minute * Math.PI / 30;
         int x = this.centerX + (int) (r * 0.70 * Math.sin(angle));
         int y = this.centerY - (int) (r * 0.70 * Math.cos(angle));
         g.setColor(minute > 0 && minute < fraction * 60 ? Ui.FACE : Ui.INK);
         Ui.center(g, String.valueOf(minute), numbers, new Rectangle(x - numberSize, y - numberSize / 2, 2 * numberSize, numberSize));
      }
      g.setColor(Ui.INK);
      g.setStroke(new BasicStroke(stroke));
      g.drawOval(this.centerX - r, this.centerY - r, 2 * r, 2 * r);

      // The knob at the wedge's edge, to drag, and the hub.
      double edge = fraction * 2 * Math.PI;
      int knob = Math.max(5, r / 11);
      int knobX = this.centerX + (int) Math.round(wedge * Math.sin(edge));
      int knobY = this.centerY - (int) Math.round(wedge * Math.cos(edge));
      g.setStroke(new BasicStroke(Math.max(2, r / 25)));
      g.drawLine(this.centerX, this.centerY, knobX, knobY);
      g.fillOval(knobX - knob, knobY - knob, 2 * knob, 2 * knob);
      g.setColor(Ui.FACE);
      g.fillOval(knobX - knob / 2, knobY - knob / 2, knob, knob);
      g.setColor(Ui.INK);
      int hub = Math.max(4, r / 14);
      g.fillOval(this.centerX - hub, this.centerY - hub, 2 * hub, 2 * hub);
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   /** The whole minute the point aims at on the dial, 0 to 59. */
   private int minuteAt(int x, int y) {
      double angle = Math.atan2(x - this.centerX, this.centerY - y);
      if (angle < 0) {
         angle += 2 * Math.PI;
      }
      return (int) Math.round(angle / (2 * Math.PI) * 60) % 60;
   }

   private boolean onDial(int x, int y) {
      double dx = x - this.centerX;
      double dy = y - this.centerY;
      return this.radius > 0 && dx * dx + dy * dy <= this.radius * this.radius * 1.1;
   }

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      Countdown countdown = this.countdown();
      Rectangle preset = Ui.hit(this.presets, x, y);
      Rectangle button = Ui.hit(this.buttons, x, y);
      if (countdown.isDone() && button == null) {
         this.stopAlarm();
      } else if (preset != null) {
         countdown.set(PRESETS[Ui.indexOf(this.presets, preset)] * Countdown.MINUTE, now);
      } else if (button != null) {
         if (Ui.indexOf(this.buttons, button) == 0) {
            this.startOrPause(now);
         } else {
            this.reset(now);
         }
      } else if (this.onDial(x, y)) {
         this.dragging = true;
         this.dragMinutes = (int) ((countdown.remaining(now) + Countdown.MINUTE / 2) / Countdown.MINUTE);
         this.drag(x, y, now);
      }
   }

   /** Turning the knob: snaps to whole minutes, and stops at 0 and at 60 rather than wrapping round past 12. */
   @Override
   public void drag(int x, int y, long now) {
      if (!this.dragging) {
         return;
      }
      int minutes = this.minuteAt(x, y);
      if (this.dragMinutes >= 45 && minutes < 15) {
         minutes = 60;
      } else if (this.dragMinutes <= 15 && minutes > 45) {
         minutes = 0;
      }
      this.dragMinutes = minutes;
      this.countdown().set(minutes * Countdown.MINUTE, now);
   }

   @Override
   public void release(int x, int y, long now) {
      this.dragging = false;
   }

   @Override
   public boolean key(int keyCode, long now) {
      Countdown countdown = this.countdown();
      if (countdown.isDone() && keyCode != KeyEvent.VK_R) {
         this.stopAlarm();
         return true;
      }
      switch (keyCode) {
         case KeyEvent.VK_SPACE:
         case KeyEvent.VK_ENTER:
            this.startOrPause(now);
            return true;
         case KeyEvent.VK_R:
            this.reset(now);
            return true;
         case KeyEvent.VK_UP:
            countdown.addMinutes(1, now);
            return true;
         case KeyEvent.VK_DOWN:
            countdown.addMinutes(-1, now);
            return true;
         default:
            return false;
      }
   }

   private void startOrPause(long now) {
      if (this.countdown().isDone()) {
         this.stopAlarm();
      } else {
         this.countdown().toggle(now);
      }
   }

   /** Reset, or once done Again: the same length, started again. */
   private void reset(long now) {
      boolean again = this.countdown().isDone();
      this.clock.chime.stop();
      this.countdown().reset();
      if (again) {
         this.countdown().toggle(now);
      }
   }

   private void stopAlarm() {
      this.clock.chime.stop();
      this.countdown().dismiss();
   }
}
