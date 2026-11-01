package numberguess;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * The game board: tabs for the five ranges and You pick along the top, the verdict, and two number lines. The top
 * line is the whole range with what is still possible lit; the bottom one zooms in on just that part, so a click on
 * it guesses the number under the pointer, even in 1 to 1,000,000. The guesses so far sit underneath as chips.
 */
@SuppressWarnings("serial")
public final class Board extends JPanel {
	public static final int WIDTH = 600, HEIGHT = 350;

	private static final Color BACKGROUND = new Color(22, 25, 36);
	private static final Color CARD = new Color(36, 40, 56);
	private static final Color CARD_HOVER = new Color(50, 55, 76);
	private static final Color SELECTED = new Color(60, 70, 110);
	private static final Color TEXT = new Color(236, 238, 245);
	private static final Color SOFT = new Color(152, 158, 180);
	private static final Color DIM = new Color(86, 92, 112);
	private static final Color RULED_OUT = new Color(48, 52, 68);
	private static final Color POSSIBLE = new Color(72, 196, 160);
	private static final Color POSSIBLE_DARK = new Color(40, 120, 100);
	private static final Color LOW = new Color(90, 150, 255);
	private static final Color HIGH = new Color(240, 100, 96);
	private static final Color GOLD = new Color(255, 198, 64);
	private static final Color WARN = new Color(255, 170, 70);
	private static final Color RIGHT = new Color(80, 205, 120);

	private static final int MARGIN = 20;
	private static final int TABS_Y = 10, TAB_HEIGHT = 44;
	private static final int FULL_Y = 150, BAR_HEIGHT = 12;
	private static final int ZOOM_Y = 198, ZOOM_HEIGHT = 36;
	private static final int CHIPS_Y = 264;

	private final Records records;
	private final Random random = new Random();
	private final Runnable modeChanged;
	private Level level;
	private Game game;
	private MindReader reader;
	private String headline = "";
	private String subline = "";
	private Color headlineColor = TEXT;
	private boolean newBest;
	private Level opened;

	// The lit part of the top line slides to its new size instead of jumping.
	private double shownLow, shownHigh;
	private final Timer slide = new Timer(15, event -> step());

	// Where things were last drawn, for the mouse.
	private final List<Rectangle> tabs = new ArrayList<>();
	private Rectangle zoom = new Rectangle();
	private int hoverTab = -1;
	/** Where the pointer is over the zoomed line, or -1. */
	private int hoverX = -1;

	/** {@code modeChanged} runs when the board switches between guessing and You pick, so the buttons can follow. */
	public Board(Records records, Runnable modeChanged) {
		this.records = records;
		this.modeChanged = modeChanged;
		setBackground(BACKGROUND);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setFocusable(true);
		MouseAdapter mouse = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent event) {
				click(event.getX(), event.getY());
			}

			@Override
			public void mouseMoved(MouseEvent event) {
				hover(event.getX(), event.getY());
			}

			@Override
			public void mouseExited(MouseEvent event) {
				hoverTab = -1;
				hoverX = -1;
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		start(records.suggested());
	}

	// ---- state ----

	public boolean isPicking() {
		return reader != null;
	}

	public boolean isOver() {
		return isPicking() ? reader.isOver() : game.isOver();
	}

	public Game game() {
		return game;
	}

	/** A new number to guess in {@code level}. */
	public void start(Level level) {
		boolean wasPicking = isPicking();
		this.level = level;
		reader = null;
		game = new Game(level, random);
		newBest = false;
		opened = null;
		say("Guess a number from 1 to " + Level.commas(level.max()), TEXT,
				"Par " + level.par() + ": guess the middle of what is left. " + level.limit() + " guesses before it is given away.");
		reset();
		if (wasPicking) {
			modeChanged.run();
		}
	}

	/** You think of a number and the computer guesses it. */
	public void startPicking() {
		boolean wasPicking = isPicking();
		reader = new MindReader();
		game = null;
		askReader();
		reset();
		if (!wasPicking) {
			modeChanged.run();
		}
	}

	/** The same game again: a new number in this range, or a new round of You pick. */
	public void again() {
		if (isPicking()) {
			startPicking();
		} else {
			start(opened != null ? opened : level);
		}
	}

	/** Page Up / Page Down: the tab before or after this one. */
	public void moveTab(int by) {
		int index = (isPicking() ? Level.values().length : level.ordinal()) + by;
		if (index >= 0 && index <= Level.values().length) {
			openTab(index);
		}
	}

	private void reset() {
		shownLow = low();
		shownHigh = high();
		slide.stop();
		repaint();
	}

	private int max() {
		return isPicking() ? MindReader.MAX : level.max();
	}

	private int low() {
		return isPicking() ? Math.min(reader.low(), MindReader.MAX) : game.low();
	}

	private int high() {
		return isPicking() ? Math.max(reader.high(), 1) : game.high();
	}

	private List<Integer> guesses() {
		return isPicking() ? reader.guesses() : game.guesses();
	}

	private void say(String headline, Color color, String subline) {
		this.headline = headline;
		this.headlineColor = color;
		this.subline = subline;
	}

	/** Shows a short message in place of the subline, for a guess that could not be used. */
	public void notice(String text) {
		subline = text;
		repaint();
	}

	/** A typed or clicked guess. */
	public void guess(int number) {
		if (isPicking()) {
			return;
		}
		if (game.isOver()) {
			again();
			return;
		}
		String range = "It is from " + Level.commas(game.low()) + " to " + Level.commas(game.high()) + ".";
		switch (game.guess(number)) {
		case OUT_OF_RANGE:
			say("Out of range", WARN, "Pick a number from 1 to " + Level.commas(level.max()) + ". Not counted.");
			break;
		case RULED_OUT:
			say(Level.commas(number) + " is already ruled out", WARN, range + " Not counted.");
			break;
		case TOO_LOW:
		case TOO_HIGH:
			boolean low = number < game.number();
			if (game.isOver()) {
				say("Out of guesses. It was " + Level.commas(game.number()), HIGH, "Press Enter or click the line for a new number.");
			} else {
				say(Level.commas(number) + (low ? " is too low" : " is too high"), low ? LOW : HIGH,
						"It is from " + Level.commas(game.low()) + " to " + Level.commas(game.high()) + ". "
								+ guessesLeft(game.guessesLeft()) + ".");
			}
			break;
		case CORRECT:
			win();
			break;
		default:
			break;
		}
		slide.start();
		repaint();
	}

	private static String guessesLeft(int left) {
		return left + (left == 1 ? " guess left" : " guesses left");
	}

	private void win() {
		Level next = level.ordinal() + 1 < Level.values().length ? Level.values()[level.ordinal() + 1] : null;
		boolean wasOpen = next != null && records.isOpen(next);
		newBest = records.add(game);
		opened = next != null && !wasOpen && records.isOpen(next) ? next : null;
		int guesses = game.getGuesses();
		String more = opened != null ? " " + opened.label() + " is open: Enter plays it."
				: game.stars() < 3 ? " Par is " + level.par() + "." : " Press Enter for another.";
		say("Got it: " + Level.commas(game.number()) + " in " + guesses + (guesses == 1 ? " guess" : " guesses"), RIGHT,
				(newBest ? "New best! " : "") + more.trim());
	}

	// ---- You pick ----

	private void askReader() {
		if (reader.isFound()) {
			int guesses = reader.guesses().size();
			say("Your number is " + reader.guess() + "!", RIGHT, "Found in " + guesses + (guesses == 1 ? " guess" : " guesses")
					+ ". Guessing the middle never needs more than " + MindReader.par() + ".");
		} else if (reader.isCaught()) {
			say("Wait, that can't be!", HIGH, caughtReason() + " Did your number change?");
		} else {
			int number = reader.guesses().size();
			say("Is it " + reader.guess() + "?", TEXT, number == 1
					? "Think of a number from 1 to " + MindReader.MAX + ". Is yours lower, higher, or this one?"
					: "Guess " + number + ". It is from " + reader.low() + " to " + reader.high() + ", if you told the truth.");
		}
	}

	private String caughtReason() {
		if (reader.high() < 1) {
			return "You said lower than 1.";
		}
		if (reader.low() > MindReader.MAX) {
			return "You said higher than " + MindReader.MAX + ".";
		}
		return "You said higher than " + (reader.low() - 1) + " and lower than " + (reader.high() + 1) + ".";
	}

	public void higher() {
		answer(1);
	}

	public void lower() {
		answer(-1);
	}

	public void correct() {
		answer(0);
	}

	private void answer(int direction) {
		if (!isPicking()) {
			return;
		}
		if (reader.isOver()) {
			startPicking();
			return;
		}
		if (direction > 0) {
			reader.higher();
		} else if (direction < 0) {
			reader.lower();
		} else {
			reader.correct();
		}
		askReader();
		slide.start();
		repaint();
	}

	// ---- mouse ----

	private void openTab(int index) {
		if (index == Level.values().length) {
			startPicking();
			return;
		}
		Level target = Level.values()[index];
		if (records.isOpen(target)) {
			start(target);
		} else {
			notice("Win " + Level.values()[index - 1].label() + " with " + Records.STARS_TO_OPEN + " stars (par + 2 or better) to open "
					+ target.label() + ".");
		}
	}

	private void click(int x, int y) {
		requestFocusInWindow();
		layOut();
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(x, y)) {
				openTab(i);
				return;
			}
		}
		if (zoom.contains(x, y) && !isPicking()) {
			guess(numberAt(x));
		}
	}

	private void hover(int x, int y) {
		layOut();
		int tab = -1;
		for (int i = 0; i < tabs.size(); i++) {
			if (tabs.get(i).contains(x, y)) {
				tab = i;
			}
		}
		int over = zoom.contains(x, y) ? x : -1;
		if (tab != hoverTab || over != hoverX) {
			hoverTab = tab;
			hoverX = over;
			setCursor(Cursor.getPredefinedCursor(tab >= 0 || hoverNumber() > 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			repaint();
		}
	}

	/** The number a click on the zoomed line would guess now, or 0. */
	private int hoverNumber() {
		return hoverX >= 0 && !isPicking() && !isOver() ? numberAt(hoverX) : 0;
	}

	/** The still possible number under {@code x} on the zoomed line. */
	private int numberAt(int x) {
		long count = (long) high() - low() + 1;
		long offset = (long) (x - zoom.x) * count / zoom.width;
		return (int) Math.max(low(), Math.min(high(), low() + offset));
	}

	// ---- drawing ----

	private void step() {
		double low = low(), high = high();
		shownLow += (low - shownLow) * 0.22;
		shownHigh += (high - shownHigh) * 0.22;
		if (Math.abs(shownLow - low) < 0.01 * max() / getWidth() + 0.01 && Math.abs(shownHigh - high) < 0.01 * max() / getWidth() + 0.01) {
			shownLow = low;
			shownHigh = high;
			slide.stop();
		}
		repaint();
	}

	private void layOut() {
		int width = getWidth();
		int count = Level.values().length + 1;
		int gap = 6;
		int tabWidth = (width - 2 * MARGIN - gap * (count - 1)) / count;
		tabs.clear();
		for (int i = 0; i < count; i++) {
			tabs.add(new Rectangle(MARGIN + i * (tabWidth + gap), TABS_Y, tabWidth, TAB_HEIGHT));
		}
		zoom = new Rectangle(MARGIN, ZOOM_Y, width - 2 * MARGIN, ZOOM_HEIGHT);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		layOut();
		g.setColor(BACKGROUND);
		g.fillRect(0, 0, getWidth(), getHeight());
		paintTabs(g);
		g.setColor(headlineColor);
		g.setFont(new Font("SansSerif", Font.BOLD, 22));
		g.drawString(headline, MARGIN, 86);
		g.setColor(SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 13));
		g.drawString(subline, MARGIN, 108);
		paintGuessMeter(g);
		paintFullLine(g);
		paintZoom(g);
		paintChips(g);
	}

	private void paintTabs(Graphics2D g) {
		for (int i = 0; i < tabs.size(); i++) {
			Rectangle tab = tabs.get(i);
			boolean picking = i == Level.values().length;
			Level tabLevel = picking ? null : Level.values()[i];
			boolean selected = picking ? isPicking() : !isPicking() && tabLevel == level;
			boolean open = picking || records.isOpen(tabLevel);
			g.setColor(selected ? SELECTED : i == hoverTab ? CARD_HOVER : CARD);
			g.fillRoundRect(tab.x, tab.y, tab.width, tab.height, 10, 10);
			if (selected) {
				g.setColor(POSSIBLE);
				g.fillRoundRect(tab.x + 8, tab.y + tab.height - 3, tab.width - 16, 3, 3, 3);
			}
			g.setFont(new Font("SansSerif", Font.BOLD, 13));
			g.setColor(open ? TEXT : DIM);
			String label = picking ? "You pick" : tabLevel.label();
			FontMetrics metrics = g.getFontMetrics();
			g.drawString(label, tab.x + (tab.width - metrics.stringWidth(label)) / 2, tab.y + 19);
			int centerX = tab.x + tab.width / 2;
			if (picking) {
				g.setFont(new Font("SansSerif", Font.PLAIN, 11));
				g.setColor(SOFT);
				String sub = "I guess yours";
				g.drawString(sub, centerX - g.getFontMetrics().stringWidth(sub) / 2, tab.y + 35);
			} else if (!open) {
				paintLock(g, centerX, tab.y + 31);
			} else {
				int stars = records.stars(tabLevel);
				for (int s = 0; s < 3; s++) {
					paintStar(g, centerX - 16 + s * 16, tab.y + 31, 6, s < stars ? GOLD : DIM);
				}
			}
		}
	}

	/** Dots for the guesses allowed: used ones filled, par marked by a gap. */
	private void paintGuessMeter(Graphics2D g) {
		if (isPicking()) {
			return;
		}
		int limit = level.limit();
		int used = game.getGuesses();
		int size = 9, gap = 4;
		int x = MARGIN;
		int y = 117;
		for (int i = 0; i < limit; i++) {
			int dotX = x + i * (size + gap) + (i >= level.par() ? 6 : 0);
			boolean usedDot = i < used;
			boolean par = i < level.par();
			g.setColor(usedDot ? (game.isFound() && i == used - 1 ? RIGHT : par ? SOFT : WARN) : par ? RULED_OUT : CARD);
			g.fillOval(dotX, y, size, size);
			if (!usedDot) {
				g.setColor(par ? DIM : WARN.darker().darker());
				g.drawOval(dotX, y, size, size);
			}
		}
	}

	private int xOn(Rectangle line, double value, double from, double to) {
		if (to <= from) {
			return line.x + line.width / 2;
		}
		return (int) Math.round(line.x + (value - from) / (to - from) * line.width);
	}

	private void paintFullLine(Graphics2D g) {
		Rectangle line = new Rectangle(MARGIN, FULL_Y, getWidth() - 2 * MARGIN, BAR_HEIGHT);
		int max = max();
		g.setColor(RULED_OUT);
		g.fillRoundRect(line.x, line.y, line.width, line.height, BAR_HEIGHT, BAR_HEIGHT);
		// Each number owns a slice of the line: number n covers n - 1 to n.
		int left = xOn(line, shownLow - 1, 0, max);
		int right = Math.max(left + 3, xOn(line, shownHigh, 0, max));
		boolean caught = isPicking() && reader.isCaught();
		if (!caught) {
			g.setColor(isOver() ? RIGHT : POSSIBLE);
			g.fillRoundRect(left, line.y, right - left, line.height, 6, 6);
		}
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(SOFT);
		g.drawString("1", line.x + 4, line.y + line.height + 14);
		String end = Level.commas(max);
		g.drawString(end, line.x + line.width - 4 - g.getFontMetrics().stringWidth(end), line.y + line.height + 14);
		// A marker over the line for each guess, the latest one labelled.
		List<Integer> guesses = guesses();
		for (int i = 0; i < guesses.size(); i++) {
			int guess = guesses.get(i);
			int x = xOn(line, guess - 0.5, 0, max);
			g.setColor(chipColor(i));
			g.fillPolygon(new int[] { x - 4, x + 4, x }, new int[] { line.y - 8, line.y - 8, line.y - 1 }, 3);
			if (i == guesses.size() - 1) {
				String label = Level.commas(guess);
				int width = g.getFontMetrics().stringWidth(label);
				int labelX = Math.max(line.x, Math.min(line.x + line.width - width, x - width / 2));
				g.drawString(label, labelX, line.y - 11);
			}
		}
		// The magnifier: the lit part of the top line opens out into the zoomed line below.
		if (!caught) {
			g.setColor(POSSIBLE_DARK);
			g.drawLine(left, line.y + line.height + 1, zoom.x, zoom.y - 2);
			g.drawLine(right, line.y + line.height + 1, zoom.x + zoom.width, zoom.y - 2);
		}
	}

	private void paintZoom(Graphics2D g) {
		Rectangle line = zoom;
		g.setColor(CARD);
		g.fillRoundRect(line.x, line.y, line.width, line.height, 8, 8);
		if (isPicking() && reader.isCaught()) {
			g.setColor(HIGH);
			g.setFont(new Font("SansSerif", Font.BOLD, 14));
			String text = "No number fits those answers";
			g.drawString(text, line.x + (line.width - g.getFontMetrics().stringWidth(text)) / 2, line.y + 23);
			paintZoomEnds(g, "", "");
			return;
		}
		int low = low(), high = high();
		long count = (long) high - low + 1;
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		if (count <= line.width / 4) {
			// Few enough to draw one cell per number.
			boolean labels = metrics.stringWidth(Level.commas(high)) + 4 <= line.width / count;
			for (long i = 0; i < count; i++) {
				int number = (int) (low + i);
				int x0 = xOn(line, i, 0, count), x1 = xOn(line, i + 1, 0, count);
				g.setColor(cellColor(number));
				g.fillRoundRect(x0 + 1, line.y + 3, x1 - x0 - 2, line.height - 6, 6, 6);
				if (labels) {
					String label = Level.commas(number);
					g.setColor(number == hoverNumber() || isOver() ? BACKGROUND : TEXT);
					g.drawString(label, (x0 + x1 - metrics.stringWidth(label)) / 2, line.y + 23);
				}
			}
		} else {
			g.setColor(POSSIBLE_DARK);
			g.fillRoundRect(line.x + 1, line.y + 3, line.width - 2, line.height - 6, 6, 6);
			// Ticks every tenth of what is left.
			g.setColor(POSSIBLE);
			for (int i = 1; i < 10; i++) {
				int x = line.x + line.width * i / 10;
				g.fillRect(x, line.y + (i == 5 ? 6 : 12), 1, line.height - (i == 5 ? 12 : 24));
			}
			if (hoverNumber() > 0) {
				int x0 = xOn(line, hoverNumber() - low, 0, count), x1 = xOn(line, hoverNumber() - low + 1, 0, count);
				g.setColor(GOLD);
				g.fillRect(x0, line.y + 3, Math.max(2, x1 - x0), line.height - 6);
			}
		}
		if (isPicking() && !reader.isOver()) {
			int guess = reader.guess();
			int x = xOn(line, guess - low + 0.5, 0, count);
			g.setColor(GOLD);
			g.fillPolygon(new int[] { x - 6, x + 6, x }, new int[] { line.y - 9, line.y - 9, line.y }, 3);
		}
		paintZoomEnds(g, low == high ? "" : Level.commas(low), low == high ? "" : Level.commas(high));
		if (hoverNumber() > 0) {
			paintBubble(g, hoverNumber(), xOn(line, hoverNumber() - low + 0.5, 0, count));
		}
	}

	private Color cellColor(int number) {
		if (isOver()) {
			return RIGHT;
		}
		if (number == hoverNumber()) {
			return GOLD;
		}
		if (isPicking() && number == reader.guess()) {
			return SELECTED;
		}
		return POSSIBLE_DARK;
	}

	private void paintZoomEnds(Graphics2D g, String low, String high) {
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(SOFT);
		int y = zoom.y + zoom.height + 14;
		g.drawString(low, zoom.x, y);
		g.drawString(high, zoom.x + zoom.width - g.getFontMetrics().stringWidth(high), y);
		String middle = isOver() ? "" : isPicking() ? "What is still possible" : "Click to guess";
		g.drawString(middle, zoom.x + (zoom.width - g.getFontMetrics().stringWidth(middle)) / 2, y);
	}

	/** The number under the pointer, in a bubble above the zoomed line. */
	private void paintBubble(Graphics2D g, int number, int x) {
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		String text = Level.commas(number);
		int width = g.getFontMetrics().stringWidth(text) + 16;
		int left = Math.max(zoom.x, Math.min(zoom.x + zoom.width - width, x - width / 2));
		int top = zoom.y - 30;
		g.setColor(GOLD);
		g.fillRoundRect(left, top, width, 24, 10, 10);
		g.fillPolygon(new int[] { x - 5, x + 5, x }, new int[] { top + 23, top + 23, top + 29 }, 3);
		g.setColor(BACKGROUND);
		g.drawString(text, left + 8, top + 17);
	}

	/** The colour of guess {@code index}: blue when too low, red when too high, green when right. */
	private Color chipColor(int index) {
		List<Integer> guesses = guesses();
		int guess = guesses.get(index);
		if (!isPicking()) {
			return guess < game.number() ? LOW : guess > game.number() ? HIGH : RIGHT;
		}
		if (index + 1 < guesses.size()) {
			return guesses.get(index + 1) > guess ? LOW : HIGH;
		}
		return reader.isFound() ? RIGHT : reader.isCaught() ? WARN : GOLD;
	}

	private void paintChips(Graphics2D g) {
		List<Integer> guesses = guesses();
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		int x = MARGIN, y = CHIPS_Y, height = 22;
		int right = getWidth() - MARGIN;
		if (isOver() && !(isPicking() && reader.isCaught())) {
			int stars = isPicking() ? 0 : game.stars();
			if (!isPicking()) {
				for (int s = 0; s < 3; s++) {
					paintStar(g, right - 72 + s * 26, y + 26, 11, s < stars ? GOLD : DIM);
				}
			}
		}
		for (int i = 0; i < guesses.size(); i++) {
			String label = Level.commas(guesses.get(i));
			int width = metrics.stringWidth(label) + 26;
			if (x + width > right - 90) {
				x = MARGIN;
				y += height + 6;
				if (y + height > getHeight()) {
					break;
				}
			}
			Color color = chipColor(i);
			g.setColor(CARD);
			g.fillRoundRect(x, y, width, height, 10, 10);
			g.setColor(color);
			g.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
			g.drawString(label, x + 8, y + 16);
			int arrowX = x + width - 11;
			if (color == LOW) {
				g.fillPolygon(new int[] { arrowX - 4, arrowX + 4, arrowX }, new int[] { y + 14, y + 14, y + 8 }, 3);
			} else if (color == HIGH) {
				g.fillPolygon(new int[] { arrowX - 4, arrowX + 4, arrowX }, new int[] { y + 8, y + 8, y + 14 }, 3);
			} else {
				g.fillOval(arrowX - 3, y + 8, 7, 7);
			}
			x += width + 6;
		}
	}

	private static void paintStar(Graphics2D g, int centerX, int centerY, int radius, Color color) {
		int[] xs = new int[10], ys = new int[10];
		for (int i = 0; i < 10; i++) {
			double angle = -Math.PI / 2 + i * Math.PI / 5;
			double r = i % 2 == 0 ? radius : radius * 0.45;
			xs[i] = (int) Math.round(centerX + r * Math.cos(angle));
			ys[i] = (int) Math.round(centerY + r * Math.sin(angle));
		}
		g.setColor(color);
		g.fillPolygon(xs, ys, 10);
	}

	private static void paintLock(Graphics2D g, int centerX, int centerY) {
		g.setColor(DIM);
		g.drawRoundRect(centerX - 4, centerY - 8, 8, 10, 6, 6);
		g.fillRoundRect(centerX - 6, centerY - 3, 12, 9, 3, 3);
	}
}
