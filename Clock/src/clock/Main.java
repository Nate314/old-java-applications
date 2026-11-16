package clock;

import java.awt.Toolkit;
import java.io.File;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main extends JFrame {
   static final File STATE_FILE = new File(System.getProperty("user.home", "."), ".clock.txt");

   ClockState state;
   final Chime chime = new Chime();
   /** The quarter chimes, on a bell of their own so the timer's alarm can cut them short. */
   final Chime bells = new Chime();
   ClockPanel clockPanel = new ClockPanel(this);
   public int month;
   public int dayOfMonth;
   public int dayOfWeek;
   public int year;
   public int hours;
   public int minutes;
   public int seconds;
   public int milliseconds;
   public String timeText = "";
   String[] dayNames = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

   /** Public and taking a Boolean so NathaNOS can launch it hidden and borrow its content pane. */
   public Main(Boolean visible) {
      // Read after the frame exists: in the browser, saved files are restored when the first component is made.
      this.state = ClockState.load(STATE_FILE);
      this.setDefaultCloseOperation(3);
      this.setTitle("Clock");
      this.add(this.clockPanel);
      this.addKeyListener(this.clockPanel.keys());
      this.clockPanel.addKeyListener(this.clockPanel.keys());
      // Square, a quarter of the screen wide as before, but at least big enough for the timer's buttons.
      java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      int side = Math.min(Math.max(screen.width / 4, 380), Math.min(screen.width, screen.height));
      this.setSize(side, side);
      ClockTicker ticker = new ClockTicker(this);
      ticker.actionPerformed(null);
      // Ticks several times a second so the second hand sweeps smoothly.
      Timer timer = new Timer(50, ticker);
      timer.setRepeats(true);
      timer.start();
      this.setVisible(visible);
   }

   /** The title says what the timer is doing, so it shows on the taskbar (and in the browser tab). */
   void refresh() {
      long now = System.currentTimeMillis();
      Countdown countdown = this.state.countdown;
      Focus focus = this.state.focus;
      Alarm alarm = this.state.alarm;
      String title = alarm.isRinging() ? "Wake up! - Clock"
            : countdown.isDone() ? "Time's up! - Clock"
            : countdown.isRunning() ? Ui.countdown(countdown.remaining(now)) + " left - Clock"
            : focus.isRunning() ? (focus.onBreak() ? "Break " : "Focus ") + Ui.countdown(focus.remaining(now)) + " - Clock"
            : alarm.snoozeUntil() > now ? "Snoozing " + Ui.countdown(alarm.snoozeUntil() - now) + " - Clock"
            : "Clock";
      if (!title.equals(this.getTitle())) {
         this.setTitle(title);
      }
      this.clockPanel.repaint();
   }

   /** The timer ran out: ring, and show the Timer tab. */
   void timerFinished() {
      this.bells.stop();
      this.chime.ring();
      this.clockPanel.show(ClockState.TIMER);
      this.state.save();
   }

   /** The alarm (or a snooze) came due: ring for about a minute, and show the clock. */
   void alarmRang() {
      this.bells.stop();
      this.chime.ring(Chime.WAKE_UP, Chime.WAKE_REPEATS);
      this.clockPanel.show(ClockState.CLOCK);
      this.state.save();
   }

   /** Snooze for nine minutes, or (when {@code snooze} is false) stop until tomorrow. */
   void hushAlarm(boolean snooze) {
      this.chime.stop();
      if (snooze) {
         this.state.alarm.snooze(System.currentTimeMillis());
      } else {
         this.state.alarm.stop();
      }
   }

   /**
    * A focus session or a break ended. When that just happened (not long ago while the window was closed), ring and
    * show the Focus tab.
    */
   void focusFinished(int event, long now) {
      if (now - this.state.focus.eventAt() < Countdown.MINUTE) {
         this.bells.stop();
         this.chime.ring(event == Focus.FOCUS_DONE ? Chime.BREAK_TIME : Chime.BACK_TO_WORK, 2);
         this.clockPanel.show(ClockState.FOCUS);
      }
      this.state.save();
   }

   /** A quarter hour has come round: ring it, unless the chimes are off or the timer or a focus bell is ringing. */
   void quarterStruck(int hours, int quarter) {
      if (this.state.chimes && !this.chime.isRinging()) {
         this.bells.toll(Westminster.steps(hours, quarter), Westminster.STEP_MILLIS);
      }
   }

   /** Turns the chimes on, ringing the last quarter so it is heard at once, or off, silencing them. */
   void toggleChimes() {
      this.state.chimes = !this.state.chimes;
      this.bells.stop();
      this.quarterStruck(this.hours, this.minutes / 15);
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
