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
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The board under the inputs. In Groups it deals the newest round onto one card per group, marks the people who were
 * together before, and swaps two names of different groups when they are clicked one after the other. Partner chart
 * shows, for every pair in the class, how many rounds put them in one group. Pick someone spins a wheel of the class
 * that lands on someone who has not had a turn yet. Roll call marks who is away today and which names are kept apart.
 * Its buttons run along the bottom, and change with the view.
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
   static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
   private static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);

   /** What the board's buttons and edits ask the window to do. */
   public interface Actions {
      void newRound();

      void reshuffle();

      void clearHistory();

      /** The newest round was changed by hand (two names swapped). */
      void edited(List<List<String>> groups);

      /** The names typed in the names field now (the wheel follows the field, not only the saved rounds). */
      List<String> classNames();

      /** The wheel picked someone, or a pick was put back or forgotten: save the class. */
      void picksChanged();

      /** Someone was marked away or here, or a keep-apart rule was added or removed: save the class. */
      void rulesChanged();
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
   private final PickBag picks;
   private final ClassRules rules;
   private final Actions actions;
   private final List<Button> groupButtons = new ArrayList<>();
   private final List<Button> pickButtons = new ArrayList<>();
   private final List<Button> rollButtons = new ArrayList<>();
   private final Button keepApartButton;
   private final Button reshuffle;
   private final Button chartButton;
   private final Button clear;
   private final Button spin;
   private final Button putBack;
   private final Button startOver;
   private final PickWheel wheel;
   private final Random random = new Random();
   private boolean picking;
   private boolean rollCall;

   // Roll call: the name tiles, the keep-apart list beside them, and the keyboard's place among the tiles.
   private boolean keepApart;
   private String linkFrom;
   private List<String> tileNames = Collections.emptyList();
   private Rectangle[] tiles = new Rectangle[0];
   private int tileColumns = 1;
   private List<String[]> ruleList = Collections.emptyList();
   private Rectangle[] ruleRows = new Rectangle[0];
   private int focus = -1;
   private boolean keyboard;

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

   public GroupBoard(PartnerHistory history, PickBag picks, ClassRules rules, Actions actions) {
      this.history = history;
      this.picks = picks;
      this.rules = rules;
      this.actions = actions;
      setPreferredSize(new Dimension(WIDTH, HEIGHT));
      setFocusable(true);
      wheel = new PickWheel(this::repaint, CARD_COLORS);
      List<Button> buttons = groupButtons;
      buttons.add(new Button("New round", 'n', actions::newRound));
      reshuffle = new Button("Reshuffle", 'r', actions::reshuffle);
      buttons.add(reshuffle);
      chartButton = new Button("Partner chart", 'c', () -> {
         chart = !chart;
         unpick();
      });
      buttons.add(chartButton);
      buttons.add(new Button("Pick someone", 'p', () -> {
         picking = true;
         unpick();
      }));
      buttons.add(new Button("Roll call", 'a', () -> {
         rollCall = true;
         unpick();
      }));
      rollButtons.add(new Button("Everyone here", 'e', () -> {
         rules.everyoneHere();
         actions.rulesChanged();
      }));
      keepApartButton = new Button("Keep apart", 'k', () -> {
         keepApart = !keepApart;
         linkFrom = null;
      });
      rollButtons.add(keepApartButton);
      rollButtons.add(new Button("Make groups", 'n', actions::newRound));
      rollButtons.add(new Button("Groups", 'a', () -> {
         rollCall = false;
         keepApart = false;
         linkFrom = null;
      }));
      spin = new Button("Spin", 's', this::spin);
      pickButtons.add(spin);
      putBack = new Button("Put back", 'b', () -> {
         wheel.reset();
         picks.putBack(actions.classNames());
         actions.picksChanged();
      });
      pickButtons.add(putBack);
      startOver = new Button("Start over", 'o', () -> {
         wheel.reset();
         picks.clear();
         actions.picksChanged();
      });
      pickButtons.add(startOver);
      pickButtons.add(new Button("Groups", 'p', () -> {
         wheel.finish();
         picking = false;
      }));
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
            keyboard = false;
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
      if (deal) {
         picking = false;
         rollCall = false;
         keepApart = false;
         linkFrom = null;
      }
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

   private List<Button> buttons() {
      return picking ? pickButtons : rollCall ? rollButtons : groupButtons;
   }

   /** Picks someone who has not had a turn yet, and spins the wheel to them. */
   private void spin() {
      if (wheel.spinning()) {
         return;
      }
      List<String> names = rules.present(actions.classNames());
      List<String> pool = picks.pool(names);
      String name = picks.pick(names, random);
      if (name == null) {
         return;
      }
      wheel.spin(pool, name);
      actions.picksChanged();
   }

   private void press(int x, int y) {
      if (y >= getHeight() - BAR) {
         for (Button button : buttons()) {
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
      if (rollCall) {
         pressRollCall(x, y);
         return;
      }
      if (picking) {
         if (wheel.spinning()) {
            wheel.finish();
         } else if (wheel.contains(x, y)) {
            spin();
         }
         return;
      }
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
      if (rollCall && rollCallKey(event)) {
         return;
      }
      if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
         unpick();
         clearArmed = false;
         return;
      }
      char key = Character.toLowerCase(event.getKeyChar());
      if (key == ' ') {
         key = picking ? 's' : 'n';
      }
      for (Button button : buttons()) {
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
      List<String> present = rules.present(actions.classNames());
      spin.enabled = !wheel.spinning() && !present.isEmpty();
      putBack.enabled = !wheel.spinning() && !picks.picked(present).isEmpty();
      keepApartButton.label = keepApart ? "Keep apart: on" : "Keep apart";
      startOver.enabled = putBack.enabled;
      String[] header;
      if (picking) {
         header = paintPick(g, w, h - BAR);
      } else if (rollCall) {
         header = paintRollCall(g, w, h - BAR);
      } else if (chart) {
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
         if (landed && clashes(c, k)) {
            // Kept apart from someone in this group: a red ring.
            g.setColor(ERROR);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, y, slot.width, slot.height, 8, 8);
            g.setStroke(new BasicStroke(1f));
         }
      }
      return summary();
   }

   /** The wheel on the left, and on the right who it picked, how far round the class it is, and the turns so far. */
   private String[] paintPick(Graphics2D g, int w, int bottom) {
      chips = new Rectangle[0][];
      List<String> names = rules.present(actions.classNames());
      List<String> done = picks.picked(names);
      List<String> pool = picks.pool(names);
      List<String> toGo = picks.remaining(names);
      if (wheel.spinning() && !done.isEmpty()) {
         // The pick is made when the spin starts; keep it secret until the wheel stops.
         done.remove(done.size() - 1);
         toGo = new ArrayList<>();
         for (String name : names) {
            if (!done.contains(name) && !toGo.contains(name)) {
               toGo.add(name);
            }
         }
      }
      int everyone = toGo.size() + done.size();
      int top = HEADER + 4;
      int height = bottom - top - 6;
      int size = Math.min(height, w * 56 / 100);
      wheel.paint(g, pool, GAP, top, size, height);

      int left = GAP + size + 20;
      int width = w - left - 16;
      String landed = wheel.landedOn();
      String under = wheel.underPointer();
      // The result card.
      int cardTop = top + 14;
      int cardHeight = 112;
      g.setColor(new Color(0, 0, 0, 30));
      g.fillRoundRect(left + 2, cardTop + 3, width, cardHeight, 14, 14);
      g.setColor(Color.WHITE);
      g.fillRoundRect(left, cardTop, width, cardHeight, 14, 14);
      Color strip = landed != null ? wheel.landedColor() : INK;
      g.setColor(strip);
      g.fillRoundRect(left, cardTop, width, 26, 14, 14);
      g.fillRect(left, cardTop + 14, width, 12);
      String label = landed != null ? "Turn " + done.size() + " of " + everyone : wheel.spinning() ? "Spinning..." : "Who is next?";
      text(g, label, BOLD, Color.WHITE, left + 12, cardTop + 18);
      Font big = new Font("SansSerif", Font.BOLD, 30);
      if (landed != null) {
         text(g, fit(g, landed, big, width - 24), big, INK, left + 12, cardTop + 70);
         String group = groupOf(landed);
         if (group != null) {
            text(g, group, SMALL, MUTED, left + 12, cardTop + 94);
         }
      } else if (wheel.spinning() && under != null) {
         text(g, fit(g, under, big, width - 24), big, MUTED, left + 12, cardTop + 70);
      } else {
         boolean allAway = names.isEmpty() && !actions.classNames().isEmpty();
         text(g, names.isEmpty() ? "Nobody yet" : "Spin the wheel", big, new Color(190, 186, 176), left + 12, cardTop + 70);
         text(g, allAway ? "Everyone is marked away in Roll call." : names.isEmpty() ? "Type the class in the names field."
            : "Space, S or a click on the wheel", SMALL, MUTED, left + 12, cardTop + 94);
      }

      // How far round the class this time around.
      int y = cardTop + cardHeight + 26;
      int had = done.size();
      text(g, "Had a turn: " + had + " of " + everyone, BOLD, INK, left, y);
      y += 8;
      g.setColor(new Color(222, 214, 198));
      g.fillRoundRect(left, y, width, 10, 10, 10);
      if (everyone > 0 && had > 0) {
         g.setColor(FRESH);
         g.fillRoundRect(left, y, Math.max(10, width * had / everyone), 10, 10, 10);
      }
      y += 30;
      // The turns so far, newest first, as many as fit.
      List<String> recent = new ArrayList<>(done);
      Collections.reverse(recent);
      int lines = Math.max(0, (bottom - 26 - y) / 18);
      for (int i = 0; i < Math.min(lines, recent.size()); i++) {
         String line = (done.size() - i) + ". " + recent.get(i);
         if (i == lines - 1 && recent.size() > lines) {
            line = "... and " + (recent.size() - i) + " more";
         }
         text(g, fit(g, line, TEXT, width), i == 0 && landed != null ? BOLD : TEXT, i == 0 && landed != null ? INK : MUTED, left, y);
         y += 18;
      }
      if (!toGo.isEmpty() && !done.isEmpty()) {
         text(g, fit(g, "Still to go: " + String.join(", ", toGo), SMALL, width), SMALL, MUTED, left, bottom - 12);
      }

      String title = "Pick someone: " + (names.isEmpty() ? "no names yet"
         : toGo.size() + " of " + everyone + " still to have a turn this time around");
      if (wheel.spinning()) {
         return new String[] {title, "Round and round it goes... (a click stops it where it was heading)", ""};
      }
      if (landed != null && toGo.isEmpty()) {
         return new String[] {title, landed + " is up, and everyone has now had a turn. The next spin puts everyone else back on the wheel.", "fresh"};
      }
      if (landed != null) {
         return new String[] {title, landed + " is up. Away today? Put back (B), and mark them away in Roll call.", "fresh"};
      }
      if (!names.isEmpty() && toGo.isEmpty()) {
         return new String[] {title, "Everyone has had a turn. The next spin starts again with everyone but the last pick.", "fresh"};
      }
      List<String> away = new ArrayList<>(distinct(actions.classNames()));
      away.removeAll(names);
      if (!away.isEmpty()) {
         return new String[] {title, "The wheel holds those here today who have not had a turn yet. Away today: "
            + String.join(", ", away) + ".", ""};
      }
      return new String[] {title, "The wheel only holds people who have not had a turn yet, so everyone gets one before anyone gets two.", ""};
   }

   /** "In Group 2 this round" for a name in the newest round, or null. */
   private String groupOf(String name) {
      for (int c = 0; c < groups.size(); c++) {
         if (groups.get(c).contains(name)) {
            return "In Group " + (c + 1) + " this round";
         }
      }
      return null;
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

   /** True when name k of group c shares this group with someone they are kept apart from. */
   private boolean clashes(int c, int k) {
      List<String> group = groups.get(c);
      return !rules.apartFrom(group.get(k), group).isEmpty();
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
      List<String[]> broken = rules.broken(groups);
      if (!broken.isEmpty()) {
         String detail = broken.size() == 1
            ? broken.get(0)[0] + " and " + broken.get(0)[1] + " should be kept apart, but share Group " + groupNumber(broken.get(0)[0])
               + " (ringed in red)."
            : broken.size() + " keep-apart pairs share a group (ringed in red). Swap a name by hand, or Reshuffle (R).";
         return new String[] {title, detail, "error"};
      }
      int kept = rules.rules(flatten(groups)).size();
      if (round == 1 && kept > 0) {
         return new String[] {title, "A shuffle that keeps " + kept + (kept == 1 ? " pair" : " pairs")
            + " apart. From the next round on, people also get partners they have not had yet.", "fresh"};
      }
      if (round == 1) {
         return new String[] {title, "The first round is a plain shuffle. From the next one on, people get partners they have not had yet.", ""};
      }
      List<String[]> repeats = before.repeats(groups);
      if (repeats.isEmpty()) {
         return new String[] {title, "Everyone has new partners: no two people in a group were together before"
            + (kept > 0 ? ", and every keep-apart pair is apart." : "."), "fresh"};
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
      List<String> clash = rules.apartFrom(name, group);
      if (!clash.isEmpty()) {
         return name + " should be kept apart from " + String.join(", ", clash) + ". Click a name, then one in another group, to swap.";
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

   /** The number (from 1) of the group holding {@code name} in the round shown, or 0. */
   private int groupNumber(String name) {
      for (int c = 0; c < groups.size(); c++) {
         if (groups.get(c).contains(name)) {
            return c + 1;
         }
      }
      return 0;
   }

   private static List<String> flatten(List<List<String>> groups) {
      List<String> all = new ArrayList<>();
      for (List<String> group : groups) {
         all.addAll(group);
      }
      return all;
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

   static Color mix(Color from, Color to, float amount) {
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
      List<Button> buttons = buttons();
      // Narrower padding when the buttons would not fit (the Groups bar with "Sure? Press again").
      int pad = 12;
      int total = barWidth(g, buttons, pad, gap);
      if (total > w - 16) {
         pad = 7;
         total = barWidth(g, buttons, pad, gap);
      }
      int x = (w - total) / 2;
      for (Button button : buttons) {
         int bw = buttonWidth(g, button, pad);
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
         text(g, button.label, BOLD, ink, x + pad, top + 30);
         String key = String.valueOf(Character.toUpperCase(button.key));
         text(g, key, SMALL, armed ? Color.WHITE : MUTED, x + bw - pad - width(g, key, SMALL), top + 30);
         x += bw + gap;
      }
   }

   private static int barWidth(Graphics2D g, List<Button> buttons, int pad, int gap) {
      int total = -gap;
      for (Button button : buttons) {
         total += buttonWidth(g, button, pad) + gap;
      }
      return total;
   }

   private static int buttonWidth(Graphics2D g, Button button, int pad) {
      return pad + width(g, button.label, BOLD) + 10 + width(g, String.valueOf(Character.toUpperCase(button.key)), SMALL) + pad;
   }

   /** The class's names, each once, in the order typed. */
   private static List<String> distinct(List<String> names) {
      List<String> once = new ArrayList<>();
      for (String name : names) {
         if (!once.contains(name)) {
            once.add(name);
         }
      }
      return once;
   }

   /** A click in Roll call: a tile marks someone away or here (or links two names in Keep apart), a rule row removes it. */
   private void pressRollCall(int x, int y) {
      for (int i = 0; i < tiles.length; i++) {
         if (tiles[i] != null && tiles[i].contains(x, y)) {
            focus = i;
            activateTile(i);
            return;
         }
      }
      for (int r = 0; r < ruleRows.length; r++) {
         if (ruleRows[r] != null && ruleRows[r].contains(x, y)) {
            rules.toggleApart(ruleList.get(r)[0], ruleList.get(r)[1]);
            actions.rulesChanged();
            return;
         }
      }
      linkFrom = null;
   }

   private void activateTile(int i) {
      String name = tileNames.get(i);
      if (!keepApart) {
         rules.toggleAway(name);
         actions.rulesChanged();
      } else if (linkFrom == null) {
         linkFrom = name;
      } else if (linkFrom.equals(name)) {
         linkFrom = null;
      } else {
         rules.toggleApart(linkFrom, name);
         actions.rulesChanged();
      }
   }

   /** Arrow keys move among the tiles, Enter or Space acts on the one with the ring, Esc lets go. True when used. */
   private boolean rollCallKey(KeyEvent event) {
      int code = event.getKeyCode();
      int n = tileNames.size();
      int step = code == KeyEvent.VK_LEFT ? -1 : code == KeyEvent.VK_RIGHT ? 1
         : code == KeyEvent.VK_UP ? -tileColumns : code == KeyEvent.VK_DOWN ? tileColumns : 0;
      if (step != 0 && n > 0) {
         keyboard = true;
         focus = focus < 0 ? 0 : Math.max(0, Math.min(n - 1, focus + step));
         return true;
      }
      if ((code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) && n > 0) {
         keyboard = true;
         if (focus < 0 || focus >= n) {
            focus = 0;
         } else {
            activateTile(focus);
         }
         return true;
      }
      if (code == KeyEvent.VK_ESCAPE) {
         if (linkFrom != null) {
            linkFrom = null;
         } else {
            keepApart = false;
         }
         return true;
      }
      return false;
   }

   /** Roll call: a tile per name, here or away, and the keep-apart rules in a card beside them. */
   private String[] paintRollCall(Graphics2D g, int w, int bottom) {
      chips = new Rectangle[0][];
      List<String> people = distinct(actions.classNames());
      tileNames = people;
      int n = people.size();
      if (focus >= n) {
         focus = n - 1;
      }
      if (linkFrom != null && !people.contains(linkFrom)) {
         linkFrom = null;
      }
      int side = 232;
      int top = HEADER + 8;
      int areaWidth = w - side - 3 * GAP;
      int areaHeight = bottom - top - 8;
      tiles = new Rectangle[n];
      String hovered = null;
      if (n == 0) {
         centered(g, "Type the class in the names field above.", BOLD, MUTED, GAP + areaWidth / 2, (top + bottom) / 2);
      } else {
         int columns = Math.max(1, Math.min(n, (areaWidth + 8) / 118));
         int rows = (n + columns - 1) / columns;
         int tileHeight = Math.max(16, Math.min(48, (areaHeight - (rows - 1) * 6) / rows));
         int tileWidth = (areaWidth - (columns - 1) * 8) / columns;
         int y0 = top;
         tileColumns = columns;
         for (int i = 0; i < n; i++) {
            tiles[i] = new Rectangle(GAP + (i % columns) * (tileWidth + 8), y0 + (i / columns) * (tileHeight + 6), tileWidth, tileHeight);
            if (tiles[i].contains(mouseX, mouseY)) {
               hovered = people.get(i);
            }
         }
         for (int i = 0; i < n; i++) {
            paintTile(g, people, i);
         }
         // Red threads from the name being linked (or under the pointer) to the names it is kept apart from.
         String from = linkFrom != null ? linkFrom : hovered != null ? hovered : keyboard && focus >= 0 ? people.get(focus) : null;
         if (from != null) {
            Rectangle a = tiles[people.indexOf(from)];
            g.setColor(new Color(ERROR.getRed(), ERROR.getGreen(), ERROR.getBlue(), 170));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (String other : rules.apartFrom(from, people)) {
               Rectangle b = tiles[people.indexOf(other)];
               dashed(g, a.x + a.width / 2, a.y + a.height / 2, b.x + b.width / 2, b.y + b.height / 2);
            }
            g.setStroke(new BasicStroke(1f));
            for (String other : rules.apartFrom(from, people)) {
               Rectangle b = tiles[people.indexOf(other)];
               g.setColor(ERROR);
               g.fillOval(b.x + b.width / 2 - 4, b.y + b.height / 2 - 4, 8, 8);
            }
         }
      }
      paintRules(g, people, w - side - GAP, top, side, bottom - top - 8);

      int away = rules.awayCount(people);
      int pairs = rules.rules(people).size();
      String title = "Roll call: " + (n - away) + " of " + n + " here" + (pairs == 0 ? ""
         : ", " + pairs + (pairs == 1 ? " pair" : " pairs") + " kept apart");
      if (n == 0) {
         return new String[] {title, "Mark who is away today, and who should not share a group.", ""};
      }
      if (keepApart && linkFrom != null) {
         List<String> others = rules.apartFrom(linkFrom, people);
         return new String[] {title, linkFrom + (others.isEmpty() ? " is linked to nobody yet" : " is kept apart from " + String.join(", ", others))
            + ". Click names to add or remove; Esc when done.", "error"};
      }
      if (hovered != null) {
         List<String> others = rules.apartFrom(hovered, people);
         String state = hovered + (rules.isAway(hovered) ? " is away today: they sit out the groups and the wheel." : " is here.");
         return new String[] {title, state + (others.isEmpty() ? "" : " Kept apart from " + String.join(", ", others) + "."), ""};
      }
      if (keepApart) {
         return new String[] {title, "Keep apart: click a name, then each name they should never share a group with.", "error"};
      }
      return new String[] {title, "Click a name to mark them away today (again for here). Away names keep their history and turns.", ""};
   }

   /** A dashed line, drawn as short segments (the browser build's strokes have no dash pattern). */
   private static void dashed(Graphics2D g, int x1, int y1, int x2, int y2) {
      double length = Math.hypot(x2 - x1, y2 - y1);
      for (double d = 0; d < length; d += 11) {
         double e = Math.min(length, d + 6);
         g.drawLine((int) Math.round(x1 + (x2 - x1) * d / length), (int) Math.round(y1 + (y2 - y1) * d / length),
            (int) Math.round(x1 + (x2 - x1) * e / length), (int) Math.round(y1 + (y2 - y1) * e / length));
      }
   }

   private void paintTile(Graphics2D g, List<String> people, int i) {
      Rectangle t = tiles[i];
      String name = people.get(i);
      boolean away = rules.isAway(name);
      boolean selected = name.equals(linkFrom);
      boolean linked = linkFrom != null && rules.apart(linkFrom, name);
      boolean hover = t.contains(mouseX, mouseY);
      if (!away) {
         g.setColor(new Color(0, 0, 0, 30));
         g.fillRoundRect(t.x + 2, t.y + 3, t.width, t.height, 10, 10);
      }
      g.setColor(selected ? PICKED : away ? new Color(226, 220, 207) : Color.WHITE);
      g.fillRoundRect(t.x, t.y, t.width, t.height, 10, 10);
      if (!away) {
         // A green strip down the left edge: here today.
         g.setColor(FRESH);
         g.fillRoundRect(t.x, t.y, 8, t.height, 8, 8);
         g.setColor(selected ? PICKED : Color.WHITE);
         g.fillRect(t.x + 5, t.y, 4, t.height);
      }
      boolean ring = keyboard && i == focus;
      g.setColor(ring ? new Color(40, 100, 220) : linked ? ERROR : hover ? INK : away ? new Color(190, 184, 172) : new Color(205, 200, 190));
      g.setStroke(new BasicStroke(ring ? 2.6f : hover || linked ? 1.8f : 1f));
      g.drawRoundRect(t.x, t.y, t.width, t.height, 10, 10);
      g.setStroke(new BasicStroke(1f));

      int count = rules.apartFrom(name, people).size();
      int room = t.width - 22 - (count > 0 ? 22 : 0);
      boolean twoLines = t.height >= 34;
      int nameY = twoLines ? t.y + t.height / 2 - 2 : t.y + t.height / 2 + 5;
      text(g, fit(g, name, BOLD, room), BOLD, away ? MUTED : INK, t.x + 14, nameY);
      if (twoLines) {
         String status = away ? "Away today" : linked ? "Kept apart" : "Here";
         text(g, status, SMALL, linked ? ERROR : away ? MUTED : FRESH, t.x + 14, nameY + 15);
      }
      if (away) {
         // A line through the name.
         g.setColor(MUTED);
         g.drawLine(t.x + 12, nameY - 4, t.x + 16 + Math.min(room, width(g, name, BOLD)), nameY - 4);
      }
      if (count > 0) {
         // A red badge: how many names this one is kept apart from.
         int d = Math.min(18, t.height - 8);
         int bx = t.x + t.width - d - 6;
         int by = t.y + (t.height - d) / 2;
         g.setColor(ERROR);
         g.fillOval(bx, by, d, d);
         centered(g, String.valueOf(count), SMALL, Color.WHITE, bx + d / 2, by + d / 2 + 4);
      }
   }

   /** The keep-apart card: one row per rule, a click on a row removes it. */
   private void paintRules(Graphics2D g, List<String> people, int x, int y, int width, int height) {
      ruleList = rules.rules(people);
      g.setColor(new Color(0, 0, 0, 30));
      g.fillRoundRect(x + 2, y + 3, width, height, 14, 14);
      g.setColor(Color.WHITE);
      g.fillRoundRect(x, y, width, height, 14, 14);
      g.setColor(ERROR);
      g.fillRoundRect(x, y, width, 26, 14, 14);
      g.fillRect(x, y + 14, width, 12);
      text(g, "Keep apart", BOLD, Color.WHITE, x + 10, y + 18);
      String count = ruleList.size() + (ruleList.size() == 1 ? " pair" : " pairs");
      text(g, count, SMALL, Color.WHITE, x + width - 10 - width(g, count, SMALL), y + 18);
      ruleRows = new Rectangle[ruleList.size()];
      int rowY = y + 34;
      if (ruleList.isEmpty()) {
         String[] lines = {"Names that should never", "share a group: turn on", "Keep apart (K), then click", "two names. The mix keeps", "them apart whenever the", "group sizes allow."};
         for (String line : lines) {
            text(g, line, TEXT, MUTED, x + 12, rowY + 12);
            rowY += 18;
         }
         return;
      }
      int fits = Math.max(1, (y + height - 8 - rowY) / 22);
      for (int r = 0; r < ruleList.size(); r++) {
         if (r == fits - 1 && ruleList.size() > fits) {
            text(g, "... and " + (ruleList.size() - r) + " more", SMALL, MUTED, x + 12, rowY + 15);
            break;
         }
         Rectangle row = new Rectangle(x + 6, rowY, width - 12, 20);
         ruleRows[r] = row;
         boolean hover = row.contains(mouseX, mouseY);
         if (hover) {
            g.setColor(new Color(255, 230, 226));
            g.fillRoundRect(row.x, row.y, row.width, row.height, 8, 8);
         }
         String[] pair = ruleList.get(r);
         String label = fit(g, pair[0] + "  /  " + pair[1], TEXT, row.width - 58);
         text(g, label, TEXT, INK, row.x + 6, row.y + 15);
         text(g, hover ? "remove" : "x", SMALL, hover ? ERROR : MUTED, row.x + row.width - 6 - width(g, hover ? "remove" : "x", SMALL), row.y + 14);
         rowY += 22;
      }
   }

   static void text(Graphics2D g, String text, Font font, Color color, int x, int y) {
      g.setFont(font);
      g.setColor(color);
      g.drawString(text, x, y);
   }

   static void centered(Graphics2D g, String text, Font font, Color color, int centerX, int y) {
      text(g, text, font, color, centerX - width(g, text, font) / 2, y);
   }

   static int width(Graphics2D g, String text, Font font) {
      return g.getFontMetrics(font).stringWidth(text);
   }

   /** The text, cut short with "..." to fit in {@code room} pixels. */
   static String fit(Graphics2D g, String text, Font font, int room) {
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
