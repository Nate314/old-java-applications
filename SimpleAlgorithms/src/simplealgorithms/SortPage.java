package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

/**
 * Sorting race: six sorts on the same numbers, side by side, each getting the same number of steps per tick. The
 * bars show each compare (orange) and each swap or write (red); a finished sort is checked bar by bar in green and
 * gets its place. Click a sort to listen to it: each tick plays a note pitched by the value it just touched. The
 * Prints view (V) draws each sort's whole run as one picture instead, time going down and each value in its own
 * color, and dragging on a picture scrubs the race back and forth.
 */
class SortPage extends Page {
	private static final int[] SIZES = { 16, 32, 64, 128 };
	private static final int[] SPEEDS = { 1, 3, 10, 40 };
	private static final String[] SPEED_NAMES = { "Slow", "Normal", "Fast", "Turbo" };
	private static final Color BAR = new Color(120, 146, 190);
	private static final Color COMPARE = new Color(232, 146, 40);
	private static final Color[] MEDALS = { new Color(214, 170, 30), new Color(150, 160, 175), new Color(190, 120, 70) };
	/** Where the lanes start: narrow, under the status, two lines of summary and three about the followed sort. */
	private static final int TOP = NARROW ? 160 : 82;
	/** Lanes across: three when wide, two in three rows when narrow. */
	private static final int COLUMNS = NARROW ? 2 : 3;
	private static final int MARGIN = 16;
	private static final int GAP = 8;
	/** The Bars | Prints switch beside the title: one half per view. */
	private static final Rectangle[] VIEWS = { new Rectangle(140, 16, 52, 22), new Rectangle(192, 16, 58, 22) };
	/** The colors a value runs through from smallest to largest in the Prints view. */
	private static final Color[] RAMP = { new Color(52, 78, 160), new Color(38, 160, 170), new Color(150, 200, 80),
			new Color(245, 180, 50), new Color(210, 70, 60) };

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
	/** Bars, or each sort's run as one picture; and the lane being scrubbed by a drag (-1 for none). */
	private boolean prints;
	private int scrubbing = -1;
	/** The color of each value for the Prints view, made for the race's largest value. */
	private Color[] colors = new Color[0];
	/** Each lane's print drawn so far, for which race, and how many rows it holds. */
	private final BufferedImage[] printImages = new BufferedImage[Sorts.NAMES.length];
	private final int[] printRows = new int[Sorts.NAMES.length];
	private SortRace printRace;
	/** The page's height at the last paint, so clicks find the same lanes. */
	private int pageHeight = LabPanel.HEIGHT - (NARROW ? 168 : 90);

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
		playHeard();
	}

	/** The note of the value the listened-to sort just touched, or of its check; silence when it has neither. */
	private void playHeard() {
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
	boolean drags() {
		return prints;
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
		if (Character.toLowerCase(event.getKeyChar()) == 'v') {
			prints = !prints;
			return true;
		}
		if (event.getKeyCode() == KeyEvent.VK_LEFT) {
			seek(race.clock - 1);
			return true;
		}
		if (Character.toLowerCase(event.getKeyChar()) == 'r') {
			restart();
			return true;
		}
		return false;
	}

	/** Moves the race to any time, paused, and plays the note there. */
	private void seek(int time) {
		playing = false;
		race.seek(time);
		labels();
		playHeard();
	}

	@Override
	void click(int x, int y) {
		for (int view = 0; view < VIEWS.length; view++) {
			if (VIEWS[view].contains(x, y)) {
				prints = view == 1;
			}
		}
		int lane = laneAt(x, y);
		if (lane >= 0) {
			listen = lane;
			if (!sound.isOn()) {
				sound.toggle();
				labels();
			}
			if (prints && area(laneBox(lane, pageHeight)).contains(x, y)) {
				scrubbing = lane;
				drag(x, y);
			}
		}
	}

	@Override
	void drag(int x, int y) {
		if (scrubbing >= 0) {
			Rectangle area = area(laneBox(scrubbing, pageHeight));
			int row = Math.max(0, Math.min(SortRace.ROWS - 1, (y - area.y) * SortRace.ROWS / area.height));
			seek(race.timeOf(row));
		}
	}

	@Override
	void release() {
		scrubbing = -1;
	}

	@Override
	void hover(int x, int y) {
		hovered = laneAt(x, y);
	}

	private Rectangle laneBox(int index, int h) {
		int rows = Sorts.NAMES.length / COLUMNS;
		int laneWidth = (LabPanel.WIDTH - 2 * MARGIN - (COLUMNS - 1) * GAP) / COLUMNS;
		int laneHeight = (h - TOP - 6 - (rows - 1) * GAP) / rows;
		return new Rectangle(MARGIN + index % COLUMNS * (laneWidth + GAP), TOP + index / COLUMNS * (laneHeight + GAP), laneWidth, laneHeight);
	}

	/** Where a lane draws its bars or print. */
	private static Rectangle area(Rectangle box) {
		return new Rectangle(box.x + 8, box.y + 42, box.width - 16, box.height - 50);
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
		paintViews(g);
		String status = race.numbers.length + " numbers, " + Sorts.ORDERS[order].toLowerCase() + "  \u00b7  step " + race.clock;
		text(g, status, BOLD, INK, NARROW ? 20 : w - 20 - width(g, status, BOLD), NARROW ? 58 : 32);
		paintSummary(g);
		paintFollowed(g);
		for (int i = 0; i < race.lanes.length; i++) {
			paintLane(g, race.lanes[i], laneBox(i, h));
		}
	}

	private void paintViews(Graphics2D g) {
		String[] names = { "Bars", "Prints" };
		for (int view = 0; view < VIEWS.length; view++) {
			Rectangle r = VIEWS[view];
			boolean on = prints == (view == 1);
			g.setColor(on ? INK : Color.WHITE);
			g.fillRoundRect(r.x, r.y, r.width, r.height, 22, 22);
			centered(g, names[view], BOLD, on ? Color.WHITE : INK, r.x + r.width / 2, r.y + 16);
		}
		Rectangle all = new Rectangle(VIEWS[0].x, VIEWS[0].y, VIEWS[1].x + VIEWS[1].width - VIEWS[0].x, VIEWS[0].height);
		g.setColor(INK);
		g.setStroke(new BasicStroke(1.5f));
		g.drawRoundRect(all.x, all.y, all.width, all.height, 22, 22);
		g.setStroke(new BasicStroke(1f));
		if (!NARROW) {
			text(g, "V", SMALL, PENCIL, all.x + all.width + 6, all.y + 15);
		}
	}

	private void paintSummary(Graphics2D g) {
		if (NARROW) {
			paintNarrowSummary(g);
			return;
		}
		if (!race.finished() && prints) {
			text(g, "Each run as one picture: time goes down and each number keeps its color."
					+ " Drag on one to go back and forth.", TEXT, PENCIL, 20, 54);
			return;
		}
		if (!race.finished()) {
			text(g, "Every sort gets the same steps each tick: a compare or a write is one, a swap two."
					+ " Click a sort to follow it.", TEXT, PENCIL, 20, 54);
			return;
		}
		SortRace.Lane first = race.first();
		SortRace.Lane last = race.last();
		String won = Sorts.NAMES[first.algorithm] + " won in " + first.total + " steps.";
		text(g, won, BOLD, GREEN, 20, 54);
		String slow = "  " + Sorts.NAMES[last.algorithm] + " took " + last.total
				+ (first.total > 0 ? ", " + Page.number((double) last.total / first.total, 2) + " times as many." : ".");
		String tip = prints ? "  Try another order (O) or more numbers (B)." : "  See each run as a picture (V).";
		int x = 20 + width(g, won, BOLD);
		text(g, slow + (x + width(g, slow + tip, TEXT) < LabPanel.WIDTH - 16 ? tip : ""), TEXT, INK, x, 54);
	}

	/** The summary in up to two wrapped lines, for a narrow lab. */
	private void paintNarrowSummary(Graphics2D g) {
		int width = LabPanel.WIDTH - 40;
		if (!race.finished()) {
			String says = prints ? "Each run as one picture: time goes down and each number keeps its color. Drag on one to go back and forth."
					: "Every sort gets the same steps each tick: a compare or a write is one, a swap two. Tap a sort to follow it.";
			paragraph(g, says, TEXT, PENCIL, 20, 80, width, 0, 16, 2);
			return;
		}
		SortRace.Lane first = race.first();
		SortRace.Lane last = race.last();
		String won = Sorts.NAMES[first.algorithm] + " won in " + first.total + " steps.";
		text(g, won, BOLD, GREEN, 20, 80);
		String slow = " " + Sorts.NAMES[last.algorithm] + " took " + last.total
				+ (first.total > 0 ? ", " + Page.number((double) last.total / first.total, 2) + " times as many." : ".");
		paragraph(g, slow, TEXT, INK, 20, 80, width, width(g, won, BOLD), 16, 2);
	}

	/**
	 * The line about the sort being followed (the one clicked last): while it runs, how it works; paused, what its
	 * last step did and why, so stepping (S, Left) reads as the sort's own reasoning.
	 */
	private void paintFollowed(Graphics2D g) {
		SortRace.Lane lane = race.lanes[listen];
		String name = Sorts.NAMES[lane.algorithm] + ": ";
		String says = !playing && lane.last != null ? lane.last.describe(lane.values) : Sorts.METHODS[lane.algorithm];
		if (NARROW) {
			text(g, name, BOLD, INK, 20, 118);
			String tip = playing && !race.finished() ? " Pause and step to see why." : "";
			paragraph(g, says + tip, TEXT, INK, 20, 118, LabPanel.WIDTH - 40, width(g, name, BOLD), 16, 3);
			return;
		}
		text(g, name, BOLD, INK, 20, 72);
		int x = 20 + width(g, name, BOLD);
		String tip = playing && !race.finished() ? "  Pause and step (S) to see why." : "";
		text(g, says + (x + width(g, says + tip, TEXT) < LabPanel.WIDTH - 16 ? tip : ""), TEXT, INK, x, 72);
	}

	private void paintLane(Graphics2D g, SortRace.Lane lane, Rectangle box) {
		boolean followed = lane.algorithm == listen;
		boolean heard = followed && sound.isOn();
		g.setColor(lane.algorithm == hovered ? new Color(255, 252, 236) : Color.WHITE);
		g.fillRoundRect(box.x, box.y, box.width, box.height, 12, 12);
		g.setColor(followed ? INK : GRID);
		g.setStroke(new BasicStroke(followed ? 2f : 1f));
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
			text(g, "Click to follow", SMALL, PENCIL, right - width(g, "Click to follow", SMALL), box.y + 20);
		}
		if (prints) {
			paintPrint(g, lane, area(box));
		} else {
			paintBars(g, lane, area(box));
		}
	}

	/**
	 * The lane's print up to the furthest the race has been, with a line at the clock. The print is kept as an
	 * image at twice the size (sharp on a 2x screen) and only the rows the race has newly reached are added to it, as
	 * redrawing thousands of cells every tick is slow in the browser.
	 */
	private void paintPrint(Graphics2D g, SortRace.Lane lane, Rectangle area) {
		if (printRace != race) {
			printRace = race;
			Arrays.fill(printImages, null);
		}
		int index = lane.algorithm;
		BufferedImage image = printImages[index];
		if (image == null || image.getWidth() != 2 * area.width || image.getHeight() != 2 * area.height) {
			image = new BufferedImage(2 * area.width, 2 * area.height, BufferedImage.TYPE_INT_RGB);
			printImages[index] = image;
			printRows[index] = 0;
			Graphics2D blank = image.createGraphics();
			blank.setColor(Color.WHITE);
			blank.fillRect(0, 0, image.getWidth(), image.getHeight());
			blank.dispose();
		}
		int shown = race.rowAt(race.reached) + 1;
		if (printRows[index] < shown) {
			Graphics2D cells = image.createGraphics();
			paintRows(cells, lane, new Rectangle(0, 0, image.getWidth(), image.getHeight()), printRows[index], shown);
			cells.dispose();
			printRows[index] = shown;
		}
		g.drawImage(image, area.x, area.y, area.width, area.height, null);
		g.setColor(GRID);
		g.drawRect(area.x, area.y, area.width - 1, area.height - 1);
		int y = rowY(area, race.rowAt(race.clock) + 1) - 1;
		g.setColor(INK);
		g.fillRect(area.x - 4, y, area.width + 8, 2);
	}

	/** Rows from to to of the print: a column per place, each cell in its value's color, as runs of equal cells. */
	private void paintRows(Graphics2D g, SortRace.Lane lane, Rectangle area, int from, int to) {
		if (colors.length != race.max + 1) {
			colors = new Color[race.max + 1];
			for (int value = 0; value <= race.max; value++) {
				colors[value] = ramp((double) value / race.max);
			}
		}
		int n = lane.values.length;
		for (int i = 0; i < n; i++) {
			int x0 = area.x + i * area.width / n;
			int x1 = area.x + (i + 1) * area.width / n;
			for (int row = from; row < to;) {
				int value = lane.print[row][i];
				int end = row + 1;
				while (end < to && lane.print[end][i] == value) {
					end++;
				}
				g.setColor(colors[value]);
				g.fillRect(x0, rowY(area, row), x1 - x0, rowY(area, end) - rowY(area, row));
				row = end;
			}
		}
	}

	private static int rowY(Rectangle area, int row) {
		return area.y + row * area.height / SortRace.ROWS;
	}

	/** A color along {@link #RAMP}, for t from 0 to 1. */
	private static Color ramp(double t) {
		double at = Math.max(0, Math.min(1, t)) * (RAMP.length - 1);
		int i = Math.min(RAMP.length - 2, (int) at);
		double f = at - i;
		Color a = RAMP[i];
		Color b = RAMP[i + 1];
		return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
				(int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
				(int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
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
		if (last != null && !playing && lane.algorithm == listen) {
			paintTags(g, lane, area, last.a, last.kind == Sorts.WRITE ? last.a : last.b);
		}
	}

	/**
	 * Paused on the followed sort, the bars its line names carry their numbers on white tags, side by side on one
	 * line just above the taller bar (inside the lane), pushed apart when the bars are close.
	 */
	private void paintTags(Graphics2D g, SortRace.Lane lane, Rectangle area, int a, int b) {
		int left = Math.min(a, b);
		int right = Math.max(a, b);
		int n = lane.values.length;
		int tallest = Math.max(lane.values[left], lane.values[right]);
		int y = Math.max(area.y + 12, area.y + area.height - Math.max(1, tallest * area.height / race.max) - 4);
		String leftText = String.valueOf(lane.values[left]);
		String rightText = String.valueOf(lane.values[right]);
		int leftW = width(g, leftText, BOLD) + 6;
		int rightW = width(g, rightText, BOLD) + 6;
		int leftX = barCenter(area, n, left) - leftW / 2;
		int rightX = barCenter(area, n, right) - rightW / 2;
		if (left != right && leftX + leftW + 2 > rightX) {
			int middle = (barCenter(area, n, left) + barCenter(area, n, right)) / 2;
			leftX = middle - 1 - leftW;
			rightX = middle + 1;
		}
		// Keep both inside the lane, moving them together.
		int shift = Math.max(0, area.x - leftX) - Math.max(0, rightX + rightW - area.x - area.width);
		tag(g, leftText, leftX + shift, leftW, y);
		if (left != right) {
			tag(g, rightText, rightX + shift, rightW, y);
		}
	}

	private static int barCenter(Rectangle area, int n, int i) {
		return area.x + (2 * i + 1) * area.width / (2 * n);
	}

	private static void tag(Graphics2D g, String number, int x, int w, int y) {
		g.setColor(Color.WHITE);
		g.fillRoundRect(x, y - 11, w, 14, 6, 6);
		g.setColor(GRID);
		g.drawRoundRect(x, y - 11, w, 14, 6, 6);
		text(g, number, BOLD, INK, x + 3, y);
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
