package simplealgorithms;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * One page of the Algorithm Lab: it paints its part of the graph-paper window, takes clicks and keys there, and
 * lists the buttons {@link LabPanel} draws along the bottom.
 */
abstract class Page {
	static final Color PAPER = new Color(251, 248, 240);
	static final Color GRID = new Color(214, 226, 240);
	static final Color INK = new Color(31, 58, 104);
	static final Color PENCIL = new Color(120, 128, 140);
	static final Color RED = new Color(200, 69, 58);
	static final Color GREEN = new Color(46, 125, 80);
	static final Color HIGHLIGHT = new Color(255, 226, 120);

	static final Font TITLE = new Font("Serif", Font.ITALIC | Font.BOLD, 20);
	static final Font TEXT = new Font("SansSerif", Font.PLAIN, 13);
	static final Font BOLD = new Font("SansSerif", Font.BOLD, 13);
	static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);
	static final Font CODE = new Font("Monospaced", Font.PLAIN, 13);
	static final Font CODE_BOLD = new Font("Monospaced", Font.BOLD, 13);

	/** A button in the bottom bar: its label can change (Play and Pause), and key is its keyboard shortcut. */
	static final class Button {
		String label;
		final char key;
		final Runnable action;
		final Rectangle bounds = new Rectangle();

		Button(String label, char key, Runnable action) {
			this.label = label;
			this.key = key;
			this.action = action;
		}
	}

	final String title;
	final List<Button> buttons = new ArrayList<>();

	Page(String title) {
		this.title = title;
	}

	Button button(String label, char key, Runnable action) {
		Button button = new Button(label, key, action);
		buttons.add(button);
		return button;
	}

	/** Paints the page in a w x h area whose top left is (0, 0). */
	abstract void paint(Graphics2D g, int w, int h);

	/** A click at (x, y) in the page's area. */
	void click(int x, int y) {
	}

	/** The pointer moved to (x, y) in the page's area, or left it (x and y are -1). */
	void hover(int x, int y) {
	}

	/** A key the bar's buttons did not take. Returns true if the page used it. */
	boolean key(KeyEvent event) {
		return false;
	}

	/** Called about 30 times a second while the page shows. Returns true if it needs a repaint. */
	boolean tick() {
		return false;
	}

	/** The page was hidden by another tab. */
	void hidden() {
	}

	static void text(Graphics2D g, String text, Font font, Color color, int x, int y) {
		g.setFont(font);
		g.setColor(color);
		g.drawString(text, x, y);
	}

	static void centered(Graphics2D g, String text, Font font, Color color, int centerX, int y) {
		g.setFont(font);
		g.setColor(color);
		g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, y);
	}

	static int width(Graphics2D g, String text, Font font) {
		FontMetrics metrics = g.getFontMetrics(font);
		return metrics.stringWidth(text);
	}

	/** A number with at most the given significant digits, without a trailing ".0". */
	static String number(double value, int digits) {
		if (Double.isNaN(value) || Double.isInfinite(value)) {
			return String.valueOf(value);
		}
		if (value == 0) {
			return "0";
		}
		int magnitude = (int) Math.floor(Math.log10(Math.abs(value)));
		if (magnitude < -4 || magnitude > 14) {
			// Tiny errors and huge sums: Java's own notation, like 1.2E-9.
			double scale = Math.pow(10, digits - 1 - magnitude);
			return String.valueOf(Math.round(value * scale) / scale);
		}
		// Written out by hand, so 617283950 never turns into 6.1728395E8.
		int places = Math.max(0, Math.min(digits - 1 - magnitude, 17 - magnitude));
		long scale = 1;
		for (int i = 0; i < places; i++) {
			scale *= 10;
		}
		long scaled = Math.round(Math.abs(value) * scale);
		String sign = value < 0 && scaled != 0 ? "-" : "";
		String fraction = String.valueOf(scaled % scale + scale).substring(1);
		while (fraction.endsWith("0")) {
			fraction = fraction.substring(0, fraction.length() - 1);
		}
		return sign + scaled / scale + (fraction.isEmpty() ? "" : "." + fraction);
	}
}
