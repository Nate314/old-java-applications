package numberguess;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Draws Hot and cold: one cell per number from 1 to 100 (what still fits lit, every guess in the colour of its answer),
 * the halfway line between the last two guesses that the answer split at, and the guesses as chips, red for warmer
 * and blue for colder.
 */
final class WarmthView {
	private static final int LINE_Y = 168, LINE_HEIGHT = 36, CHIPS_Y = 248;

	private Rectangle line = new Rectangle();
	/** The number under the pointer, or 0. */
	private int hover;
	/** The number in the text field, or 0. */
	private int typed;

	static Color color(Warmth.Read read) {
		switch (read) {
		case WARMER:
			return Board.HIGH;
		case COLDER:
			return Board.LOW;
		case SAME:
			return Board.GOLD;
		case FOUND:
			return Board.RIGHT;
		default:
			return Board.SOFT;
		}
	}

	private static String word(Warmth.Read read) {
		switch (read) {
		case WARMER:
			return "warmer";
		case COLDER:
			return "colder";
		case SAME:
			return "same";
		case FOUND:
			return "found";
		default:
			return "first";
		}
	}

	/** The number a click at x, y guesses, or 0 off the line. */
	int numberAt(int x, int y) {
		if (!line.contains(x, y)) {
			return 0;
		}
		return Math.max(1, Math.min(Warmth.MAX, 1 + (x - line.x) * Warmth.MAX / line.width));
	}

	/** Follows the pointer; true if the number under it changed. */
	boolean hover(Warmth warm, int x, int y) {
		int over = warm.isOver() ? 0 : numberAt(x, y);
		boolean changed = over != hover;
		hover = over;
		return changed;
	}

	/** Follows the text field. */
	void type(int number) {
		typed = number;
	}

	int hoverNumber() {
		return hover;
	}

	private int xOf(double n) {
		return line.x + (int) Math.round((n - 1) * line.width / Warmth.MAX);
	}

	void paint(Graphics2D g, Warmth warm, Rectangle area) {
		line = new Rectangle(area.x, LINE_Y, area.width, LINE_HEIGHT);
		if (warm.isOver()) {
			hover = 0; // the pointer may not have moved since the last guess
		}
		// The pointer wins over the text field; any number can be guessed here, even a ruled-out one.
		int shown = hover > 0 || warm.isOver() || typed > Warmth.MAX ? hover : typed;
		paintMeter(g, warm, area.x);
		List<Integer> guesses = warm.guesses();
		List<Warmth.Read> reads = warm.reads();
		g.setColor(Board.CARD);
		g.fillRoundRect(line.x - 2, line.y - 2, line.width + 4, line.height + 4, 8, 8);
		for (int n = 1; n <= Warmth.MAX; n++) {
			int index = guesses.lastIndexOf(n);
			Color color = n == shown ? Board.GOLD
					: warm.isOver() && n == warm.number() ? Board.RIGHT
							: index >= 0 ? color(reads.get(index)) : warm.isPossible(n) ? Board.POSSIBLE_DARK : Board.RULED_OUT;
			g.setColor(color);
			int x0 = xOf(n), x1 = xOf(n + 1);
			g.fillRect(x0 + (x1 - x0 > 3 ? 1 : 0), line.y + (index >= 0 ? 0 : 4), Math.max(1, x1 - x0 - 1), line.height - (index >= 0 ? 0 : 8));
		}
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(Board.SOFT);
		FontMetrics small = g.getFontMetrics();
		int below = line.y + line.height + 15;
		g.drawString("1", line.x, below);
		g.drawString("100", line.x + line.width - small.stringWidth("100"), below);
		String note = hover > 0 || warm.isOver() ? null : Board.typedNote(typed, "guesses", 1, Warmth.MAX, Warmth.MAX);
		String middle = warm.isOver() ? "" : note != null ? note : "Click any number, even a ruled-out one";
		g.setColor(note != null && shown == 0 ? Board.WARN : Board.SOFT);
		g.drawString(middle, line.x + (line.width - small.stringWidth(middle)) / 2, below);
		int count = guesses.size();
		if (count >= 2 && !warm.isFound()) {
			paintHalfway(g, guesses.get(count - 2), guesses.get(count - 1), reads.get(count - 1));
		}
		for (int i = Math.max(0, count - 2); i < count; i++) {
			paintPin(g, guesses.get(i), color(reads.get(i)), i == count - 1);
		}
		if (shown > 0) {
			Board.paintBubble(g, shown, (xOf(shown) + xOf(shown + 1)) / 2, line);
		}
		paintChips(g, guesses, reads, area);
	}

	/** Guess dots: grey up to par, orange for the spare ones, like the ranges. */
	private void paintMeter(Graphics2D g, Warmth warm, int x) {
		int used = warm.guesses().size(), size = 9, gap = 4;
		for (int i = 0; i < Warmth.LIMIT; i++) {
			int dotX = x + i * (size + gap) + (i >= Warmth.PAR ? 6 : 0);
			boolean par = i < Warmth.PAR;
			g.setColor(i < used ? color(warm.reads().get(i)) : par ? Board.RULED_OUT : Board.CARD);
			g.fillOval(dotX, 117, size, size);
			if (i >= used) {
				g.setColor(par ? Board.DIM : Board.WARN.darker().darker());
				g.drawOval(dotX, 117, size, size);
			}
		}
	}

	/** The halfway point the last answer split at, dashed, with the side that was kept marked by an arrow. */
	private void paintHalfway(Graphics2D g, int before, int last, Warmth.Read read) {
		double half = (before + last) / 2.0;
		int x = (xOf(half) + xOf(half + 1)) / 2;
		g.setColor(Board.GOLD);
		// Dashes drawn by hand: the browser build's strokes have no dash pattern.
		for (int y = line.y - 6; y < line.y + line.height + 4; y += 8) {
			g.fillRect(x - 1, y, 2, 4);
		}
		g.setFont(new Font("SansSerif", Font.BOLD, 11));
		String label = "halfway " + Board.half(before + last);
		int width = g.getFontMetrics().stringWidth(label);
		int labelX = Math.max(line.x + 14, Math.min(line.x + line.width - width - 14, x - width / 2));
		g.drawString(label, labelX, line.y + line.height + 30);
		if (read == Warmth.Read.WARMER || read == Warmth.Read.COLDER) {
			boolean right = (last > before) == (read == Warmth.Read.WARMER);
			// The arrow sits past the label, on the side that was kept.
			int tip = right ? labelX + width + 12 : labelX - 12;
			int y = line.y + line.height + 26;
			g.fillPolygon(new int[] { tip, tip - (right ? 7 : -7), tip - (right ? 7 : -7) }, new int[] { y, y - 5, y + 5 }, 3);
		}
	}

	/** A pin above the line for one of the last two guesses, labelled with its number. */
	private void paintPin(Graphics2D g, int n, Color color, boolean latest) {
		int x = (xOf(n) + xOf(n + 1)) / 2;
		int top = line.y - 10;
		g.setColor(color);
		g.fillPolygon(new int[] { x - 5, x + 5, x }, new int[] { top - 6, top - 6, top + 2 }, 3);
		g.setFont(new Font("SansSerif", latest ? Font.BOLD : Font.PLAIN, latest ? 13 : 11));
		String label = Integer.toString(n);
		int width = g.getFontMetrics().stringWidth(label);
		g.drawString(label, Math.max(line.x, Math.min(line.x + line.width - width, x - width / 2)), top - 9);
	}

	private void paintChips(Graphics2D g, List<Integer> guesses, List<Warmth.Read> reads, Rectangle area) {
		g.setFont(new Font("SansSerif", Font.BOLD, 12));
		FontMetrics metrics = g.getFontMetrics();
		int x = area.x, y = CHIPS_Y, height = 22;
		int[] widths = new int[guesses.size()];
		for (int i = 0; i < widths.length; i++) {
			widths[i] = metrics.stringWidth(guesses.get(i) + " " + word(reads.get(i))) + 16;
		}
		for (int i = Board.firstChip(widths, area.x, area.x + area.width, (area.y + area.height + 12 - CHIPS_Y + 6) / (height + 6)); i < guesses.size(); i++) {
			String label = guesses.get(i) + " " + word(reads.get(i));
			int width = widths[i];
			if (x + width > area.x + area.width) {
				x = area.x;
				y += height + 6;
			}
			Color color = color(reads.get(i));
			g.setColor(Board.CARD);
			g.fillRoundRect(x, y, width, height, 10, 10);
			g.setColor(color);
			g.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
			g.drawString(label, x + 8, y + 16);
			x += width + 6;
		}
	}
}
