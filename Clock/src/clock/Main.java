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
      String title = countdown.isDone() ? "Time's up! - Clock"
            : countdown.isRunning() ? Ui.countdown(countdown.remaining(now)) + " left - Clock"
            : "Clock";
      if (!title.equals(this.getTitle())) {
         this.setTitle(title);
      }
      this.clockPanel.repaint();
   }

   /** The timer ran out: ring, and show the Timer tab. */
   void timerFinished() {
      this.chime.ring();
      this.clockPanel.show(ClockState.TIMER);
      this.state.save();
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
