package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

/**
 * Sine and cosine: plots {@link MyMath#estimateSine} or {@link MyMath#estimateCosine} with 1 to 10 Taylor terms
 * against the real curve, the fewer-term sums fading behind. Pointing at the plot picks x; the side lists each term
 * at that x, their sum and its error. It opens on the original program's sin(pi/4) with 4 terms. At x, a ladder of
 * arrows adds the terms one by one (blue up for a plus term, orange down for a minus), climbing to the red sum; a
 * green band shows how far out the sum stays within 0.01 of the curve, and a new term pulls the red curve over.
 */
class TaylorPage extends Page {
	private static final int MAX_TERMS = 10;
	private static final int LEFT = 30;
	/** Narrow, the plot takes the lab's width and the terms go under it, in two columns. */
	private static final int TOP = NARROW ? 92 : 74;
	private static final int PLOT_W = NARROW ? 420 : 450;
	private static final int PLOT_H = NARROW ? 240 : 270;
	private static final double X_RANGE = 2 * Math.PI;
	private static final double Y_RANGE = 2;
	static final Color PLUS = new Color(52, 104, 190);
	static final Color MINUS = new Color(222, 126, 36);
	private static final Color BAND = new Color(46, 125, 80, 34);
	/** Ticks a change of terms takes to pull the red curve over (about 300 ms). */
	private static final int MORPH_TICKS = 10;

	private boolean sine = true;
	private int terms = 4;
	private double x = Math.PI / 4;
	private boolean playing;
	private int wait;
	/** The terms the red curve is leaving while a change of terms plays, and how far it has got (1: there). */
	private int from = 4;
	private double morph = 1;

	private final Button play;
	private final Button which;

	TaylorPage() {
		super("Sine & cosine");
		play = button("Play", ' ', this::togglePlay);
		button("Fewer terms", '-', () -> setTerms(terms - 1));
		button("More terms", '+', () -> setTerms(terms + 1));
		which = button("Cosine", 'c', this::toggle);
		button("x = pi/4", 'p', () -> x = Math.PI / 4);
	}

	private void setTerms(int count) {
		goTo(count);
		playing = false;
		play.label = "Play";
	}

	/** Changes the terms, pulling the red curve over from the old sum unless reduced motion is on. */
	private void goTo(int count) {
		count = Math.max(1, Math.min(MAX_TERMS, count));
		if (count == terms) {
			return;
		}
		from = terms;
		terms = count;
		morph = Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("oldjava.reducedMotion")) ? 1 : 0;
	}

	/** The red curve's height at x: the sum of the terms, or on its way there from the old sum. */
	private double shown(double at) {
		double to = estimate(at, terms);
		return morph >= 1 ? to : estimate(at, from) + (to - estimate(at, from)) * ease();
	}

	private double ease() {
		return morph * morph * (3 - 2 * morph);
	}

	private void toggle() {
		sine = !sine;
		which.label = sine ? "Cosine" : "Sine";
	}

	private void togglePlay() {
		playing = !playing;
		play.label = playing ? "Pause" : "Play";
		if (playing) {
			goTo(1);
			wait = 0;
		}
	}

	private double estimate(double at, int count) {
		return sine ? MyMath.estimateSine(at, count) : MyMath.estimateCosine(at, count);
	}

	private double exact(double at) {
		return sine ? Math.sin(at) : Math.cos(at);
	}

	@Override
	boolean key(KeyEvent event) {
		if (event.getKeyChar() == '=' || event.getKeyCode() == KeyEvent.VK_UP) {
			setTerms(terms + 1);
		} else if (event.getKeyCode() == KeyEvent.VK_DOWN) {
			setTerms(terms - 1);
		} else if (event.getKeyCode() == KeyEvent.VK_LEFT) {
			x = Math.max(-X_RANGE, x - Math.PI / 32);
		} else if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
			x = Math.min(X_RANGE, x + Math.PI / 32);
		} else {
			return false;
		}
		return true;
	}

	@Override
	void hover(int px, int py) {
		if (px >= LEFT && px <= LEFT + PLOT_W && py >= TOP && py <= TOP + PLOT_H) {
			x = toX(px);
		}
	}

	@Override
	void click(int px, int py) {
		hover(px, py);
	}

	@Override
	boolean tick() {
		boolean moved = morph < 1;
		morph = Math.min(1, morph + 1.0 / MORPH_TICKS);
		if (playing && ++wait >= 25) {
			wait = 0;
			if (terms < MAX_TERMS) {
				goTo(terms + 1);
			} else {
				playing = false;
				play.label = "Play";
			}
			return true;
		}
		return moved;
	}

	private static double toX(int px) {
		return (px - LEFT) * 2 * X_RANGE / PLOT_W - X_RANGE;
	}

	private static double toPx(double at) {
		return LEFT + (at + X_RANGE) * PLOT_W / (2 * X_RANGE);
	}

	private static double toPy(double y) {
		return TOP + PLOT_H / 2.0 - y * PLOT_H / (2 * Y_RANGE);
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		String name = sine ? "sine" : "cosine";
		text(g, "Taylor series for " + name, TITLE, INK, 20, 32);
		paragraph(g, "MyMath.estimate" + (sine ? "Sine" : "Cosine") + " with " + terms + " term" + (terms == 1 ? "" : "s")
				+ " (red) against the real curve (green). " + (NARROW ? "Tap" : "Point at") + " the plot to pick x.", TEXT, PENCIL, 20, 54, w - 40, 0, 16, 2);
		paintAxes(g);
		paintBand(g);
		for (int count = 1; count < terms; count++) {
			int fade = 40 + 120 * count / terms;
			paintCurve(g, count, new Color(31, 58, 104, fade), 1f);
		}
		paintCurve(g, 0, GREEN, 2f);
		paintCurve(g, -1, RED, 2.5f);
		int px = (int) Math.round(toPx(x));
		g.setColor(PENCIL);
		g.drawLine(px, TOP, px, TOP + PLOT_H);
		paintLadder(g, px);
		int py = (int) Math.round(toPy(shown(x)));
		if (py >= TOP && py <= TOP + PLOT_H) {
			g.setColor(RED);
			g.fillOval(px - 4, py - 4, 8, 8);
		}
		paintTerms(g, w);
		paintCaption(g, h);
	}

	private void paintAxes(Graphics2D g) {
		g.setColor(new Color(255, 255, 255, 200));
		g.fillRect(LEFT, TOP, PLOT_W, PLOT_H);
		g.setColor(PENCIL);
		g.drawRect(LEFT, TOP, PLOT_W, PLOT_H);
		int zeroY = (int) toPy(0);
		g.drawLine(LEFT, zeroY, LEFT + PLOT_W, zeroY);
		String[] labels = { "-2pi", "-pi", "0", "pi", "2pi" };
		for (int i = 0; i < labels.length; i++) {
			int tx = (int) toPx((i - 2) * Math.PI);
			g.setColor(GRID);
			g.drawLine(tx, TOP, tx, TOP + PLOT_H);
			centered(g, labels[i], SMALL, PENCIL, tx, TOP + PLOT_H + 14);
		}
		for (int y = -1; y <= 1; y += 2) {
			int ty = (int) toPy(y);
			g.setColor(GRID);
			g.drawLine(LEFT, ty, LEFT + PLOT_W, ty);
			text(g, String.valueOf(y), SMALL, PENCIL, LEFT - 6 - width(g, String.valueOf(y), SMALL), ty + 4);
		}
	}

	/** Shades the stretch of x, either side of 0, where the sum stays within 0.01 of the real function. */
	private void paintBand(Graphics2D g) {
		double reach = reach();
		int left = (int) Math.round(toPx(-reach));
		int right = (int) Math.round(toPx(reach));
		g.setColor(BAND);
		g.fillRect(left, TOP + 1, right - left, PLOT_H - 1);
		g.setColor(new Color(46, 125, 80, 120));
		g.drawLine(left, TOP + 1, left, TOP + PLOT_H - 1);
		g.drawLine(right, TOP + 1, right, TOP + PLOT_H - 1);
		String label = "within 0.01";
		if (right - left > width(g, label, SMALL) + 8) {
			centered(g, label, SMALL, GREEN, (left + right) / 2, TOP + 14);
		}
	}

	/**
	 * The terms at x as a ladder of arrows: each starts where the sum of the ones before it ends, so the rungs climb
	 * and fall onto the sum. It steps away from x toward the middle of the plot, and the newest term grows in while the
	 * red curve is pulled over.
	 */
	private void paintLadder(Graphics2D g, int px) {
		Graphics2D c = (Graphics2D) g.create();
		c.clipRect(LEFT + 1, TOP + 1, PLOT_W - 1, PLOT_H - 1);
		int step = px < LEFT + PLOT_W / 2 ? 7 : -7;
		boolean growing = morph < 1 && terms == from + 1;
		double sum = 0;
		for (int i = 0; i < terms; i++) {
			double next = estimate(x, i + 1);
			if (growing && i == terms - 1) {
				next = sum + (next - sum) * ease();
			}
			int ax = px + step * (i + 1);
			int y1 = (int) Math.round(clamp(toPy(sum)));
			int y2 = (int) Math.round(clamp(toPy(next)));
			c.setColor(new Color(120, 128, 140, 150));
			c.drawLine(ax - step, y1, ax, y1);
			c.setColor(next >= sum ? PLUS : MINUS);
			c.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			c.drawLine(ax, y1, ax, y2);
			c.setStroke(new BasicStroke(1f));
			if (Math.abs(y2 - y1) >= 6 && y2 > TOP + 1 && y2 < TOP + PLOT_H - 1) {
				int back = y2 > y1 ? -5 : 5;
				c.fillPolygon(new int[] { ax - 4, ax + 4, ax }, new int[] { y2 + back, y2 + back, y2 }, 3);
			}
			sum = next;
		}
		c.dispose();
	}

	/** A plot row kept a little past the plot's edges, so an arrow off the plot still runs to its border. */
	private static double clamp(double py) {
		return Math.max(TOP - 2, Math.min(TOP + PLOT_H + 2, py));
	}

	/** Plots the sum of count terms, the real function for count 0, or the red curve as shown for -1, clipped to the plot. */
	private void paintCurve(Graphics2D g, int count, Color color, float thickness) {
		g.setColor(color);
		g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		double lastX = 0;
		double lastY = 0;
		for (int px = LEFT; px <= LEFT + PLOT_W; px += 2) {
			double at = toX(px);
			double y = count == 0 ? exact(at) : count < 0 ? shown(at) : estimate(at, count);
			if (px > LEFT) {
				segment(g, lastX, lastY, px, y);
			}
			lastX = px;
			lastY = y;
		}
		g.setStroke(new BasicStroke(1f));
	}

	/** Draws the part of the segment from (x1, y1) to (x2, y2), in pixels across and plot units up, inside the plot. */
	private static void segment(Graphics2D g, double x1, double y1, double x2, double y2) {
		double low = Math.max(Math.min(y1, y2), -Y_RANGE);
		double high = Math.min(Math.max(y1, y2), Y_RANGE);
		if (low > high) {
			return;
		}
		double ta = y1 == y2 ? 0 : (low - y1) / (y2 - y1);
		double tb = y1 == y2 ? 1 : (high - y1) / (y2 - y1);
		double t1 = Math.max(0, Math.min(ta, tb));
		double t2 = Math.min(1, Math.max(ta, tb));
		g.drawLine((int) Math.round(x1 + (x2 - x1) * t1), (int) Math.round(toPy(y1 + (y2 - y1) * t1)),
				(int) Math.round(x1 + (x2 - x1) * t2), (int) Math.round(toPy(y1 + (y2 - y1) * t2)));
	}

	private void paintTerms(Graphics2D g, int w) {
		int left = NARROW ? LEFT : LEFT + PLOT_W + 24;
		int y = NARROW ? TOP + PLOT_H + 40 : TOP + 6;
		text(g, "at x = " + number(x, 5), BOLD, INK, left, y);
		y += 22;
		int first = sine ? 1 : 0;
		// Narrow: five terms a column, two columns across.
		int perColumn = NARROW ? 5 : MAX_TERMS;
		int columnW = NARROW ? (w - 20 - left - 24) / 2 : w - 20 - left;
		for (int i = 0; i < terms; i++) {
			int power = 2 * i + first;
			double value = (i % 2 == 0 ? 1 : -1) * MyMath.pow(x, power) / MyMath.factorial(power);
			String term = (i % 2 == 0 ? (i == 0 ? "  " : "+ ") : "- ") + (power == 0 ? "1" : "x^" + power + "/" + power + "!");
			int tx = left + i / perColumn * (columnW + 24);
			int ty = y + i % perColumn * 18;
			text(g, term, CODE, INK, tx, ty);
			String shown = number(value, 4);
			// The value in its arrow's color, so a row and its rung on the plot read together.
			text(g, shown, CODE, value >= 0 ? PLUS : MINUS, tx + columnW - width(g, shown, CODE), ty);
		}
		y += Math.min(terms, perColumn) * 18;
		g.setColor(PENCIL);
		g.drawLine(left, y - 12, w - 20, y - 12);
		y += 4;
		double sum = estimate(x, terms);
		double error = Math.abs(sum - exact(x));
		row(g, "sum", number(sum, 8), RED, left, w, y);
		row(g, sine ? "sin x" : "cos x", number(exact(x), 8), GREEN, left, w, y + 18);
		row(g, "error", error < 1e-15 ? "0" : number(error, 2), error < 1e-6 ? GREEN : RED, left, w, y + 36);
		paragraph(g, "The arrows at x add the terms in turn: blue up, orange down, ending on the red dot.", SMALL, PENCIL, left, y + 58,
				w - 20 - left, 0, 14, 3);
	}

	private static void row(Graphics2D g, String name, String value, Color color, int left, int w, int y) {
		text(g, name, BOLD, color, left, y);
		text(g, value, CODE_BOLD, color, w - 20 - width(g, value, CODE_BOLD), y);
	}

	private void paintCaption(Graphics2D g, int h) {
		String caption = Math.abs(x - Math.PI / 4) < 1e-12 && terms == 4
				? "The original program printed this: " + (sine ? "sin" : "cos") + "(pi/4) ~= " + estimate(x, terms)
				: "More terms reach further: with " + terms + ", the sum stays within 0.01 of the curve out to x = "
						+ number(reach(), 3) + ".";
		if (NARROW) {
			paragraph(g, caption, BOLD, INK, 20, h - 32, LabPanel.WIDTH - 40, 0, 18, 2);
		} else {
			text(g, caption, BOLD, INK, 20, h - 14);
		}
	}

	/** The largest x from 0 out to which the sum stays within 0.01 of the real function. */
	private double reach() {
		double at = 0;
		while (at < X_RANGE && Math.abs(estimate(at + 0.01, terms) - exact(at + 0.01)) < 0.01) {
			at += 0.01;
		}
		return at;
	}
}
