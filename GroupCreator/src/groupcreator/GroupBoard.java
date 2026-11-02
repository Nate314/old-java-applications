package groupcreator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The board under the inputs. In Groups it deals the newest round onto one card per group, marks the people who were
 * together before, and swaps two names of different groups when they are clicked one after the other. Partner chart
 * shows, for every pair in the class, how many rounds put them in one group. Its buttons run along the bottom.
 */
@SuppressWarnings("serial")
public class GroupBoard extends JPanel {
   static final int WIDTH = 760;
   static final int HEIGHT = 440;
   private static final int HEADER = 50;
   private static final int BAR = 50;
   private static final int GAP = 12;
   private static final int FLY_MS = 260;

   static final Color DESK = new Color(236, 230, 218);
   static final Color INK = new Color(40, 44, 52);
   static final Color MUTED = new Color(110, 112, 120);
   static final Color FRESH = new Color(38, 128, 78);
   static final Color AGAIN = new Color(214, 132, 20);
   static final Color AGAIN_FILL = new Color(255, 236, 200);
   static final Color PICKED = new Color(255, 222, 102);
   static final Color ERROR = new Color(190, 52, 44);
   private static final Color[] CARD_COLORS = {
      new Color(66, 120, 212), new Color(220, 92, 70), new Color(46, 150, 100), new Color(150, 92, 200),
      new Color(230, 160, 30), new Color(40, 150, 170), new Color(210, 80, 140), new Color(110, 120, 60)
   };

   private static final Font TITLE = new Font("SansSerif", Font.BOLD, 15);
   private static final Font TEXT = new Font("SansSerif", Font.PLAIN, 13);
   private static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
   private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);

   /** What the board's buttons and edits ask the window to do. */
   public interface Actions {
      void newRound();

      void reshuffle();

      void clearHistory();

      /** The newest round was changed by hand (two names swapped). */
      void edited(List<List<String>> groups);
   }

   private final class Button {
      final char key;
      final Runnable action;
      final Rectangle bounds = new Rectangle();
      String label;
      boolean enabled = true;

      Button(String label, char key, Runnable action) {
         this.label = label;
         this.key = key;
         this.action = action;
      }
   }

   private final PartnerHistory history;
   private final Actions actions;
   private final List<Button> buttons = new ArrayList<>();
   private final Button reshuffle;
   private final Button chartButton;
   private final Button clear;

   private List<List<String>> groups = Collections.emptyList();
   /** The rounds before the one shown, for marking who meets again. */
   private PartnerHistory before = new PartnerHistory();
   private List<String> names = Collections.emptyList();
   private String message;
   private boolean chart;
   private boolean clearArmed;
   private long dealStart;
   private int dealStep = 45;
   private final Timer dealTimer;

   private Rectangle[][] chips = new Rectangle[0][];
   private int pickedGroup = -1;
   private int pickedIndex = -1;
   private int hoverGroup = -1;
   private int hoverIndex = -1;
   private int mouseX = -1;
   private int mouseY = -1;

   public GroupBoard(PartnerHistory history, Actions actions) {
      this.history = history;
      this.actions = actions;
      setPreferredSize(new Dimension(WIDTH, HEIGHT));
      setFocusable(true);
      buttons.add(new Button("New round", 'n', actions::newRound));
      reshuffle = new Button("Reshuffle", 'r', actions::reshuffle);
      buttons.add(reshuffle);
      chartButton = new Button("Partner chart", 'c', () -> {
         chart = !chart;
         unpick();
      });
      buttons.add(chartButton);
      clear = new Button("Clear history", 'x', () -> {
         if (clearArmed) {
            clearArmed = false;
            actions.clearHistory();
         } else {
            clearArmed = true;
         }
      });
      buttons.add(clear);
      dealTimer = new Timer(16, event -> {
         if (!dealing()) {
            ((Timer) event.getSource()).stop();
         }
         repaint();
      });

      MouseAdapter mouse = new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent event) {
            requestFocusInWindow();
            press(event.getX(), event.getY());
            repaint();
         }

         @Override
         public void mouseMoved(MouseEvent event) {
            mouseX = event.getX();
            mouseY = event.getY();
            repaint();
         }

         @Override
         public void mouseExited(MouseEvent event) {
            mouseX = -1;
            mouseY = -1;
            repaint();
         }
      };
      addMouseListener(mouse);
      addMouseMotionListener(mouse);
      addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent event) {
            key(event);
            repaint();
         }
      });
   }

   /** Shows the newest round of the history, dealt card by card when {@code deal} is set. */
   public void showRound(List<String> roster, boolean deal) {
      names = new ArrayList<>(roster);
      groups = history.last();
      before = history.first(history.size() - 1);
      message = null;
      clearArmed = false;
      unpick();
      if (deal && !groups.isEmpty()) {
         int count = 0;
         for (List<String> group : groups) {
            count += group.size();
         }
         // A big class is dealt faster, so the whole deal takes about two seconds at most.
         dealStep = Math.max(8, Math.min(45, 1700 / Math.max(1, count)));
         dealStart = System.currentTimeMillis();
         chart = false;
         dealTimer.start();
      } else {
         dealStart = 0;
      }
      repaint();
   }

   /** Shows a message (why groups could not be made) in place of the round's summary. */
   public void showMessage(String text) {
      message = text;
      repaint();
   }

   private boolean dealing() {
      return dealStart > 0 && System.currentTimeMillis() - dealStart < (long) (dealOrder().size() - 1) * dealStep + FLY_MS;
   }

   private void unpick() {
      pickedGroup = -1;
      pickedIndex = -1;
   }

   private void press(int x, int y) {
      if (y >= getHeight() - BAR) {
         for (Button button : buttons) {
            if (button.enabled && button.bounds.contains(x, y)) {
               if (button != clear) {
                  clearArmed = false;
               }
               button.action.run();
               return;
            }
         }
         return;
      }
      clearArmed = false;
      message = null;
      if (dealing()) {
         dealStart = 1;   // finish the deal
         return;
      }
      if (chart) {
         return;
      }
      for (int g = 0; g < chips.length; g++) {
         for (int k = 0; k < chips[g].length; k++) {
            if (chips[g][k] != null && chips[g][k].contains(x, y)) {
               pick(g, k);
               return;
            }
         }
      }
      unpick();
   }

   /** The first click picks a name; a click on a name of another group swaps the two. */
   private void pick(int g, int k) {
      if (pickedGroup < 0 || pickedGroup == g) {
         boolean same = pickedGroup == g && pickedIndex == k;
         pickedGroup = same ? -1 : g;
         pickedIndex = same ? -1 : k;
         return;
      }
      List<List<String>> swapped = PartnerHistory.copy(groups);
      String a = swapped.get(pickedGroup).get(pickedIndex);
      swapped.get(pickedGroup).set(pickedIndex, swapped.get(g).get(k));
      swapped.get(g).set(k, a);
      unpick();
      actions.edited(swapped);
   }

   private void key(KeyEvent event) {
      if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
         unpick();
         clearArmed = false;
         return;
      }
      char key = Character.toLowerCase(event.getKeyChar());
      if (key == ' ') {
         key = 'n';
      }
      for (Button button : buttons) {
         if (button.key == key && button.enabled) {
            if (button != clear) {
               clearArmed = false;
            }
            button.action.run();
            return;
         }
      }
   }

   /** Group and place of each name, in the order they are dealt: the first of every group, then the second... */
   private List<int[]> dealOrder() {
      List<int[]> order = new ArrayList<>();
      int longest = 0;
      for (List<String> group : groups) {
         longest = Math.max(longest, group.size());
      }
      for (int k = 0; k < longest; k++) {
         for (int g = 0; g < groups.size(); g++) {
            if (k < groups.get(g).size()) {
               order.add(new int[] {g, k});
            }
         }
      }
      return order;
   }

   @Override
   protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D) graphics;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int w = getWidth();
      int h = getHeight();
      g.setColor(DESK);
      g.fillRect(0, 0, w, h);
      reshuffle.enabled = !history.isEmpty();
      clear.enabled = !history.isEmpty();
      chartButton.label = chart ? "Groups" : "Partner chart";
      clear.label = clearArmed ? "Sure? Press again" : "Clear history";
      String[] header;
      if (chart) {
         header = paintChart(g, w, h - BAR);
      } else {
         header = paintGroups(g, w, h - BAR);
      }
      if (message != null) {
         header = new String[] {header[0], message, "error"};
      }
      paintHeader(g, w, header);
      paintBar(g, w, h);
   }

   /** Two lines for the header: a title, and a detail line drawn in the color of the third entry. */
   private void paintHeader(Graphics2D g, int w, String[] header) {
      text(g, header[0], TITLE, INK, 16, 22);
      Color color = header[2].equals("fresh") ? FRESH : header[2].equals("again") ? AGAIN : header[2].equals("error") ? ERROR : MUTED;
      text(g, fit(g, header[1], TEXT, w - 32), TEXT, color, 16, 41);
   }

   private String[] paintGroups(Graphics2D g, int w, int bottom) {
      if (groups.isEmpty()) {
         chips = new Rectangle[0][];
         paintEmptyDeck(g, w, bottom);
         return new String[] {"Group Creator", "Type the class above, then Make groups (or N) deals round 1. Later rounds mix in new partners.", ""};
      }
      Rectangle[] cards = layoutCards(w, HEADER + 6, bottom - HEADER - 12);
      int rowHeight = rowHeight(cards);
      chips = new Rectangle[groups.size()][];
      for (int c = 0; c < groups.size(); c++) {
         Rectangle card = cards[c];
         chips[c] = new Rectangle[groups.get(c).size()];
         g.setColor(new Color(0, 0, 0, 30));
         g.fillRoundRect(card.x + 2, card.y + 3, card.width, card.height, 14, 14);
         g.setColor(Color.WHITE);
         g.fillRoundRect(card.x, card.y, card.width, card.height, 14, 14);
         Color color = CARD_COLORS[c % CARD_COLORS.length];
         g.setColor(color);
         g.fillRoundRect(card.x, card.y, card.width, 26, 14, 14);
         g.fillRect(card.x, card.y + 14, card.width, 12);
         text(g, "Group " + (c + 1), BOLD, Color.WHITE, card.x + 10, card.y + 18);
         String size = groups.get(c).size() + (groups.get(c).size() == 1 ? " name" : " names");
         text(g, size, SMALL, Color.WHITE, card.x + card.width - 10 - width(g, size, SMALL), card.y + 18);
         for (int k = 0; k < groups.get(c).size(); k++) {
            chips[c][k] = new Rectangle(card.x + 8, card.y + 32 + k * rowHeight, card.width - 16, rowHeight - 3);
            g.setColor(new Color(244, 242, 236));
            Rectangle slot = chips[c][k];
            g.fillRoundRect(slot.x, slot.y, slot.width, slot.height, 8, 8);
         }
      }

      // The deal: each name flies from the top of the board to its place.
      long elapsed = dealStart > 0 ? System.currentTimeMillis() - dealStart : Long.MAX_VALUE / 2;
      List<int[]> order = dealOrder();
      hoverGroup = -1;
      hoverIndex = -1;
      for (int i = 0; i < order.size(); i++) {
         int c = order.get(i)[0];
         int k = order.get(i)[1];
         double t = Math.min(1, (elapsed - (double) i * dealStep) / FLY_MS);
         if (t <= 0) {
            continue;
         }
         double ease = 1 - (1 - t) * (1 - t) * (1 - t);
         Rectangle slot = chips[c][k];
         int x = (int) Math.round(w / 2.0 - slot.width / 2.0 + (slot.x - (w / 2.0 - slot.width / 2.0)) * ease);
         int y = (int) Math.round(HEADER - 20 + (slot.y - (HEADER - 20)) * ease);
         boolean landed = t >= 1;
         if (landed && slot.contains(mouseX, mouseY) && !dealing()) {
            hoverGroup = c;
            hoverIndex = k;
         }
         paintChip(g, groups.get(c).get(k), x, y, slot.width, slot.height, landed ? metAgain(c, k) : false,
            c == pickedGroup && k == pickedIndex, c == hoverGroup && k == hoverIndex);
      }
      return summary();
   }

   private void paintChip(Graphics2D g, String name, int x, int y, int width, int height, boolean again, boolean picked, boolean hover) {
      g.setColor(picked ? PICKED : again ? AGAIN_FILL : Color.WHITE);
      g.fillRoundRect(x, y, width, height, 8, 8);
      g.setColor(picked ? AGAIN : hover ? INK : new Color(205, 200, 190));
      g.setStroke(new BasicStroke(hover || picked ? 1.6f : 1f));
      g.drawRoundRect(x, y, width, height, 8, 8);
      g.setStroke(new BasicStroke(1f));
      Font font = height >= 16 ? TEXT : SMALL;
      int baseline = y + height / 2 + (height >= 16 ? 5 : 4);
      int room = width - 16 - (again ? 14 : 0);
      text(g, fit(g, name, font, room), font, INK, x + 8, baseline);
      if (again) {
         int dot = Math.min(8, height - 6);
         g.setColor(AGAIN);
         g.fillOval(x + width - 8 - dot, y + (height - dot) / 2, dot, dot);
      }
   }

   /** True when name k of group c shares this group with someone they were grouped with before. */
   private boolean metAgain(int c, int k) {
      List<String> group = groups.get(c);
      for (int other = 0; other < group.size(); other++) {
         if (other != k && before.together(group.get(k), group.get(other)) > 0) {
            return true;
         }
      }
      return false;
   }

   /** The header for the round shown, or for the name under the pointer or picked for a swap. */
   private String[] summary() {
      int round = history.size();
      int count = 0;
      for (List<String> group : groups) {
         count += group.size();
      }
      String title = "Round " + round + ": " + count + (count == 1 ? " name" : " names") + " in " + groups.size()
         + (groups.size() == 1 ? " group" : " groups");
      if (pickedGroup >= 0) {
         return new String[] {title, groups.get(pickedGroup).get(pickedIndex)
            + " is picked: click a name in another group to swap them (Esc cancels).", ""};
      }
      if (hoverGroup >= 0) {
         return new String[] {title, describe(hoverGroup, hoverIndex), metAgain(hoverGroup, hoverIndex) ? "again" : "fresh"};
      }
      if (round == 1) {
         return new String[] {title, "The first round is a plain shuffle. From the next one on, people get partners they have not had yet.", ""};
      }
      List<String[]> repeats = before.repeats(groups);
      if (repeats.isEmpty()) {
         return new String[] {title, "Everyone has new partners: no two people in a group were together before.", "fresh"};
      }
      String[] pair = repeats.get(0);
      String detail = repeats.size() == 1
         ? "1 pair was together before (" + pair[0] + " and " + pair[1] + "), the fewest repeats found."
         : repeats.size() + " pairs were together before (marked), the fewest repeats found.";
      return new String[] {title, detail, "again"};
   }

   /** Who name k of group c meets again, and who is new to them. */
   private String describe(int c, int k) {
      List<String> group = groups.get(c);
      String name = group.get(k);
      List<String> again = new ArrayList<>();
      List<String> fresh = new ArrayList<>();
      for (int other = 0; other < group.size(); other++) {
         if (other == k) {
            continue;
         }
         List<Integer> rounds = before.roundsTogether(name, group.get(other));
         if (rounds.isEmpty()) {
            fresh.add(group.get(other));
         } else {
            again.add(group.get(other) + " (round " + join(rounds) + ")");
         }
      }
      if (again.isEmpty() && fresh.isEmpty()) {
         return name + " is on their own this round. Click a name, then a name in another group, to swap them.";
      }
      StringBuilder text = new StringBuilder(name).append(": ");
      if (!fresh.isEmpty()) {
         text.append("new partners ").append(String.join(", ", fresh));
      }
      if (!again.isEmpty()) {
         text.append(fresh.isEmpty() ? "" : "; ").append("again with ").append(String.join(", ", again));
      }
      return text.toString();
   }

   private static String join(List<Integer> numbers) {
      List<String> parts = new ArrayList<>();
      for (int number : numbers) {
         parts.add(String.valueOf(number));
      }
      if (parts.size() == 1) {
         return parts.get(0);
      }
      return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
   }

   /** A small stack of blank cards, before the first round. */
   private void paintEmptyDeck(Graphics2D g, int w, int bottom) {
      int cx = w / 2;
      int cy = (HEADER + bottom) / 2;
      for (int i = 3; i >= 0; i--) {
         int x = cx - 80 + i * 10;
         int y = cy - 55 - i * 6;
         g.setColor(new Color(0, 0, 0, 25));
         g.fillRoundRect(x + 2, y + 3, 160, 110, 14, 14);
         g.setColor(Color.WHITE);
         g.fillRoundRect(x, y, 160, 110, 14, 14);
         g.setColor(CARD_COLORS[i]);
         g.fillRoundRect(x, y, 160, 26, 14, 14);
         g.fillRect(x, y + 14, 160, 12);
      }
      centered(g, "No groups yet", BOLD, MUTED, cx - 80 + 80, cy + 10);
   }

   /** One card per group, in as few rows as fit, centered on the board, as wide as fits (at most 230 px). */
   private Rectangle[] layoutCards(int w, int top, int height) {
      int count = groups.size();
      int longest = 0;
      for (List<String> group : groups) {
         longest = Math.max(longest, group.size());
      }
      // The fewest rows whose cards are at least 110 px wide and fit at full height.
      int columns = -1;
      int row = 22;
      for (int rows = 1; rows <= count; rows++) {
         int c = (count + rows - 1) / rows;
         int cardWidth = (w - 2 * GAP - (c - 1) * GAP) / c;
         if (cardWidth >= 110 && rows * (38 + longest * row) + (rows - 1) * GAP <= height) {
            columns = c;
            break;
         }
      }
      if (columns < 0) {
         // Too many to fit at full size: as many columns as fit, with shorter rows.
         columns = Math.max(1, Math.min(count, (w - GAP) / (100 + GAP)));
         int rows = (count + columns - 1) / columns;
         row = Math.max(12, Math.min(22, ((height - (rows - 1) * GAP) / rows - 38) / Math.max(1, longest)));
      }
      int cardWidth = Math.min(230, (w - 2 * GAP - (columns - 1) * GAP) / columns);
      int cardHeight = 38 + longest * row;
      int rows = (count + columns - 1) / columns;
      int y = top + Math.max(0, (height - rows * cardHeight - (rows - 1) * GAP) / 2);
      Rectangle[] cards = new Rectangle[count];
      for (int r = 0; r < rows; r++) {
         int inRow = Math.min(columns, count - r * columns);
         int x = (w - inRow * cardWidth - (inRow - 1) * GAP) / 2;
         for (int i = 0; i < inRow; i++) {
            cards[r * columns + i] = new Rectangle(x + i * (cardWidth + GAP), y + r * (cardHeight + GAP), cardWidth, cardHeight);
         }
      }
      return cards;
   }

   private int rowHeight(Rectangle[] cards) {
      int longest = 0;
      for (List<String> group : groups) {
         longest = Math.max(longest, group.size());
      }
      return cards.length == 0 || longest == 0 ? 22 : (cards[0].height - 38) / longest;
   }

   /** The partner chart: a cell for every pair of names, darker the more rounds they shared. */
   private String[] paintChart(Graphics2D g, int w, int bottom) {
      chips = new Rectangle[0][];
      List<String> people = new ArrayList<>();
      for (String name : names) {
         if (!people.contains(name)) {
            people.add(name);
         }
      }
      int n = people.size();
      if (n < 2 || history.isEmpty()) {
         centered(g, "The chart fills in as you make rounds.", BOLD, MUTED, w / 2, (HEADER + bottom) / 2);
         return new String[] {"Partner chart", "Who has been in a group with whom, over every round so far.", ""};
      }
      int labelWidth = 0;
      for (int i = 0; i < n; i++) {
         labelWidth = Math.max(labelWidth, width(g, people.get(i), SMALL));
      }
      labelWidth = Math.min(labelWidth, 130) + 26;
      int metWidth = 70;
      int top = HEADER + 22;
      int cell = Math.max(6, Math.min(26, Math.min((w - 32 - labelWidth - metWidth) / n, (bottom - top - 8) / n)));
      int left = (w - labelWidth - n * cell - metWidth) / 2 + labelWidth;

      int hoverRow = -1;
      int hoverColumn = -1;
      if (mouseX >= left && mouseX < left + n * cell && mouseY >= top && mouseY < top + n * cell) {
         hoverRow = (mouseY - top) / cell;
         hoverColumn = (mouseX - left) / cell;
      }

      int pairsMet = 0;
      int most = 1;
      for (int i = 0; i < n; i++) {
         for (int j = i + 1; j < n; j++) {
            int together = history.together(people.get(i), people.get(j));
            pairsMet += together > 0 ? 1 : 0;
            most = Math.max(most, together);
         }
      }
      for (int i = 0; i < n; i++) {
         boolean lit = i == hoverRow || i == hoverColumn;
         String label = fit(g, people.get(i), SMALL, labelWidth - 26);
         text(g, label, lit ? BOLD : SMALL, lit ? INK : MUTED, left - 8 - width(g, label, lit ? BOLD : SMALL), top + i * cell + cell / 2 + 4);
         if (cell >= 14 || i % 2 == 0) {
            String number = String.valueOf(i + 1);
            centered(g, number, SMALL, lit ? INK : MUTED, left + i * cell + cell / 2, top - 6);
         }
         int met = 0;
         for (int j = 0; j < n; j++) {
            int together = history.together(people.get(i), people.get(j));
            met += together > 0 ? 1 : 0;
            int x = left + j * cell;
            int y = top + i * cell;
            if (i == j) {
               g.setColor(new Color(200, 196, 186));
            } else if (together == 0) {
               g.setColor(Color.WHITE);
            } else {
               // From a light to a full green: the most-met pair is the darkest.
               float share = (float) together / most;
               g.setColor(mix(new Color(196, 232, 206), FRESH, share));
            }
            g.fillRect(x, y, cell - 1, cell - 1);
            if (together > 0 && cell >= 16) {
               centered(g, String.valueOf(together), SMALL, share(together, most) > 0.6f ? Color.WHITE : INK, x + cell / 2, y + cell / 2 + 4);
            }
            if ((i == hoverRow && j == hoverColumn) && i != j) {
               g.setColor(INK);
               g.setStroke(new BasicStroke(2f));
               g.drawRect(x, y, cell - 2, cell - 2);
               g.setStroke(new BasicStroke(1f));
            }
         }
         text(g, "met " + met + "/" + (n - 1), SMALL, met == n - 1 ? FRESH : MUTED, left + n * cell + 8, top + i * cell + cell / 2 + 4);
      }
      // Number the rows too, so a column number can be matched to its name.
      for (int i = 0; i < n; i++) {
         if (cell >= 14 || i % 2 == 0) {
            text(g, String.valueOf(i + 1), SMALL, MUTED, left - labelWidth + 2, top + i * cell + cell / 2 + 4);
         }
      }

      int pairs = n * (n - 1) / 2;
      String title = "Partner chart: " + pairsMet + " of " + pairs + " pairs have shared a group in "
         + history.size() + (history.size() == 1 ? " round" : " rounds");
      if (hoverRow >= 0 && hoverRow != hoverColumn) {
         String a = people.get(hoverRow);
         String b = people.get(hoverColumn);
         List<Integer> rounds = history.roundsTogether(a, b);
         if (rounds.isEmpty()) {
            return new String[] {title, a + " and " + b + " have not been in a group together yet.", "fresh"};
         }
         return new String[] {title, a + " and " + b + ": together in round " + join(rounds) + ".", "again"};
      }
      if (pairsMet == pairs) {
         return new String[] {title, "Everyone has worked with everyone. New rounds now repeat the pairs met least.", "fresh"};
      }
      return new String[] {title, "Each cell counts the rounds a pair shared a group. Point at one for the rounds.", ""};
   }

   private static float share(int together, int most) {
      return (float) together / most;
   }

   private static Color mix(Color from, Color to, float amount) {
      return new Color(
         Math.round(from.getRed() + (to.getRed() - from.getRed()) * amount),
         Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * amount),
         Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * amount));
   }

   private void paintBar(Graphics2D g, int w, int h) {
      int top = h - BAR;
      g.setColor(new Color(222, 214, 198));
      g.fillRect(0, top, w, BAR);
      int gap = 8;
      int total = -gap;
      for (Button button : buttons) {
         total += buttonWidth(g, button) + gap;
      }
      int x = (w - total) / 2;
      for (Button button : buttons) {
         int bw = buttonWidth(g, button);
         button.bounds.setBounds(x, top + 10, bw, 30);
         boolean hover = button.enabled && button.bounds.contains(mouseX, mouseY);
         boolean armed = button == clear && clearArmed;
         g.setColor(armed ? ERROR : hover ? new Color(255, 250, 238) : Color.WHITE);
         g.fillRoundRect(x, top + 10, bw, 30, 14, 14);
         g.setColor(button.enabled ? INK : new Color(180, 176, 168));
         g.setStroke(new BasicStroke(hover ? 2f : 1.2f));
         g.drawRoundRect(x, top + 10, bw, 30, 14, 14);
         g.setStroke(new BasicStroke(1f));
         Color ink = armed ? Color.WHITE : button.enabled ? INK : new Color(170, 166, 158);
         text(g, button.label, BOLD, ink, x + 12, top + 30);
         String key = String.valueOf(Character.toUpperCase(button.key));
         text(g, key, SMALL, armed ? Color.WHITE : MUTED, x + bw - 12 - width(g, key, SMALL), top + 30);
         x += bw + gap;
      }
   }

   private static int buttonWidth(Graphics2D g, Button button) {
      return 12 + width(g, button.label, BOLD) + 10 + width(g, String.valueOf(Character.toUpperCase(button.key)), SMALL) + 12;
   }

   private static void text(Graphics2D g, String text, Font font, Color color, int x, int y) {
      g.setFont(font);
      g.setColor(color);
      g.drawString(text, x, y);
   }

   private static void centered(Graphics2D g, String text, Font font, Color color, int centerX, int y) {
      text(g, text, font, color, centerX - width(g, text, font) / 2, y);
   }

   private static int width(Graphics2D g, String text, Font font) {
      return g.getFontMetrics(font).stringWidth(text);
   }

   /** The text, cut short with "..." to fit in {@code room} pixels. */
   private static String fit(Graphics2D g, String text, Font font, int room) {
      if (width(g, text, font) <= room) {
         return text;
      }
      String cut = text;
      while (cut.length() > 1 && width(g, cut + "...", font) > room) {
         cut = cut.substring(0, cut.length() - 1);
      }
      return cut + "...";
   }
}
