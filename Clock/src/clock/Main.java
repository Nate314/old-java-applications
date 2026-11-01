package clock;

import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class Main extends JFrame {
   JPanel clockPanel = new ClockPanel(this);
   public int month;
   public int dayOfMonth;
   public int dayOfWeek;
   public int year;
   public int hours;
   public int minutes;
   public int seconds;
   public int milliseconds;
   public boolean twentyFourHour = false;
   public String timeText = "";
   String[] dayNames = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

   /** Public and taking a Boolean so NathaNOS can launch it hidden and borrow its content pane. */
   public Main(Boolean visible) {
      this.setDefaultCloseOperation(3);
      this.setTitle("Clock");
      this.add(this.clockPanel);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().width / 4, Toolkit.getDefaultToolkit().getScreenSize().width / 4);
      ClockTicker ticker = new ClockTicker(this);
      ticker.actionPerformed(null);
      // Ticks several times a second so the second hand sweeps smoothly.
      Timer timer = new Timer(50, ticker);
      timer.setRepeats(true);
      timer.start();
      this.setVisible(visible);
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(() -> new Main(true));
   }
}
