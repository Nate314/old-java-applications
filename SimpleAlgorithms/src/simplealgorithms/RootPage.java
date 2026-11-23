package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Square root: steps through {@link MyMath#squareRootSteps}. Each guess a is drawn as an a by n / a rectangle, whose
 * area is always n; averaging its sides makes it squarer each step, until it is the square of side sqrt(n). The
 * table lists every guess with its correct digits in green. The visitor types the number, and can drag a side of the
 * rectangle to start from a first guess of their own instead of 1: the other side follows, so the area stays n.
 */
class RootPage extends Page {
	private static final String[] PRESETS = { "42", "2", "10", "1000", "0.5", "123456789" };
	private static final int TICKS_PER_STEP = 28;
	/** Centre of the picture, and how far from it a dragged rectangle may reach. */
	private static final int CX = 240;
	private static final int CY = NARROW ? 214 : 192;
	private static final int HALF_W = 150;
	private static final int HALF_H = 115;

	private String entry = "42";
	private double num;
	private List<Double> steps;
	private int shown;
	/** The first guess: 1 as the author's loop starts, or one the visitor dragged the rectangle to. */
	private double start = 1;
	/** How many averages the author's start (1) takes on this number, to set a dragged start against. */
	private int fromOne;
	/** True while a side of the rectangle is held; the scale then stays put under the finger. */
	private boolean grabbing;
	private boolean overPicture;
	private int preset;
	private boolean playing = true;
	private int wait;
	/** The guess and scale being drawn, which glide towards the current step's. */
	private double drawnA;
	private double drawnScale;

	private final Button play;

	RootPage() {
		super("Square root");
		play = button("Pause", ' ', this::togglePlay);
		button("Step", 's', () -> move(1));
		button("Back", 'b', () -> move(-1));
		button("Next number", 'n', this::nextPreset);
		compute();
	}

	private void compute() {
		double value;
		try {
			value = Double.parseDouble(entry);
		} catch (NumberFormatException e) {
			value = Double.NaN;
		}
		if (!(value > 0 && value <= 1e12)) {
			steps = null;
			return;
		}
		num = value;
		steps = guesses(num, start);
		fromOne = guesses(num, 1).size() - 1;
		shown = 0;
		wait = 0;
		drawnA = start;
		drawnScale = scaleFor(start);
	}

	/** Every guess from start, the last one once. */
	static List<Double> guesses(double num, double start) {
		List<Double> steps = MyMath.squareRootSteps(num, start);
		// The last average repeats the guess before it (that is how the loop knows to stop): show it once.
		while (steps.size() > 1 && steps.get(steps.size() - 1).equals(steps.get(steps.size() - 2))) {
			steps.remove(steps.size() - 1);
		}
		return steps;
	}

	@Override
	boolean drags() {
		return true;
	}

	private boolean inPicture(int x, int y) {
		return x < (NARROW ? LabPanel.WIDTH : 410) && y > CY - HALF_H - 10 && y < CY + HALF_H + 10;
	}

	@Override
	void click(int x, int y) {
		if (steps == null || !inPicture(x, y)) {
			return;
		}
		pause();
		grabbing = true;
		drag(x, y);
	}

	/**
	 * The held side sets a: the rectangle is 2 |x - CX| wide, and n / a tall. While held the picture is zoomed out so
	 * that guesses from about sqrt(n) / 6 to 8 sqrt(n) fit inside it; let go, it zooms back in.
	 */
	@Override
	void drag(int x, int y) {
		if (!grabbing) {
			return;
		}
		double scale = 2 * HALF_H / (Math.sqrt(num) * 6);
		double most = 2 * HALF_W / scale;
		double least = num * scale / (2 * HALF_H);
		start = Math.max(least, Math.min(most, 2 * Math.abs(x - CX) / scale));
		steps = guesses(num, start);
		shown = 0;
		drawnA = start;
		drawnScale = scale;
	}

	@Override
	void release() {
		if (grabbing) {
			grabbing = false;
			// Let go: the averaging plays from the guess just made.
			togglePlay();
		}
	}

	@Override
	void hover(int x, int y) {
		overPicture = steps != null && inPicture(x, y);
	}

	@Override
	void hidden() {
		grabbing = false;
		overPicture = false;
	}

	private void togglePlay() {
		if (steps == null) {
			return;
		}
		if (!playing && shown == steps.size() - 1) {
			shown = 0;
		}
		playing = !playing;
		wait = 0;
		play.label = playing ? "Pause" : "Play";
	}

	private void pause() {
		playing = false;
		play.label = "Play";
	}

	private void move(int by) {
		pause();
		if (steps != null) {
			shown = Math.max(0, Math.min(steps.size() - 1, shown + by));
		}
	}

	private void nextPreset() {
		preset = (preset + 1) % PRESETS.length;
		entry = PRESETS[preset];
		start = 1;
		compute();
		playing = false;
		togglePlay();
	}

	@Override
	boolean key(KeyEvent event) {
		char c = event.getKeyChar();
		if ((c >= '0' && c <= '9' || c == '.') && entry.length() < 12) {
			entry = entry.equals("0") && c != '.' ? String.valueOf(c) : entry + c;
		} else if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE && entry.length() > 0) {
			entry = entry.substring(0, entry.length() - 1);
		} else if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
			move(1);
			return true;
		} else if (event.getKeyCode() == KeyEvent.VK_LEFT) {
			move(-1);
			return true;
		} else {
			return false;
		}
		start = 1;
		compute();
		pause();
		return true;
	}

	@Override
	boolean tick() {
		if (steps != null) {
			double a = steps.get(shown);
			double scale = scaleFor(a);
			if (!grabbing && Math.abs(drawnA - a) > 1e-9 * a || Math.abs(drawnScale - scale) > 1e-9 * scale) {
				drawnA += (a - drawnA) * 0.2;
				drawnScale += (scale - drawnScale) * 0.2;
			}
			if (playing && ++wait >= TICKS_PER_STEP) {
				wait = 0;
				if (shown < steps.size() - 1) {
					shown++;
				} else {
					pause();
				}
			}
		}
		// The caret blinks.
		return true;
	}

	/** Pixels per unit, so the rectangle's longer side is 220 px. */
	private double scaleFor(double a) {
		return 220 / Math.max(a, num / a);
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		text(g, "Square root by averaging", TITLE, INK, 20, 32);
		paragraph(g, "MyMath.estimateSquareRoot starts at 1, then averages the guess with n / guess until they agree.",
				TEXT, PENCIL, 20, 54, w - 40, 0, 16, 2);
		paintEntry(g, w);
		if (steps == null) {
			text(g, "Type a number above 0 (up to 1,000,000,000,000). Backspace deletes.", BOLD, RED, 20, 110);
			return;
		}
		paintRectangle(g);
		paintTable(g, w);
		paintCaption(g, h);
	}

	private void paintEntry(Graphics2D g, int w) {
		String label = "n =";
		int boxX = w - 20 - 150;
		text(g, label, BOLD, INK, boxX - 8 - width(g, label, BOLD), 32);
		g.setColor(Color.WHITE);
		g.fillRoundRect(boxX, 12, 150, 28, 8, 8);
		g.setColor(steps == null ? RED : INK);
		g.drawRoundRect(boxX, 12, 150, 28, 8, 8);
		text(g, entry, CODE_BOLD, INK, boxX + 8, 31);
		int caret = boxX + 8 + width(g, entry, CODE_BOLD) + 1;
		if (System.currentTimeMillis() / 500 % 2 == 0) {
			g.drawLine(caret, 17, caret, 35);
		}
	}

	private void paintRectangle(Graphics2D g) {
		double a = drawnA;
		double b = num / a;
		int wa = (int) Math.max(1, Math.round(a * drawnScale));
		int hb = (int) Math.max(1, Math.round(b * drawnScale));
		int side = (int) Math.round(Math.sqrt(num) * drawnScale);
		int cx = CX;
		// Narrow, the table goes under the picture, so the picture sits lower, under the wrapped line above.
		int cy = CY;
		g.setColor(new Color(60, 100, 180, 60));
		g.fillRect(cx - wa / 2, cy - hb / 2, wa, hb);
		g.setColor(INK);
		g.setStroke(new BasicStroke(2f));
		g.drawRect(cx - wa / 2, cy - hb / 2, wa, hb);
		g.setColor(GREEN);
		g.setStroke(new BasicStroke(1.5f));
		g.drawRect(cx - side / 2, cy - side / 2, side, side);
		g.setStroke(new BasicStroke(1f));
		// Grips on the two sides say they can be dragged; they grow while the pointer is on the picture.
		int grip = grabbing ? 7 : overPicture ? 6 : 5;
		for (int x : new int[] { cx - wa / 2, cx + wa / 2 }) {
			g.setColor(HIGHLIGHT);
			g.fillOval(x - grip, cy - grip, 2 * grip, 2 * grip);
			g.setColor(INK);
			g.setStroke(new BasicStroke(2f));
			g.drawOval(x - grip, cy - grip, 2 * grip, 2 * grip);
		}
		g.setStroke(new BasicStroke(1f));
		double guess = steps.get(shown);
		centered(g, "a = " + number(guess, 6), BOLD, INK, cx, cy + Math.max(hb, side) / 2 + 18);
		String bLabel = "n / a = " + number(num / guess, 6);
		int left = cx - Math.max(wa, side) / 2 - 8 - width(g, bLabel, BOLD);
		if (left >= 4) {
			text(g, bLabel, BOLD, INK, left, cy + 5);
		} else {
			// A wide, flat rectangle leaves no room on its left: the label goes above it.
			centered(g, bLabel, BOLD, INK, cx, cy - Math.max(hb, side) / 2 - 8);
		}
		String area = "area " + number(num, 12);
		if (wa > width(g, area, SMALL) + 8 && hb > 20) {
			centered(g, area, SMALL, INK, cx, cy + 4);
		}
		text(g, NARROW ? "Blue: a by n / a, area n. Green: side sqrt(n). Drag a side: your own guess."
				: "Blue: a by n / a, area n.  Green: the square of side sqrt(n).  Drag a blue side to guess first yourself.",
				SMALL, PENCIL, 20, NARROW ? 370 : 348);
	}

	private void paintTable(Graphics2D g, int w) {
		int x = NARROW ? 26 : 420;
		int top = NARROW ? 396 : 78;
		double root = Math.sqrt(num);
		text(g, "guess", BOLD, INK, x + 34, top);
		text(g, "right digits", BOLD, INK, w - 20 - width(g, "right digits", BOLD), top);
		int rows = NARROW ? 8 : 12;
		int first = Math.max(0, Math.min(shown - rows + 2, steps.size() - rows));
		for (int i = first; i <= shown && i < first + rows; i++) {
			int y = top + 22 + (i - first) * 21;
			double guess = steps.get(i);
			int right = rightDigits(guess, root);
			if (i == shown) {
				g.setColor(HIGHLIGHT);
				g.fillRoundRect(x - 6, y - 15, w - 20 - x + 6, 20, 6, 6);
			}
			text(g, String.valueOf(i), CODE, PENCIL, x, y);
			String digits = number(guess, 15);
			int split = splitAfterDigits(digits, right);
			text(g, digits.substring(0, split), CODE_BOLD, GREEN, x + 34, y);
			text(g, digits.substring(split), CODE, INK, x + 34 + width(g, digits.substring(0, split), CODE_BOLD), y);
			String count = String.valueOf(right);
			text(g, count, CODE_BOLD, right >= 15 ? GREEN : INK, w - 20 - width(g, count, CODE_BOLD), y);
		}
	}

	private void paintCaption(Graphics2D g, int h) {
		double guess = steps.get(shown);
		String caption;
		if (grabbing) {
			caption = "Your first guess: a = " + number(guess, 6) + ", so n / a = " + number(num / guess, 6)
					+ ". Let go to average from it.";
		} else if (shown == steps.size() - 1) {
			caption = "Guess " + shown + " and n / guess agree: sqrt(" + number(num, 12) + ") ~= " + guess
					+ " after " + shown + " averages" + (start == 1 ? "." : " (from 1: " + fromOne + ").");
		} else {
			caption = "Guess " + shown + ": the average of " + number(guess, 8) + " and " + number(num / guess, 8)
					+ " is " + number(steps.get(shown + 1), 8) + ", the next guess.";
		}
		if (NARROW) {
			paragraph(g, caption, BOLD, INK, 20, h - 32, LabPanel.WIDTH - 40, 0, 18, 2);
		} else {
			text(g, caption, BOLD, INK, 20, h - 14);
		}
	}

	/** Correct significant digits of guess against root, 0 to 16. */
	static int rightDigits(double guess, double root) {
		double error = Math.abs(guess - root) / root;
		if (error == 0) {
			return 16;
		}
		return (int) Math.max(0, Math.min(16, Math.floor(-Math.log10(error))));
	}

	/** Index in text just after its first count significant digits (leading zeros and the point do not count). */
	private static int splitAfterDigits(String text, int count) {
		int seen = 0;
		boolean started = false;
		for (int i = 0; i < text.length(); i++) {
			if (seen >= count) {
				return i;
			}
			char c = text.charAt(i);
			if (c >= '1' && c <= '9' || c == '0' && started) {
				started = true;
				seen++;
			} else if (c == 'E') {
				return i;
			}
		}
		return text.length();
	}
}
