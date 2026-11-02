package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * Sorting race: six sorts on the same numbers, side by side, each getting the same number of steps per tick. The
 * bars show each compare (orange) and each swap or write (red); a finished sort is checked bar by bar in green and
 * gets its place. Click a sort to listen to it: each tick plays a note pitched by the value it just touched.
 */
class SortPage extends Page {
	private static final int[] SIZES = { 16, 32, 64, 128 };
	private static final int[] SPEEDS = { 1, 3, 10, 40 };
	private static final String[] SPEED_NAMES = { "Slow", "Normal", "Fast", "Turbo" };
	private static final Color BAR = new Color(120, 146, 190);
	private static final Color COMPARE = new Color(232, 146, 40);
	private static final Color[] MEDALS = { new Color(214, 170, 30), new Color(150, 160, 175), new Color(190, 120, 70) };
	private static final int TOP = 66;
	private static final int MARGIN = 16;
	private static final int GAP = 8;

	private final Random random = new Random();
	private final SortSound sound = new SortSound();
	private SortRace race;
	private int size = 1;
	private int order;
	private int speed = 1;
	private boolean playing = true;
	/** The lane being listened to, and the one under the pointer (-1 for none). */
	private int listen = 4;
	private int hovered = -1;
	/** The page's height at the last paint, so clicks find the same lanes. */
	private int pageHeight = LabPanel.HEIGHT - 90;

	private final Button play;
	private final Button orderButton;
	private final Button sizeButton;
	private final Button speedButton;
	private final Button soundButton;

	SortPage() {
		super("Sorting race");
		play = button("Pause", ' ', this::togglePlay);
		button("New numbers", 'n', this::newNumbers);
		orderButton = button(Sorts.ORDERS[order], 'o', () -> {
			order = (order + 1) % Sorts.ORDERS.length;
			newNumbers();
		});
		sizeButton = button("", 'b', () -> {
			size = (size + 1) % SIZES.length;
			newNumbers();
		});
		speedButton = button("", 'f', () -> {
			speed = (speed + 1) % SPEEDS.length;
			labels();
		});
		soundButton = button("", 'm', () -> {
			sound.toggle();
			labels();
		});
		newNumbers();
	}

	private void newNumbers() {
		race = new SortRace(Sorts.numbers(order, SIZES[size], random));
		playing = true;
		labels();
	}

	/** The same numbers again, from the start. */
	private void restart() {
		race = new SortRace(race.numbers);
		playing = true;
		labels();
	}

	private void labels() {
		play.label = playing ? "Pause" : done() ? "Again" : "Play";
		orderButton.label = Sorts.ORDERS[order];
		sizeButton.label = SIZES[size] + " numbers";
		speedButton.label = SPEED_NAMES[speed];
		soundButton.label = sound.isOn() ? "Sound on" : "Sound off";
	}

	private boolean done() {
		for (SortRace.Lane lane : race.lanes) {
			if (!lane.swept()) {
				return false;
			}
		}
		return true;
	}

	private void togglePlay() {
		if (done()) {
			restart();
			return;
		}
		playing = !playing;
		if (!playing) {
			sound.silence();
		}
		labels();
	}

	/** One tick's worth of steps, paused. */
	private void step() {
		playing = false;
		labels();
		move(1);
	}

	private void move(int steps) {
		race.advance(steps);
		int perTick = Math.max(1, race.numbers.length / 24);
		for (SortRace.Lane lane : race.lanes) {
			if (lane.finished() && !lane.swept()) {
				lane.sweep = Math.min(lane.values.length, lane.sweep + perTick);
			}
		}
		SortRace.Lane heard = race.lanes[listen];
		if (heard.last != null) {
			sound.play(heard.touched, race.max);
		} else if (heard.sweep > 0 && !heard.swept()) {
			sound.play(heard.values[heard.sweep - 1], race.max);
		} else {
			sound.silence();
		}
	}

	@Override
	boolean tick() {
		if (!playing) {
			return false;
		}
		if (done()) {
			playing = false;
			sound.silence();
			labels();
			return true;
		}
		// Speeds are for 32 numbers; more numbers take more steps a tick, so a race lasts about as long.
		move(Math.max(1, SPEEDS[speed] * race.numbers.length / 32));
		return true;
	}

	@Override
	void hidden() {
		sound.silence();
	}

	@Override
	boolean key(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_RIGHT || Character.toLowerCase(event.getKeyChar()) == 's') {
			step();
			return true;
		}
		if (Character.toLowerCase(event.getKeyChar()) == 'r') {
			restart();
			return true;
		}
		return false;
	}

	@Override
	void click(int x, int y) {
		int lane = laneAt(x, y);
		if (lane >= 0) {
			listen = lane;
			if (!sound.isOn()) {
				sound.toggle();
				labels();
			}
		}
	}

	@Override
	void hover(int x, int y) {
		hovered = laneAt(x, y);
	}

	private Rectangle laneBox(int index, int h) {
		int laneWidth = (LabPanel.WIDTH - 2 * MARGIN - 2 * GAP) / 3;
		int laneHeight = (h - TOP - 6 - GAP) / 2;
		return new Rectangle(MARGIN + index % 3 * (laneWidth + GAP), TOP + index / 3 * (laneHeight + GAP), laneWidth, laneHeight);
	}

	private int laneAt(int x, int y) {
		for (int i = 0; i < race.lanes.length; i++) {
			if (laneBox(i, pageHeight).contains(x, y)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		pageHeight = h;
		text(g, "Sorting race", TITLE, INK, 20, 32);
		String status = race.numbers.length + " numbers, " + Sorts.ORDERS[order].toLowerCase() + "  \u00b7  step " + race.clock;
		text(g, status, BOLD, INK, w - 20 - width(g, status, BOLD), 32);
		paintSummary(g);
		for (int i = 0; i < race.lanes.length; i++) {
			paintLane(g, race.lanes[i], laneBox(i, h));
		}
	}

	private void paintSummary(Graphics2D g) {
		if (!race.finished()) {
			text(g, "Every sort gets the same steps each tick: a compare or a write is one, a swap two."
					+ " Click a sort to hear it.", TEXT, PENCIL, 20, 54);
			return;
		}
		SortRace.Lane first = race.first();
		SortRace.Lane last = race.last();
		String won = Sorts.NAMES[first.algorithm] + " won in " + first.total + " steps.";
		text(g, won, BOLD, GREEN, 20, 54);
		String slow = "  " + Sorts.NAMES[last.algorithm] + " took " + last.total
				+ (first.total > 0 ? ", " + Page.number((double) last.total / first.total, 2) + " times as many." : ".");
		String tip = "  Try another order (O) or more numbers (B).";
		int x = 20 + width(g, won, BOLD);
		text(g, slow + (x + width(g, slow + tip, TEXT) < LabPanel.WIDTH - 16 ? tip : ""), TEXT, INK, x, 54);
	}

	private void paintLane(Graphics2D g, SortRace.Lane lane, Rectangle box) {
		boolean heard = lane.algorithm == listen && sound.isOn();
		g.setColor(lane.algorithm == hovered ? new Color(255, 252, 236) : Color.WHITE);
		g.fillRoundRect(box.x, box.y, box.width, box.height, 12, 12);
		g.setColor(heard ? INK : GRID);
		g.setStroke(new BasicStroke(heard ? 2f : 1f));
		g.drawRoundRect(box.x, box.y, box.width, box.height, 12, 12);
		g.setStroke(new BasicStroke(1f));

		String name = Sorts.NAMES[lane.algorithm];
		text(g, name, BOLD, INK, box.x + 10, box.y + 19);
		text(g, Sorts.GROWTH[lane.algorithm], SMALL, PENCIL, box.x + 16 + width(g, name, BOLD), box.y + 19);
		text(g, lane.compares + " compares, " + lane.writes + " writes", SMALL, PENCIL, box.x + 10, box.y + 35);

		int right = box.x + box.width - 10;
		if (lane.finished()) {
			int place = race.place(lane);
			Color medal = place <= MEDALS.length ? MEDALS[place - 1] : PENCIL;
			String label = place + (place == 1 ? "st" : place == 2 ? "nd" : place == 3 ? "rd" : "th");
			int bw = width(g, label, BOLD) + 14;
			g.setColor(medal);
			g.fillRoundRect(right - bw, box.y + 6, bw, 20, 20, 20);
			text(g, label, BOLD, Color.WHITE, right - bw + 7, box.y + 21);
			right -= bw + 6;
		}
		if (heard) {
			paintSpeaker(g, right - 16, box.y + 9);
		} else if (lane.algorithm == hovered) {
			text(g, "Click to hear", SMALL, PENCIL, right - width(g, "Click to hear", SMALL), box.y + 20);
		}
		paintBars(g, lane, new Rectangle(box.x + 8, box.y + 42, box.width - 16, box.height - 50));
	}

	private void paintBars(Graphics2D g, SortRace.Lane lane, Rectangle area) {
		int n = lane.values.length;
		Sorts.Step last = lane.last;
		for (int i = 0; i < n; i++) {
			int x0 = area.x + i * area.width / n;
			int x1 = area.x + (i + 1) * area.width / n;
			int barHeight = Math.max(1, lane.values[i] * area.height / race.max);
			Color color = BAR;
			if (i < lane.sweep) {
				color = GREEN;
			} else if (last != null && (i == last.a || (last.kind != Sorts.WRITE && i == last.b))) {
				color = last.kind == Sorts.COMPARE ? COMPARE : RED;
			}
			g.setColor(color);
			g.fillRect(x0, area.y + area.height - barHeight, Math.max(1, x1 - x0 - (n <= 64 ? 1 : 0)), barHeight);
		}
	}

	/** A small speaker with two sound waves, for the sort being listened to. */
	private static void paintSpeaker(Graphics2D g, int x, int y) {
		g.setColor(INK);
		g.fillRect(x, y + 4, 4, 6);
		g.fillPolygon(new int[] { x + 4, x + 9, x + 9, x + 4 }, new int[] { y + 4, y, y + 14, y + 10 }, 4);
		g.setStroke(new BasicStroke(1.5f));
		g.drawArc(x + 6, y + 3, 7, 8, -50, 100);
		g.drawArc(x + 6, y, 11, 14, -50, 100);
		g.setStroke(new BasicStroke(1f));
	}
}
