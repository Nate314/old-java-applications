package clock;

import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
   public String timeText;
   String[] dayNames = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

   private Main(Boolean visible) {
      this.setDefaultCloseOperation(3);
      this.setVisible(visible);
      this.setTitle("Clock");
      Timer timer;
      (timer = new Timer(1000, new ClockTicker(this))).setRepeats(true);
      timer.start();
      this.add(this.clockPanel);
      this.setSize(Toolkit.getDefaultToolkit().getScreenSize().width / 4, Toolkit.getDefaultToolkit().getScreenSize().width / 4);
   }

   public static void main(String[] args) {
      new Main(true);
   }
}
