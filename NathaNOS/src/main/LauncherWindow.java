package main;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicInternalFrameUI;

/** The taskbar: the MENU button, a button for each open app window, and a clock. */
public class LauncherWindow extends JInternalFrame {
   static final Color BAR = new Color(18, 26, 42);
   static final Color HOVER = new Color(44, 58, 84);
   static final Color ON = new Color(56, 74, 106);
   private static final int START_WIDTH = 116;
   private static final int CLOCK_WIDTH = 96;
   private static final int MAX_TASK_WIDTH = 190;
   private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

   public JPanel panel = new JPanel();
   private final TaskList tasks;
   private final List<TaskList.Task> shown = new ArrayList<>();
   private final List<FlatButton> taskButtons = new ArrayList<>();
   private final TrayClock clock = new TrayClock();

   public LauncherWindow(Main screen, TaskList tasks) {
      super("Launcher", false, false, false);
      this.tasks = tasks;
      this.setBorder(null);
      this.setSize(screen.getWidth(), Math.max(36, screen.getHeight() / 20));
      this.setLocation(0, screen.getHeight() - this.getHeight());
      this.setVisible(true);
      this.setCursor(screen.getCursor());
      ((BasicInternalFrameUI)this.getUI()).setNorthPane(null);
      this.add(this.panel);
      this.panel.setBackground(BAR);
      this.panel.setLayout(null);

      FlatButton start = new FlatButton(
         new ImageIcon("assets/NathaNOS.png").getImage(), "NathaNOS", "MENU", new Color(0, 120, 215), new Color(0, 150, 255), null,
         () -> screen.startMenu.toggleVisibility()
      );
      start.setBounds(4, 0, START_WIDTH, this.getHeight());
      this.panel.add(start);
      this.clock.setBounds(this.getWidth() - CLOCK_WIDTH, 0, CLOCK_WIDTH, this.getHeight());
      this.panel.add(this.clock);

      Timer timer = new Timer(500, e -> this.refresh());
      timer.start();
   }

   /** Follows windows opening and closing, which window is active, and the time. */
   private void refresh() {
      List<TaskList.Task> showing = this.tasks.showing();
      if (!showing.equals(this.shown)) {
         this.shown.clear();
         this.shown.addAll(showing);
         this.layoutTasks();
      }
      for (int i = 0; i < this.shown.size(); i++) {
         this.taskButtons.get(i).setOn(this.shown.get(i).isActive());
      }
      this.clock.repaint();
   }

   private void layoutTasks() {
      for (FlatButton button : this.taskButtons) {
         this.panel.remove(button);
      }
      this.taskButtons.clear();
      int left = START_WIDTH + 12;
      int room = this.getWidth() - CLOCK_WIDTH - 8 - left;
      int width = this.shown.isEmpty() ? 0 : Math.min(MAX_TASK_WIDTH, room / this.shown.size());
      for (int i = 0; i < this.shown.size(); i++) {
         TaskList.Task task = this.shown.get(i);
         FlatButton button = new FlatButton(task.icon, task.name, task.name, null, HOVER, ON, task::activate);
         button.setBounds(left + i * width, 0, width, this.getHeight());
         this.taskButtons.add(button);
         this.panel.add(button);
      }
      this.panel.repaint();
   }

   /** The time and date at the right end of the taskbar. */
   private static final class TrayClock extends JComponent {
      @Override
      @SuppressWarnings("deprecation")
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         AppIcon.smooth(g);
         // Date's local-time getters, not Calendar: in the browser (TeaVM) Calendar's default time zone is UTC.
         Date now = new Date();
         int hour = now.getHours() % 12 == 0 ? 12 : now.getHours() % 12;
         int minute = now.getMinutes();
         String time = hour + ":" + (minute < 10 ? "0" : "") + minute + (now.getHours() < 12 ? " AM" : " PM");
         String date = MONTHS[now.getMonth()] + " " + now.getDate() + ", " + (1900 + now.getYear());
         boolean twoLines = this.getHeight() >= 34;
         g.setColor(FlatButton.TEXT);
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
         FontMetrics metrics = g.getFontMetrics();
         int timeY = twoLines ? this.getHeight() / 2 - 2 : (this.getHeight() + metrics.getAscent() - metrics.getDescent()) / 2;
         g.drawString(time, (this.getWidth() - metrics.stringWidth(time)) / 2, timeY);
         if (twoLines) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            metrics = g.getFontMetrics();
            g.drawString(date, (this.getWidth() - metrics.stringWidth(date)) / 2, this.getHeight() / 2 + metrics.getAscent());
         }
      }
   }
}
