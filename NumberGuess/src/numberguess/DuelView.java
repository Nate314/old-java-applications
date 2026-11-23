package numberguess;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Timer;

/**
 * Draws a {@link Duel} on the board. Before it starts, one line of all 100 numbers to click your hidden number on.
 * Then two lanes: yours on top (the whole range with what is left of its number lit, and below it a line of just
 * that part to click a guess on) and the computer's below (the whole range with what is left of your number lit,
 * your number ringed in gold, its guesses marked), with what it is thinking and the duels won so far.
 */
final class DuelView {
	private static final Color THEM = new Color(196, 120, 255);
	private static final Color THEM_DARK = new Color(96, 62, 140);

	/** A shade apart from {@link Board#POSSIBLE_DARK}, for every other row of ten in the hiding grid. */
	private static final Color HIDE_ODD = new Color(28, 86, 72);
	private static final int HIDE_Y = 144, CARD_HEIGHT = 42, CELL_MIN = 18, CELL_MAX = 30;
	private static final int YOUR_LABEL_Y = 135, YOUR_BAR_Y = 147, YOUR_ZOOM_Y = 165, YOUR_ZOOM_HEIGHT = 30;
	private static final int ITS_LABEL_Y = 230, ITS_BAR_Y = 244, ITS_BAR_HEIGHT = 14;
	private static final int THINK_MS = 750, FLASH_MS = 600;

	private final Timer timer;
	/** When the computer started thinking or last guessed, for the dots and the flash. */
	private long movedAt;
	/** Where the pointer is, or -1. */
	private int hoverX = -1, hoverY = -1;
	/** The number in the text field, or 0. */
	private int typed;
	/** Where the clickable line was last drawn. */
	private Rectangle clickable = new Rectangle();
	private int clickLow = 1, clickHigh = Duel.MAX;
	/** Numbers a row when the clickable part is the hiding grid, else 0 (a line). */
	private int gridColumns;

	DuelView(Runnable repaint) {
		timer = new Timer(80, event -> {
			if (elapsed() > THINK_MS + FLASH_MS) {
				((Timer) event.getSource()).stop();
			}
			repaint.run();
		});
	}

	/** How long the computer waits before it guesses. */
	static int thinkMillis() {
		return THINK_MS;
	}

	private long elapsed() {
		return System.currentTimeMillis() - movedAt;
	}

	/** The computer starts thinking (dots) or has just guessed (its guess flashes). */
	void moved() {
		movedAt = System.currentTimeMillis();
		timer.restart();
	}

	/** Remembers the pointer; true when that changes what a click would pick. */
	boolean hover(Duel duel, int x, int y) {
		int before = hoverNumber(duel);
		hoverX = x;
		hoverY = y;
		return hoverNumber(duel) != before;
	}

	/** The number a click at the pointer would hide or guess, or 0. */
	int hoverNumber(Duel duel) {
		return numberAt(duel, hoverX, hoverY);
	}

	/** Follows the text field. */
	void type(int number) {
		typed = number;
	}

	/** The number the clickable line marks: the one under the pointer, else the typed one if Enter would use it. */
	private int shown(Duel duel) {
		int hover = hoverNumber(duel);
		boolean usable = duel.isHiding() || duel.isYourTurn();
		return hover > 0 || !usable || typed < clickLow || typed > clickHigh ? hover : typed;
	}

	/** The line's middle label: what Enter would do with the typed number, or {@code idle}. */
	private String note(Duel duel, String verb, String idle) {
		String note = hoverNumber(duel) > 0 ? null : Board.typedNote(typed, verb, clickLow, clickHigh, Duel.MAX);
		return note != null ? note : idle;
	}

	/** Warning orange for a typed number Enter would not count, else the usual soft grey. */
	private Color noteColor(Duel duel) {
		return typed > 0 && shown(duel) == 0 && (duel.isHiding() || duel.isYourTurn()) ? Board.WARN : Board.SOFT;
	}

	/** The number a click at ({@code x}, {@code y}) would hide or guess, or 0. */
	int numberAt(Duel duel, int x, int y) {
		if (duel == null || !(duel.isHiding() || duel.isYourTurn()) || !clickable.contains(x, y)) {
			return 0;
		}
		for (int number = 1; gridColumns > 0 && number <= Duel.MAX; number++) {
			if (cellOf(clickable, gridColumns, number).contains(x, y)) {
				return number;
			}
		}
		long count = (long) clickHigh - clickLow + 1;
		long offset = (long) (x - clickable.x) * count / clickable.width;
		return (int) Math.max(clickLow, Math.min(clickHigh, clickLow + offset));
	}

	void paint(Graphics2D g, Duel duel, Records records, Rectangle area) {
		if (duel.isHiding()) {
			paintHiding(g, duel, records, area);
		} else {
			paintYours(g, duel, area);
			paintIts(g, duel, area);
		}
		paintTally(g, records, area);
	}

	// ---- before the duel ----

	private void paintHiding(Graphics2D g, Duel duel, Records records, Rectangle area) {
		Rectangle grid = hidingGrid(area);
		boolean card = grid.y + grid.height + 10 + CARD_HEIGHT <= area.y + area.height - 14;
		clickable = grid;
		gridColumns = columns(area);
		clickLow = 1;
		clickHigh = Duel.MAX;
		label(g, "YOUR HIDDEN NUMBER", area.x, HIDE_Y - 8, Board.SOFT);
		String note = note(duel, "hides", "Click one, or type it");
		g.setColor(noteColor(duel));
		g.drawString(note, area.x + area.width - g.getFontMetrics().stringWidth(note), HIDE_Y - 8);
		int hover = shown(duel);
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		for (int number = 1; number <= Duel.MAX; number++) {
			Rectangle cell = cellOf(grid, gridColumns, number);
			// Each row of ten a shade apart, so the tens are easy to find.
			boolean odd = (number - 1) / 10 % 2 == 1;
			g.setColor(number == hover ? Board.GOLD : odd ? HIDE_ODD : Board.POSSIBLE_DARK);
			g.fillRoundRect(cell.x + 1, cell.y + 1, cell.width - 2, cell.height - 2, 6, 6);
			String text = Integer.toString(number);
			g.setColor(number == hover ? Board.BACKGROUND : Board.TEXT);
			g.drawString(text, cell.x + (cell.width - metrics.stringWidth(text)) / 2, cell.y + (cell.height + metrics.getAscent()) / 2 - 2);
		}
		if (!card) {
			// A short board: the subline above already gives the rules.
			return;
		}
		// The computer's number, face down.
		int cardX = area.x, cardY = area.y + area.height - 14 - CARD_HEIGHT;
		g.setColor(THEM_DARK);
		g.fillRoundRect(cardX, cardY, 36, CARD_HEIGHT, 10, 10);
		g.setColor(THEM);
		g.drawRoundRect(cardX, cardY, 35, CARD_HEIGHT - 1, 10, 10);
		g.setFont(new Font("SansSerif", Font.BOLD, 24));
		g.drawString("?", cardX + 18 - g.getFontMetrics().stringWidth("?") / 2, cardY + 30);
		g.setColor(Board.TEXT);
		g.setFont(new Font("SansSerif", Font.BOLD, 14));
		Board.drawFitted(g, "The computer has hidden its number.", cardX + 48, cardY + 17, area.width - 48);
		g.setColor(Board.SOFT);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		Board.drawFitted(g, "Each round you both guess. Fewer guesses wins; the same is a draw.", cardX + 48, cardY + 35, area.width - 48);
	}

	/** Twenty numbers a row on the wide board, ten on a narrow one. */
	private static int columns(Rectangle area) {
		return area.width >= 500 ? 20 : 10;
	}

	/**
	 * The 1 to 100 grid to hide your number on: rows as tall as fit (up to {@link #CELL_MAX}) above the computer's
	 * card, or, when that would make them shorter than {@link #CELL_MIN}, in all the room without the card.
	 */
	static Rectangle hidingGrid(Rectangle area) {
		int rows = Duel.MAX / columns(area), bottom = area.y + area.height - 14;
		int rowHeight = Math.min(CELL_MAX, (bottom - CARD_HEIGHT - 10 - HIDE_Y) / rows);
		if (rowHeight < CELL_MIN) {
			rowHeight = Math.min(CELL_MAX, (bottom - HIDE_Y) / rows);
		}
		return new Rectangle(area.x, HIDE_Y, area.width, rowHeight * rows);
	}

	/** Where {@code number} sits in a grid of {@code columns} a row. */
	static Rectangle cellOf(Rectangle grid, int columns, int number) {
		int rows = Duel.MAX / columns, column = (number - 1) % columns, row = (number - 1) / columns;
		int x0 = xOf(grid, column, 0, columns), x1 = xOf(grid, column + 1, 0, columns);
		int rowHeight = grid.height / rows;
		return new Rectangle(x0, grid.y + row * rowHeight, x1 - x0, rowHeight);
	}

	// ---- your lane: finding its number ----

	private void paintYours(Graphics2D g, Duel duel, Rectangle area) {
		Game game = duel.yours();
		boolean over = duel.isOver();
		label(g, "YOU: FIND ITS NUMBER", area.x, YOUR_LABEL_Y, Board.TEXT);
		count(g, game.getGuesses(), area, YOUR_LABEL_Y, Board.TEXT);
		Rectangle bar = new Rectangle(area.x, YOUR_BAR_Y, area.width, 8);
		paintRange(g, bar, game.low(), game.high(), game.isFound() ? Board.RIGHT : Board.POSSIBLE);
		paintMarks(g, bar, game.guesses(), game.number(), false, -1);
		Rectangle zoom = new Rectangle(area.x, YOUR_ZOOM_Y, area.width, YOUR_ZOOM_HEIGHT);
		clickable = zoom;
		gridColumns = 0;
		clickLow = game.low();
		clickHigh = game.high();
		g.setColor(Board.POSSIBLE_DARK);
		g.drawLine(xOf(bar, game.low() - 1, 0, Duel.MAX), bar.y + bar.height + 1, zoom.x, zoom.y - 2);
		g.drawLine(xOf(bar, game.high(), 0, Duel.MAX), bar.y + bar.height + 1, zoom.x + zoom.width, zoom.y - 2);
		int hover = shown(duel);
		if (over && !game.isFound()) {
			g.setColor(Board.CARD);
			g.fillRoundRect(zoom.x, zoom.y, zoom.width, zoom.height, 8, 8);
			centered(g, "Its number was " + game.number() + ". You had it down to " + range(game) + ".", zoom, Board.TEXT);
		} else {
			paintCells(g, zoom, game.low(), game.high(),
					number -> game.isFound() ? Board.RIGHT : number == hover ? Board.GOLD : Board.POSSIBLE_DARK);
		}
		String middle = duel.isYourTurn() ? note(duel, "guesses", "Click to guess") : duel.isComputersTurn() ? "Its turn" : "";
		ends(g, zoom, game.low() == game.high() ? "" : Integer.toString(game.low()),
				game.low() == game.high() ? "" : Integer.toString(game.high()), middle, noteColor(duel));
		if (hover > 0) {
			Board.paintBubble(g, hover, xOf(zoom, hover - game.low() + 0.5, 0, game.high() - game.low() + 1), zoom);
		}
	}

	// ---- its lane: finding yours ----

	private void paintIts(Graphics2D g, Duel duel, Rectangle area) {
		Game game = duel.its();
		label(g, "THE COMPUTER: FIND YOURS (" + duel.hidden() + ")", area.x, ITS_LABEL_Y, THEM);
		count(g, game.getGuesses(), area, ITS_LABEL_Y, THEM);
		Rectangle bar = new Rectangle(area.x, ITS_BAR_Y, area.width, ITS_BAR_HEIGHT);
		paintRange(g, bar, game.low(), game.high(), game.isFound() ? Board.HIGH : THEM_DARK);
		paintSecret(g, bar, duel.hidden(), Board.GOLD);
		boolean flash = !duel.isComputersTurn() && game.getGuesses() > 0 && elapsed() < THINK_MS + FLASH_MS;
		paintMarks(g, bar, game.guesses(), game.number(), true, flash ? game.getGuesses() - 1 : -1);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		g.setColor(Board.SOFT);
		Board.drawFitted(g, thought(duel), area.x, ITS_BAR_Y + ITS_BAR_HEIGHT + 32, area.width);
	}

	/** What the computer is up to, in its own words. */
	private String thought(Duel duel) {
		Game game = duel.its();
		if (duel.isComputersTurn()) {
			return "Thinking" + "...".substring(0, (int) Math.min(3, elapsed() / 200));
		}
		if (game.getGuesses() == 0) {
			return "I will guess after you. I have to find " + duel.hidden() + " too.";
		}
		int last = game.guesses().get(game.getGuesses() - 1);
		if (game.isFound()) {
			return "I guessed " + last + ". Found you!";
		}
		return "I guessed " + last + ": too " + (last < game.number() ? "low" : "high") + ". So yours is from " + range(game) + ".";
	}

	private void paintTally(Graphics2D g, Records records, Rectangle area) {
		int won = records.duels(Duel.Result.WON), lost = records.duels(Duel.Result.LOST), drawn = records.duels(Duel.Result.DRAWN);
		if (won + lost + drawn == 0) {
			return;
		}
		String text = "Duels: you " + won + ", the computer " + lost + (drawn > 0 ? ", " + drawn + (drawn == 1 ? " draw" : " draws") : "");
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(Board.DIM);
		g.drawString(text, area.x + area.width - g.getFontMetrics().stringWidth(text), area.y + area.height);
	}

	// ---- drawing helpers ----

	private static String range(Game game) {
		return game.low() == game.high() ? Integer.toString(game.low()) : game.low() + " to " + game.high();
	}

	private static void label(Graphics2D g, String text, int x, int y, Color color) {
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		g.setColor(color);
		g.drawString(text, x, y);
	}

	/** "3 guesses" at the right end of a lane's label row. */
	private static void count(Graphics2D g, int guesses, Rectangle area, int y, Color color) {
		String text = guesses + (guesses == 1 ? " guess" : " guesses");
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		g.setColor(color);
		g.drawString(text, area.x + area.width - g.getFontMetrics().stringWidth(text), y);
	}

	private static int xOf(Rectangle line, double value, double from, double to) {
		return (int) Math.round(line.x + (value - from) / (to - from) * line.width);
	}

	/** A bar of 1 to MAX with {@code low} to {@code high} lit. */
	private static void paintRange(Graphics2D g, Rectangle bar, int low, int high, Color lit) {
		g.setColor(Board.RULED_OUT);
		g.fillRoundRect(bar.x, bar.y, bar.width, bar.height, bar.height, bar.height);
		if (low <= high) {
			int left = xOf(bar, low - 1, 0, Duel.MAX);
			int right = Math.max(left + 3, xOf(bar, high, 0, Duel.MAX));
			g.setColor(lit);
			g.fillRoundRect(left, bar.y, right - left, bar.height, 6, 6);
		}
	}

	/** A triangle over the bar for each guess, blue too low, red too high, green right; {@code flash} one blinks. */
	private static void paintMarks(Graphics2D g, Rectangle bar, List<Integer> guesses, int number, boolean labelled, int flash) {
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		for (int i = 0; i < guesses.size(); i++) {
			int guess = guesses.get(i);
			int x = xOf(bar, guess - 0.5, 0, Duel.MAX);
			int size = i == flash ? 7 : 4;
			g.setColor(i == flash && System.currentTimeMillis() / 150 % 2 == 0 ? Board.TEXT
					: guess < number ? Board.LOW : guess > number ? Board.HIGH : Board.RIGHT);
			g.fillPolygon(new int[] { x - size, x + size, x }, new int[] { bar.y - 8, bar.y - 8, bar.y - 1 }, 3);
			if (labelled && i == guesses.size() - 1) {
				String text = Integer.toString(guess);
				g.drawString(text, x + 7, bar.y - 1);
			}
		}
	}

	/** A diamond under the bar at {@code number}, with the number beside it. */
	private static void paintSecret(Graphics2D g, Rectangle bar, int number, Color color) {
		int x = xOf(bar, number - 0.5, 0, Duel.MAX), y = bar.y + bar.height + 3;
		g.setColor(color);
		g.fillPolygon(new int[] { x, x + 5, x, x - 5 }, new int[] { y, y + 5, y + 10, y + 5 }, 4);
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		String text = Integer.toString(number);
		int width = g.getFontMetrics().stringWidth(text);
		g.drawString(text, x + 8 + width > bar.x + bar.width ? x - 8 - width : x + 8, y + 10);
	}

	interface CellColor {
		Color of(int number);
	}

	/** One cell per number from {@code low} to {@code high}, labelled when there is room. */
	private static void paintCells(Graphics2D g, Rectangle line, int low, int high, CellColor color) {
		g.setColor(Board.CARD);
		g.fillRoundRect(line.x, line.y, line.width, line.height, 8, 8);
		int count = high - low + 1;
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		boolean labels = metrics.stringWidth(Integer.toString(high)) + 4 <= line.width / count;
		for (int i = 0; i < count; i++) {
			int number = low + i;
			int x0 = xOf(line, i, 0, count), x1 = xOf(line, i + 1, 0, count);
			Color fill = color.of(number);
			g.setColor(fill);
			int inset = x1 - x0 > 6 ? 1 : 0;
			g.fillRoundRect(x0 + inset, line.y + 3, Math.max(1, x1 - x0 - 2 * inset), line.height - 6, 6, 6);
			if (labels) {
				String text = Integer.toString(number);
				g.setColor(fill == Board.POSSIBLE_DARK ? Board.TEXT : Board.BACKGROUND);
				g.drawString(text, (x0 + x1 - metrics.stringWidth(text)) / 2, line.y + line.height / 2 + 5);
			} else if (number % 10 == 0 && number < high) {
				// Too narrow to label: a notch after every ten instead, a deeper one at 50.
				g.setColor(Board.BACKGROUND);
				int notch = number % 50 == 0 ? 0 : 8;
				g.fillRect(x1 - 1, line.y + 3 + notch, 2, line.height - 6 - 2 * notch);
			}
		}
	}

	private static void ends(Graphics2D g, Rectangle line, String low, String high, String middle, Color middleColor) {
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(Board.SOFT);
		int y = line.y + line.height + 14;
		g.drawString(low, line.x, y);
		g.drawString(high, line.x + line.width - g.getFontMetrics().stringWidth(high), y);
		g.setColor(middleColor);
		g.drawString(middle, line.x + (line.width - g.getFontMetrics().stringWidth(middle)) / 2, y);
	}

	private static void centered(Graphics2D g, String text, Rectangle line, Color color) {
		g.setFont(new Font("SansSerif", Font.BOLD, 13));
		g.setColor(color);
		g.drawString(text, line.x + (line.width - g.getFontMetrics().stringWidth(text)) / 2, line.y + line.height / 2 + 5);
	}
}
