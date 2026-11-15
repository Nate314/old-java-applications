package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Division: steps through {@link MyMath#divide}, which counts its way to the answer. It adds 1 to its
 * guess until guess * b passes a, steps back, then counts in tenths from there, then hundredths, and stops once a
 * step back is under 0.01. Each step size gets its own ruler, each ruler a tenfold zoom of one step of the ruler
 * above, and the lines it printed are listed beside them. The visitor types the division.
 */
class DividePage extends Page {
	private static final String[] PRESETS = { "22/7", "4/2", "7/2", "1/3", "100/7", "2/3" };
	private static final double MAX_QUOTIENT = 100;
	private static final int LEFT = 40;
	private static final int RULER_W = 380;
	private static final int FIRST_ROW = 108;
	private static final int ROW_GAP = 62;
	private static final Font SOURCE_FONT = new Font("Monospaced", Font.PLAIN, 12);
	private static final Color NOTE = new Color(60, 100, 180);

	enum Kind { UP, OVER, BACK, DONE }

	/**
	 * One step of the search: the guess after it, the line divide printed for it (null for the value it returns), and
	 * divide's tolerance and counter2 after it, which only change on a step back.
	 */
	static final class Step {
		final double guess;
		final String line;
		final int level;
		final Kind kind;
		double tolerance = 1;
		double counter2 = 90;

		Step(double guess, String line, int level, Kind kind) {
			this.guess = guess;
			this.line = line;
			this.level = level;
			this.kind = kind;
		}

		/** True if this pass of divide's loop took its if branch (stepped back), not the else (counted on). */
		boolean back() {
			return kind == Kind.BACK || kind == Kind.DONE && line == null;
		}
	}

	/** MyMath.divide as it was written (printing its steps), shown by the Code view. */
	static final String[] SOURCE = {
			"double divide(double num1, double num2) {",
			"  double guess = 0;",
			"  double error = 0.01;",
			"  double tolerance = 1;",
			"  double currentGuess = 5;",
			"  double counter2 = 90;",
			"  int counter = 0;",
			"  while(guess * num2 != num1) {",
			"    if(guess * num2 > num1) {",
			"      guess -= tolerance;",
			"      tolerance -= counter2 * error;",
			"      counter2 /= 10;",
			"      if(currentGuess - guess < error) break;",
			"    }",
			"    else {",
			"      guess += tolerance;",
			"    }",
			"    System.out.println(counter + \": \" + guess);",
			"    currentGuess = guess;",
			"    counter++;",
			"  }",
			"  return guess;",
			"}" };

	/**
	 * Every step divide takes for a / b, read back from the lines it prints. A guess below the one before is a step
	 * back to a smaller step size (level 0 counts by 1, level 1 by 0.1...). The value divide returns comes last, as
	 * a DONE step: it is the step back it makes but does not print, or the last guess when guess * b hits a exactly.
	 */
	static List<Step> steps(double a, double b) {
		List<String> lines = new ArrayList<>();
		double result = MyMath.divide(a, b, lines::add);
		List<Step> steps = new ArrayList<>();
		double before = 0;
		int level = 0;
		for (String line : lines) {
			double guess = Double.parseDouble(line.substring(line.indexOf(": ") + 2));
			if (guess < before) {
				steps.add(new Step(guess, line, level, Kind.BACK));
				level++;
			} else {
				steps.add(new Step(guess, line, level, guess * b > a ? Kind.OVER : Kind.UP));
			}
			before = guess;
		}
		if (steps.isEmpty() || result != before) {
			steps.add(new Step(result, null, level, Kind.DONE));
		} else {
			Step last = steps.remove(steps.size() - 1);
			steps.add(new Step(last.guess, last.line, last.level, Kind.DONE));
		}
		// tolerance and counter2 as divide changes them, with the same double arithmetic, for the Code view.
		double tolerance = 1;
		double counter2 = 90;
		for (Step step : steps) {
			if (step.back()) {
				tolerance -= counter2 * 0.01;
				counter2 /= 10;
			}
			step.tolerance = tolerance;
			step.counter2 = counter2;
		}
		return steps;
	}

	private String entry = PRESETS[0];
	private double a;
	private double b;
	private List<Step> steps;
	private int shown;
	private int preset;
	private boolean playing = true;
	private int wait;
	/** Code view: divide's source with this step's lines lit, in place of the rulers. */
	private boolean code;

	private final Button play;
	private final Button codeButton;

	DividePage() {
		super("Division");
		play = button("Pause", ' ', this::togglePlay);
		button("Step", 's', () -> move(1));
		button("Back", 'b', () -> move(-1));
		button("Next division", 'n', this::nextPreset);
		codeButton = button("Code", 'c', this::toggleCode);
		compute();
	}

	private void compute() {
		steps = null;
		int slash = entry.indexOf('/');
		if (slash < 0) {
			return;
		}
		try {
			a = Double.parseDouble(entry.substring(0, slash));
			b = Double.parseDouble(entry.substring(slash + 1));
		} catch (NumberFormatException e) {
			return;
		}
		// divide never ends for b = 0 and counts up by 1 to the answer, so keep it positive and the count by 1 short.
		if (a > 0 && b > 0 && a / b <= MAX_QUOTIENT) {
			steps = steps(a, b);
			shown = 0;
			wait = 0;
		}
	}

	private void toggleCode() {
		code = !code;
		codeButton.label = code ? "Rulers" : "Code";
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
		compute();
		playing = false;
		togglePlay();
	}

	@Override
	boolean key(KeyEvent event) {
		char c = event.getKeyChar();
		if ((c >= '0' && c <= '9' || c == '.' || c == '/') && entry.length() < 12) {
			entry += c;
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
		compute();
		pause();
		return true;
	}

	@Override
	boolean tick() {
		// A long count by 1 (100/7) plays faster, so every division takes about the same time.
		if (steps != null && playing && ++wait >= Math.max(3, Math.min(16, 400 / steps.size()))) {
			wait = 0;
			if (shown < steps.size() - 1) {
				shown++;
			} else {
				pause();
			}
		}
		// The caret blinks.
		return true;
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		text(g, "Division by counting", TITLE, INK, 20, 32);
		if (!code) {
			text(g, "MyMath.divide adds 1 until guess * b passes a, steps back, then counts by 0.1, then 0.01.",
					TEXT, PENCIL, 20, 54);
		}
		paintEntry(g, w);
		if (steps == null) {
			text(g, "Type a division like 22/7: two numbers above 0, with a / b at most 100. Backspace deletes.",
					BOLD, RED, 20, 110);
			return;
		}
		if (code) {
			paintCode(g);
		} else {
			paintRulers(g);
		}
		paintLines(g, w, code ? 505 : 470);
		paintCaption(g, h);
	}

	private void paintEntry(Graphics2D g, int w) {
		String label = "a / b =";
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

	/** The step size of a level: 1, 0.1, 0.01... */
	private static double size(int level) {
		return Math.pow(10, -level);
	}

	/** Where level's ruler starts: 0, or the guess its step back above landed on. */
	private double start(int level) {
		if (level == 0) {
			return 0;
		}
		for (Step step : steps) {
			if (step.kind == Kind.BACK && step.level == level - 1) {
				return step.guess;
			}
		}
		return 0;
	}

	/** Where level's ruler ends: ten steps on from its start, or for the count by 1 its furthest guess. */
	private double end(int level) {
		if (level > 0) {
			return start(level) + 10 * size(level);
		}
		double furthest = 1;
		for (Step step : steps) {
			if (step.level == 0) {
				furthest = Math.max(furthest, Math.ceil(step.guess));
			}
		}
		return furthest;
	}

	private int x(double value, int level) {
		double from = start(level);
		return LEFT + (int) Math.round((value - from) / (end(level) - from) * RULER_W);
	}

	private void paintRulers(Graphics2D g) {
		int deepest = steps.get(shown).level;
		double exact = a / b;
		for (int level = 0; level <= deepest; level++) {
			int y = FIRST_ROW + level * ROW_GAP;
			double from = start(level);
			double to = end(level);
			int ticks = (int) Math.round((to - from) / size(level));
			String label = "count by " + number(size(level), 3);
			// Paper behind the label, so the zoom lines from the ruler above pass under it.
			g.setColor(PAPER);
			g.fillRect(LEFT - 2, y - 44, width(g, label, BOLD) + 4, 18);
			text(g, label, BOLD, INK, LEFT, y - 30);
			g.setColor(INK);
			g.setStroke(new BasicStroke(1.5f));
			g.drawLine(LEFT, y, LEFT + RULER_W, y);
			g.setStroke(new BasicStroke(1f));
			int every = Math.max(1, (ticks + 19) / 20);
			for (int i = 0; i <= ticks; i += every) {
				int tx = x(from + i * size(level), level);
				g.drawLine(tx, y - 4, tx, y + 4);
			}
			text(g, number(from, 6), SMALL, PENCIL, LEFT - width(g, number(from, 6), SMALL) / 2, y + 17);
			text(g, number(to, 6), SMALL, PENCIL, LEFT + RULER_W - width(g, number(to, 6), SMALL) / 2, y + 17);
			if (exact >= from && exact <= to) {
				int ex = x(exact, level);
				g.setColor(GREEN);
				g.fillPolygon(new int[] { ex - 5, ex + 5, ex }, new int[] { y + 12, y + 12, y + 3 }, 3);
			}
			if (level < deepest) {
				// The zoom: one step of this ruler, between the step back and the guess that passed a, is all of the next.
				double back = start(level + 1);
				int x1 = x(back, level);
				int x2 = x(back + size(level), level);
				g.setColor(new Color(255, 226, 120, 140));
				g.fillRect(x1, y - 6, Math.max(2, x2 - x1), 12);
				g.setColor(PENCIL);
				g.drawLine(x1, y + 6, LEFT, y + ROW_GAP - 8);
				g.drawLine(x2, y + 6, LEFT + RULER_W, y + ROW_GAP - 8);
			}
		}
		paintHops(g);
		centered(g, "green mark: a / b = " + number(exact, 10), SMALL, GREEN, LEFT + RULER_W / 2,
				FIRST_ROW + (deepest + 1) * ROW_GAP - 30);
	}

	/** Each guess as a hop from the one before it: over the ruler counting up, under it stepping back. */
	private void paintHops(Graphics2D g) {
		double before = 0;
		for (int i = 0; i <= shown; i++) {
			Step step = steps.get(i);
			int y = FIRST_ROW + step.level * ROW_GAP;
			int from = x(before, step.level);
			int to = x(step.guess, step.level);
			boolean back = step.guess < before;
			Color color = step.kind == Kind.OVER ? RED : step.kind == Kind.DONE ? GREEN : back ? PENCIL : INK;
			g.setColor(color);
			g.setStroke(new BasicStroke(i == shown ? 2f : 1.2f));
			int lift = Math.min(18, 6 + Math.abs(to - from) / 3);
			// Half an ellipse from the guess before to this one (the web's AWT has drawArc, not QuadCurve2D).
			g.drawArc(Math.min(from, to), y - lift, Math.abs(to - from), 2 * lift, 0, back ? -180 : 180);
			g.setStroke(new BasicStroke(1f));
			int r = i == shown ? 5 : 3;
			if (i == shown) {
				g.setColor(HIGHLIGHT);
				g.fillOval(to - 8, y - 8, 16, 16);
				g.setColor(color);
				// Above the dot, unless that would cover the ruler's "count by" label at its left end.
				String value = number(step.guess, 8);
				boolean low = to - width(g, value, BOLD) / 2 < LEFT + width(g, "count by " + number(size(step.level), 3), BOLD) + 6;
				centered(g, value, BOLD, color, to, low ? y + 28 : y - 22);
			}
			g.fillOval(to - r, y - r, 2 * r, 2 * r);
			before = step.guess;
		}
	}

	/**
	 * The Code view: divide's source, with the lines this step ran lit and what they did written beside them. Each
	 * step is one pass of the while loop (the first also runs the set-up lines above it).
	 */
	private void paintCode(Graphics2D g) {
		Step step = steps.get(shown);
		Step previous = shown > 0 ? steps.get(shown - 1) : null;
		double before = previous != null ? previous.guess : 0;
		boolean back = step.back();
		boolean exact = step.kind == Kind.DONE && step.guess * b == a;
		String[] notes = new String[SOURCE.length];
		boolean[] ran = new boolean[SOURCE.length];
		for (int i = 0; i <= 6; i++) {
			ran[i] = shown == 0;
		}
		ran[7] = ran[8] = true;
		double product = before * b;
		notes[7] = number(product, 8) + " != " + number(a, 8);
		notes[8] = back ? number(product, 8) + " > " + number(a, 8) : "no: to else";
		if (back) {
			ran[9] = ran[10] = ran[11] = ran[12] = ran[13] = true;
			notes[9] = "guess = " + number(step.guess, 8);
			notes[10] = "tolerance = " + number(step.tolerance, 8);
			notes[11] = "counter2 = " + number(step.counter2, 8);
			// currentGuess still holds the guess before this step back: their gap is the step just taken back.
			// Written on the row under the test, which is too long to leave room beside it.
			double gap = before - step.guess;
			notes[13] = "currentGuess - guess = " + number(gap, 15) + (gap < 0.01 ? " < 0.01" : " >= 0.01");
		} else {
			ran[14] = ran[15] = true;
			notes[15] = "guess = " + number(step.guess, 8);
		}
		if (step.line != null) {
			ran[17] = ran[18] = ran[19] = true;
			notes[19] = "counter = " + (shown + 1);
		}
		if (step.kind == Kind.DONE) {
			ran[21] = true;
			notes[21] = "returns " + number(step.guess, 10);
			if (exact) {
				// The loop's test runs once more after this pass, and now it fails.
				notes[7] += ", then " + number(step.guess * b, 8) + " == " + number(a, 8);
			}
		}
		int top = 52;
		int lineH = 13;
		for (int i = 0; i < SOURCE.length; i++) {
			int y = top + i * lineH;
			if (ran[i]) {
				g.setColor(i == 21 ? new Color(46, 125, 80, 60) : new Color(255, 226, 120, 170));
				g.fillRect(16, y - 10, 480, lineH);
			}
			text(g, SOURCE[i], SOURCE_FONT, ran[i] ? INK : PENCIL, 20, y);
			if (notes[i] != null && ran[i]) {
				Color color = i == 8 && back ? RED : i == 13 ? (step.kind == Kind.DONE ? GREEN : PENCIL)
						: i == 21 ? GREEN : NOTE;
				int x = i == 13 ? 80 : Math.max(340, 28 + width(g, SOURCE[i], SOURCE_FONT));
				text(g, notes[i], SMALL, color, x, y);
			}
		}
	}

	/** The lines divide printed, the one for this step highlighted; red ones passed a. */
	private void paintLines(Graphics2D g, int w, int x) {
		int top = 78;
		text(g, "what divide prints", BOLD, INK, x, top);
		int rows = 12;
		int first = Math.max(0, Math.min(shown - rows + 2, steps.size() - rows));
		for (int i = first; i <= shown && i < first + rows; i++) {
			Step step = steps.get(i);
			int y = top + 22 + (i - first) * 21;
			if (i == shown) {
				g.setColor(HIGHLIGHT);
				g.fillRoundRect(x - 6, y - 15, w - 20 - x + 6, 20, 6, 6);
			}
			String line = step.line != null ? step.line : "returns " + step.guess;
			Color color = step.kind == Kind.OVER ? RED : step.kind == Kind.DONE ? GREEN : step.kind == Kind.BACK ? PENCIL : INK;
			// Beside the code the column is narrower, so its lines take the code's smaller type.
			text(g, line, code ? SOURCE_FONT : step.kind == Kind.DONE ? CODE_BOLD : CODE, color, x, y);
		}
	}

	private void paintCaption(Graphics2D g, int h) {
		Step step = steps.get(shown);
		double product = step.guess * b;
		String compare = product > a ? "more than" : product < a ? "less than" : "exactly";
		// When rounding would hide a miss by a hair (3.5000000000000004 * 2), show the doubles as they are.
		boolean hair = product != a && number(product, 10).equals(number(a, 10));
		text(g, "guess * b = " + (hair ? String.valueOf(step.guess) : number(step.guess, 10)) + " * " + number(b, 10)
				+ " = " + (hair ? String.valueOf(product) : number(product, 10))
				+ ", " + compare + " a = " + number(a, 10), CODE_BOLD, product > a ? RED : INK, 20, h - 34);
		text(g, explain(step), BOLD, INK, 20, h - 12);
	}

	private String explain(Step step) {
		String by = number(size(step.level), 3);
		switch (step.kind) {
		case OVER:
			if (Math.abs(step.guess * b - a) < 1e-9 * a) {
				// 0.1 has no exact binary double, so ten of them added up miss a by a hair and the count runs past.
				return "It should be exact, but adding " + by + " drifted to " + step.guess + ": too far, step back.";
			}
			return "Too far: step back " + by + " and count by " + number(size(step.level + 1), 3) + " from there.";
		case BACK:
			return "Back to " + number(step.guess, 10) + ": from here it counts by " + number(size(step.level + 1), 3)
					+ ", a tenfold zoom.";
		case DONE:
			if (step.guess * b == a) {
				return "Exactly a: divide returns " + number(step.guess, 10) + " after " + shown + " steps.";
			}
			return "That step back was under 0.01, so it stops: " + number(step.guess, 10) + " (real "
					+ number(a / b, 10) + ").";
		default:
			return "Step " + shown + ": add " + by + ". Still under a, keep counting.";
		}
	}
}
