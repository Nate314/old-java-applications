package clock;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

/**
 * The original clock: an analog face with a red second hand, or the day, date and time as text. A button in the corner
 * turns on the Westminster quarters; each bell they strike sends a ripple across the analog face. The analog face has a
 * fourth, brass hand, as a wind-up alarm clock does: drag its knob to set the alarm, which the other corner's button
 * turns on and off.
 */
final class ClockFace implements Tab {
   static final Color BRASS = new Color(200, 140, 20);
   /** The alarm hand while the alarm is off. */
   private static final Color UNSET = new Color(205, 205, 205);
   private final Main clock;
   private final FlipFace flip = new FlipFace();
   private Rectangle chimesBox;
   private Rectangle alarmBox;
   /** The analog face's middle and the alarm knob, for dragging it; knobRadius is 0 on the digital face. */
   private int centerX;
   private int centerY;
   private int knobX;
   private int knobY;
   private int knobRadius;

   ClockFace(Main clock) {
      this.clock = clock;
   }

   @Override
   public String label(long now) {
      return "Clock";
   }

   @Override
   public String hint() {
      if (this.clock.state.alarm.isRinging()) {
         return "Space or click: snooze 9 min   A or Stop: stop until tomorrow";
      }
      ClockState state = this.clock.state;
      return (state.analog ? "Drag knob: alarm   Click: digital" : "Up/Down: alarm   Click: " + (state.flip ? "analog" : "flip clock"))
            + "   Right click: 12/24 h   B: chimes";
   }

   @Override
   public void paint(Graphics2D g2, Rectangle area, long now) {
      int margin = Math.min(area.width, area.height) / 50;
      int boxHeight = Math.max(20, Math.min(30, area.height / 12));
      this.chimesBox = new Rectangle(area.x, area.y, Math.min(area.width / 3, boxHeight * 4), boxHeight);
      this.alarmBox = new Rectangle(area.x + area.width - this.chimesBox.width, area.y, this.chimesBox.width, boxHeight);
      this.knobRadius = 0;
      Alarm alarm = this.clock.state.alarm;
      // Ringing, the clock shakes on its feet like a bell-topped alarm clock, unless the browser page asks for less motion.
      int shake = alarm.isRinging() && !Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion")) ? (int)Math.round(Math.sin(now / 18.0) * Math.max(2, margin * 0.8)) : 0;
      g2.translate(shake, 0);
      if (this.clock.state.analog) {
         g2.setStroke(new BasicStroke((float)margin));
         int diameter = Math.min(area.width, area.height) - (margin << 1);
         int radius = diameter / 2;
         int left = area.x + (area.width - diameter) / 2;
         int top = area.y + (area.height - diameter) / 2;
         int centerX = left + radius;
         int centerY = top + radius;
         double seconds = this.clock.seconds + this.clock.milliseconds / 1000.0;
         double minutes = this.clock.minutes + seconds / 60.0;
         double hours = this.clock.hours % 12 + minutes / 60.0;
         g2.setColor(Ui.FACE);
         g2.fillOval(left, top, diameter, diameter);
         // A focus session or break under way shows on the face: a band round the rim from now to when it ends.
         Focus focus = this.clock.state.focus;
         if (focus.isRunning()) {
            g2.setColor(focus.onBreak() ? FocusView.BREAK : Ui.WEDGE);
            Ui.band(g2, centerX, centerY, radius, radius * 88 / 100, minutes / 60.0, focus.remaining(now) / (60.0 * Countdown.MINUTE));
         }
         // The bell just struck: a brass ring running in from the rim and fading, further and bolder for the hour bell.
         Chime bells = this.clock.bells;
         long since = now - bells.struckAt;
         if (bells.isRinging() && since >= 0 && since < 1000) {
            float fade = 1 - since / 1000f;
            boolean hour = bells.struck == Westminster.HOUR_BELL;
            int ring = radius - margin * 2 - (int)(radius * (hour ? 0.6 : 0.3) * since / 1000);
            g2.setColor(new Color(mix(BRASS.getRed(), fade), mix(BRASS.getGreen(), fade), mix(BRASS.getBlue(), fade)));
            g2.setStroke(new BasicStroke(margin * (hour ? 2.5f : 1.2f)));
            g2.drawOval(centerX - ring, centerY - ring, ring * 2, ring * 2);
            g2.setStroke(new BasicStroke((float)margin));
         }
         g2.setColor(Ui.INK);
         g2.drawOval(left, top, diameter, diameter);
         paintDial(g2, centerX, centerY, radius, margin);
         this.paintAlarm(g2, alarm, centerX, centerY, radius, margin, now);
         g2.setColor(Ui.INK);
         drawHand(g2, centerX, centerY, (Math.PI / 2) - hours * (Math.PI / 6), radius * 0.5, radius * 0.06, Math.max(4, margin * 2.2));
         drawHand(g2, centerX, centerY, (Math.PI / 2) - minutes * (Math.PI / 30), radius * 0.8, radius * 0.06, Math.max(3, margin * 1.5));
         int cap = Math.max(3, margin * 3 / 2);
         g2.fillOval(centerX - cap, centerY - cap, cap * 2, cap * 2);
         // The second hand: thin, with a short tail behind the middle and a red cap over the others.
         g2.setColor(Ui.HAND);
         double angle = (Math.PI / 2) - seconds * (Math.PI / 30);
         g2.setStroke(new BasicStroke(Math.max(1.5f, margin * 0.45f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
         g2.drawLine((int)Math.round(centerX - radius * 0.2 * Math.cos(angle)), (int)Math.round(centerY + radius * 0.2 * Math.sin(angle)),
               (int)Math.round(centerX + radius * 0.9 * Math.cos(angle)), (int)Math.round(centerY - radius * 0.9 * Math.sin(angle)));
         g2.setStroke(new BasicStroke((float)margin));
         cap = Math.max(2, margin * 4 / 5);
         g2.fillOval(centerX - cap, centerY - cap, cap * 2, cap * 2);
      } else if (this.clock.state.flip) {
         this.flip.paint(g2, new Rectangle(area.x, area.y + boxHeight, area.width, area.height - boxHeight), this.clock,
               alarm.isOn() ? this.alarmWhen(alarm, now) : null, now);
      } else {
         area = new Rectangle(area.x, area.y + boxHeight, area.width, area.height - boxHeight); // clear of the buttons
         g2.setColor(Ui.INK);
         String[] lines = this.clock.timeText.split("\\n");
         Font font = new Font(Ui.DIGITS, Font.BOLD, 100);
         g2.setFont(font);

         // Shrink until the widest line fits across and all lines fit down.
         while (font.getSize() > 1 && (widestLine(g2, lines) > area.width - 20 || font.getSize() * lines.length > area.height - 10)) {
            font = new Font(Ui.DIGITS, Font.BOLD, font.getSize() - 1);
            g2.setFont(font);
         }

         int top = area.y + (area.height - font.getSize() * lines.length) / 2;
         for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            g2.drawString(lines[lineIndex], area.x + 10, top + font.getSize() + lineIndex * font.getSize());
         }
      }
      g2.translate(-shake, 0);
      Ui.button(g2, this.chimesBox, this.clock.state.chimes ? "Chimes on" : "Chimes off", this.clock.state.chimes);
      Ui.button(g2, this.alarmBox, alarm.isRinging() ? "Stop"
            : alarm.isOn() ? "Alarm " + Ui.wallTime(Alarm.next(alarm.minute(), now), this.clock.state.twentyFour) : "Alarm off",
            alarm.isOn());
   }

   /** The alarm hand with its knob (brass when set, grey when not) and, under the middle, when it rings. */
   private void paintAlarm(Graphics2D g2, Alarm alarm, int centerX, int centerY, int radius, int margin, long now) {
      double angle = (Math.PI / 2) - (alarm.minute() % 720) * (Math.PI / 360);
      int reach = radius * 62 / 100;
      this.centerX = centerX;
      this.centerY = centerY;
      this.knobX = (int)Math.round(centerX + reach * Math.cos(angle));
      this.knobY = (int)Math.round(centerY - reach * Math.sin(angle));
      this.knobRadius = Math.max(7, radius / 14);
      g2.setColor(alarm.isOn() ? BRASS : UNSET);
      g2.setStroke(new BasicStroke(Math.max(2f, margin * 0.6f)));
      g2.drawLine(centerX, centerY, this.knobX, this.knobY);
      g2.fillOval(this.knobX - this.knobRadius, this.knobY - this.knobRadius, this.knobRadius * 2, this.knobRadius * 2);
      g2.setColor(Ui.FACE);
      int dot = Math.max(2, this.knobRadius / 3);
      g2.fillOval(this.knobX - dot, this.knobY - dot, dot * 2, dot * 2);
      g2.setStroke(new BasicStroke((float)margin));
      if (alarm.isOn()) {
         g2.setColor(BRASS);
         // Under the middle, or over it while the knob is in the lower half, so the knob never covers it.
         int height = Math.max(10, radius / 10);
         int y = this.knobY > centerY ? centerY - radius * 45 / 100 - height : centerY + radius * 45 / 100;
         Ui.line(g2, this.alarmWhen(alarm, now), Ui.LABELS, new Rectangle(centerX - radius / 2, y, radius, height));
      }
   }

   /** When an alarm that is on rings, as said under the face. */
   private String alarmWhen(Alarm alarm, long now) {
      long snooze = alarm.snoozeUntil();
      return alarm.isRinging() ? "Wake up!"
            : snooze > now ? "Snoozing, rings at " + Ui.wallTime(snooze, this.clock.state.twentyFour)
            : "Rings in " + Alarm.wait(alarm.nextAt(now), now);
   }

   /** A brass channel {@code amount} of the way from the white face. */
   private static int mix(int brass, float amount) {
      return Math.round(255 + (brass - 255) * amount);
   }

   /**
    * The dial a clock face is read by: a tick for every minute, a longer, bolder one for every hour, and the hours'
    * numbers inside them, clear of the alarm knob's circle (62% out).
    */
   static void paintDial(Graphics2D g2, int centerX, int centerY, int radius, int margin) {
      g2.setColor(Ui.INK);
      BasicStroke minuteTick = new BasicStroke(Math.max(1f, margin * 0.3f));
      BasicStroke hourTick = new BasicStroke(Math.max(2f, margin * 0.9f));
      for (int minute = 0; minute < 60; minute++) {
         boolean hour = minute % 5 == 0;
         double angle = (Math.PI / 2) - minute * (Math.PI / 30);
         double inner = radius * (hour ? 0.84 : 0.89);
         double outer = radius * 0.94;
         g2.setStroke(hour ? hourTick : minuteTick);
         g2.drawLine((int)Math.round(centerX + inner * Math.cos(angle)), (int)Math.round(centerY - inner * Math.sin(angle)),
               (int)Math.round(centerX + outer * Math.cos(angle)), (int)Math.round(centerY - outer * Math.sin(angle)));
      }
      g2.setStroke(new BasicStroke((float)margin));
      int size = Math.max(8, radius * 12 / 100);
      Font font = new Font(Ui.DIGITS, Font.BOLD, size);
      for (int hour = 1; hour <= 12; hour++) {
         double angle = (Math.PI / 2) - hour * (Math.PI / 6);
         int x = (int)Math.round(centerX + radius * 0.76 * Math.cos(angle));
         int y = (int)Math.round(centerY - radius * 0.76 * Math.sin(angle));
         Ui.center(g2, String.valueOf(hour), font, new Rectangle(x - size, y - size / 2, size * 2, size));
      }
   }

   /** A hand as a filled baton, {@code width} wide at the middle and narrowing towards its tip, with a short tail. */
   private static void drawHand(Graphics2D g2, int centerX, int centerY, double angle, double length, double tail, double width) {
      double dx = Math.cos(angle);
      double dy = -Math.sin(angle);
      double[] along = {-tail, -tail, length, length};
      double[] across = {width / 2, -width / 2, -width / 5, width / 5};
      int[] xs = new int[4];
      int[] ys = new int[4];
      for (int i = 0; i < 4; i++) {
         xs[i] = (int)Math.round(centerX + along[i] * dx - across[i] * dy);
         ys[i] = (int)Math.round(centerY + along[i] * dy + across[i] * dx);
      }
      g2.fillPolygon(xs, ys, 4);
   }

   private static int widestLine(Graphics2D g2, String[] lines) {
      int widest = 0;
      for (String line : lines) {
         widest = Math.max(widest, g2.getFontMetrics().stringWidth(line));
      }
      return widest;
   }

   private boolean rightPressed;
   private boolean chimesPressed;
   private boolean alarmPressed;
   private boolean knobPressed;
   /** The press silenced a ringing alarm, so its release does nothing more. */
   private boolean hushed;

   @Override
   public void press(int x, int y, boolean rightButton, long now) {
      this.rightPressed = rightButton;
      this.chimesPressed = this.chimesBox != null && this.chimesBox.contains(x, y);
      this.alarmPressed = this.alarmBox != null && this.alarmBox.contains(x, y);
      this.hushed = this.clock.state.alarm.isRinging() && !this.chimesPressed;
      if (this.hushed) {
         this.clock.hushAlarm(!this.alarmPressed);
         return;
      }
      int dx = x - this.knobX;
      int dy = y - this.knobY;
      int grab = this.knobRadius * 2;
      this.knobPressed = !rightButton && !this.chimesPressed && !this.alarmPressed && this.knobRadius > 0
            && dx * dx + dy * dy <= grab * grab;
   }

   /** Dragging the knob turns the alarm hand, in five-minute steps, and sets the alarm. */
   @Override
   public void drag(int x, int y, long now) {
      if (this.knobPressed) {
         double turn = Math.atan2(x - this.centerX, this.centerY - y) / (2 * Math.PI);
         int dialMinute = (int)Math.round((turn < 0 ? turn + 1 : turn) * 720 / Alarm.STEP) * Alarm.STEP;
         this.clock.state.alarm.setFromDial(dialMinute, this.clock.hours * 60 + this.clock.minutes, now);
      }
   }

   /** As in the original, on release: left click switches the face (analog, digital, flip clock), right click 12 and 24 hours. */
   @Override
   public void release(int x, int y, long now) {
      if (this.hushed || this.knobPressed) {
         this.hushed = false;
         this.knobPressed = false;
      } else if (this.alarmPressed) {
         if (this.alarmBox.contains(x, y)) {
            this.clock.state.alarm.toggle(now);
         }
      } else if (this.chimesPressed) {
         if (this.chimesBox.contains(x, y)) {
            this.clock.toggleChimes();
         }
      } else if (this.rightPressed) {
         this.clock.state.twentyFour = !this.clock.state.twentyFour;
      } else {
         ClockState state = this.clock.state;
         state.flip = !state.analog && !state.flip;
         state.analog = !state.analog && !state.flip;
      }
   }

   @Override
   public boolean key(int keyCode, long now) {
      Alarm alarm = this.clock.state.alarm;
      boolean ringing = alarm.isRinging();
      if (keyCode == KeyEvent.VK_B) {
         this.clock.toggleChimes();
      } else if (ringing && (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER)) {
         this.clock.hushAlarm(true);
      } else if (ringing && (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_ESCAPE)) {
         this.clock.hushAlarm(false);
      } else if (keyCode == KeyEvent.VK_A) {
         alarm.toggle(now);
      } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
         alarm.set(alarm.minute() / Alarm.STEP * Alarm.STEP + (keyCode == KeyEvent.VK_UP ? Alarm.STEP : -Alarm.STEP), now);
      } else {
         return false;
      }
      return true;
   }
}
