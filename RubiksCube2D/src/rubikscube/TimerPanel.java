package rubikscube;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * The Timer card, in place of the message and help: the scramble to make on a real cube, the time (red while held,
 * green once letting go will start it), what to do next, the best time and averages, and the last few times. The
 * time is a pad too: press and hold it, let go to start, press again to stop, for touch screens.
 */
final class TimerPanel extends ClickCard {
   /** What a click asks for. */
   static final int NEW = -1;
   static final int DROP = -2;
   static final int PAD = -3;
   static final int LET_GO = -4;
   private static final Color HELD = new Color(200, 50, 50);
   private static final Color GO = new Color(30, 150, 70);
   private String scramble = "";
   private SolveTimer timer = new SolveTimer();
   private List<Long> times = new ArrayList<>();

   TimerPanel(IntConsumer onClick) {
      super(onClick);
      // Takes the focus when the card opens, so Space is the timer's and no button's.
      this.setFocusable(true);
      this.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseReleased(MouseEvent event) {
            onClick.accept(LET_GO);
         }
      });
   }

   /** The card for this scramble, the timer as it is now and the times kept (oldest first). */
   void show(String scramble, SolveTimer timer, List<Long> times) {
      this.scramble = scramble;
      this.timer = timer;
      this.times = times;
      this.repaint();
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      long now = System.currentTimeMillis();
      int width = this.getWidth();
      int height = this.getHeight();
      CardPaint.card(g, width, height);
      this.clearTargets();
      int x = 9;
      int inner = width - 2 * x;
      SolveTimer.State state = this.timer.state();
      g.setFont(new Font("Dialog", Font.BOLD, 10));
      g.setColor(CardPaint.MUTED);
      g.drawString("TIMER", x, 16);
      String count = this.times.size() + (this.times.size() == 1 ? " solve" : " solves");
      g.drawString(count, width - x - g.getFontMetrics().stringWidth(count), 16);
      g.setFont(new Font("Dialog", Font.BOLD, 12));
      g.setColor(CardPaint.INK);
      int y = CardPaint.wrap(g, "Scramble your cube, blue on top and red in front:", x, 34, inner, 15, 2);
      g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
      g.setColor(CardPaint.NOW);
      y = CardPaint.wrap(g, this.scramble, x, y + 17, inner, 16, 4);
      // The time, which is also the pad to hold.
      int padTop = y + 8;
      int padHeight = 48;
      boolean ready = this.timer.ready(now);
      g.setColor(state == SolveTimer.State.HOLDING ? CubeView3D.mix(ready ? GO : HELD, CardPaint.CARD, 0.85)
            : this.hover == PAD ? new Color(232, 238, 250) : Color.WHITE);
      g.fillRoundRect(x, padTop, inner, padHeight, 10, 10);
      g.setColor(CardPaint.EDGE);
      g.drawRoundRect(x, padTop, inner - 1, padHeight - 1, 10, 10);
      long shown = state == SolveTimer.State.HOLDING ? 0L : this.timer.shown(now);
      String time = shown < 0 ? "0.00" : SolveTimer.format(shown);
      g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
      g.setColor(state == SolveTimer.State.HOLDING ? (ready ? GO : HELD) : CardPaint.INK);
      g.drawString(time, x + (inner - g.getFontMetrics().stringWidth(time)) / 2, padTop + 35);
      this.target(x, padTop, inner, padHeight, PAD);
      long best = SolveTimer.best(this.times);
      String say;
      if (state == SolveTimer.State.RUNNING) {
         say = "Solve it! Space (any key, or a tap here) stops.";
      } else if (state == SolveTimer.State.HOLDING) {
         say = ready ? "Let go to start." : "Keep holding until it turns green...";
      } else if (this.timer.last() >= 0 && this.timer.last() == best && this.times.size() > 1) {
         say = "Your best yet! The next scramble is up.";
      } else if (this.timer.last() >= 0) {
         say = "The next scramble is up.";
      } else {
         say = "Then hold Space (or press here), let go to start, press again to stop. No cube? Use the one on screen.";
      }
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      g.setColor(CardPaint.MUTED);
      y = CardPaint.wrap(g, say, x, padTop + padHeight + 14, inner, 13, 4);
      String stats = best < 0 ? "No times yet." : "Best " + SolveTimer.format(best)
            + words("  Ao5 ", SolveTimer.average(this.times, 5)) + words("  Ao12 ", SolveTimer.average(this.times, 12));
      g.setFont(new Font("Dialog", Font.BOLD, 11));
      g.setColor(CardPaint.INK);
      y = CardPaint.wrap(g, stats, x, y + 17, inner, 13, 2);
      StringBuilder recent = new StringBuilder();
      for (int i = this.times.size() - 1; i >= 0 && i >= this.times.size() - 5; i--) {
         recent.append(recent.length() == 0 ? "Last: " : ", ").append(SolveTimer.format(this.times.get(i)));
      }
      g.setFont(new Font("Dialog", Font.PLAIN, 11));
      g.setColor(CardPaint.MUTED);
      int buttonY = height - 34;
      CardPaint.wrap(g, recent.toString(), x, y + 14, inner, 13, Math.max(1, (buttonY - y - 4) / 13));
      int half = (inner - 6) / 2;
      this.button(g, x, buttonY, half, "New scramble", NEW, false);
      this.button(g, x + half + 6, buttonY, half, "Delete last", DROP, false);
      if (state == SolveTimer.State.RUNNING) {
         // Then a tap anywhere on the card stops it.
         this.target(0, 0, width, height, PAD);
      }
   }

   private static String words(String label, long millis) {
      return millis < 0 ? "" : label + SolveTimer.format(millis);
   }
}
