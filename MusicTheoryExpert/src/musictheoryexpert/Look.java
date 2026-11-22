package musictheoryexpert;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;

/** The colors, fonts and keyboard drawing the Ear training and Songwriter tabs share. */
final class Look {
	static final Color BACKGROUND = new Color(24, 26, 34);
	static final Color CARD = new Color(38, 41, 54);
	static final Color CARD_HOVER = new Color(52, 56, 72);
	static final Color SELECTED = new Color(62, 70, 104);
	static final Color TEXT = new Color(236, 237, 242);
	static final Color SOFT = new Color(150, 155, 172);
	static final Color DIM = new Color(92, 96, 112);
	static final Color GOLD = new Color(255, 196, 60);
	static final Color BLUE = new Color(86, 132, 255);
	static final Color BLUE_HOVER = new Color(116, 156, 255);
	static final Color RIGHT = new Color(64, 186, 108);
	static final Color WRONG = new Color(226, 86, 86);
	private static final Color WHITE_KEY = new Color(246, 246, 242);
	private static final Color BLACK_KEY = new Color(16, 16, 20);

	/** The keyboard runs from C3 to C7, which holds every note either tab plays. */
	static final int LOW_NOTE = 48, HIGH_NOTE = 96;

	/** Tabs narrower than this (a phone held upright, in the browser) stack their parts in one column. */
	static final int NARROW = 600;

	private Look() {
	}

	static boolean isBlack(int note) {
		int pitch = note % 12;
		return pitch == 1 || pitch == 3 || pitch == 6 || pitch == 8 || pitch == 10;
	}

	/** A narrow keyboard (see {@link #twoRows}) splits at C5: C5 to C7 on top, C3 to B4 under it. */
	static final int SPLIT = 72;
	private static final int ROW_GAP = 6;

	/**
	 * True when a narrow tab's keyboard is two rows: on a phone held upright four octaves in one row leave keys about
	 * 12 px wide, too thin for a finger; two rows of two octaves make them twice as wide. Too short a box keeps one row.
	 */
	static boolean twoRows(boolean narrow, int height) {
		return narrow && height >= 70;
	}

	private static int whites(int low, int high) {
		int whites = 0;
		for (int note = low; note <= high; note++) {
			whites += isBlack(note) ? 0 : 1;
		}
		return whites;
	}

	/**
	 * The keyboard from C3 to C7 in the given box. {@code colors[note]} paints a key in that color (a white key's
	 * color is used as is; give black keys a darker one); null leaves the key plain. A {@code narrow} tab's keyboard
	 * is two rows (see {@link #twoRows}).
	 */
	static void paintKeyboard(Graphics2D g, int x, int y, int width, int height, boolean narrow, Color[] colors) {
		paintKeyboard(g, x, y, width, height, narrow, colors, null);
	}

	/**
	 * As above, and a key lit by {@code colors} is drawn pressed down with {@code names[note]} written on it (for
	 * example "G" or "Bb"), so the notes sounding read off the keys. {@code names} may be null.
	 */
	static void paintKeyboard(Graphics2D g, int x, int y, int width, int height, boolean narrow, Color[] colors,
			String[] names) {
		if (twoRows(narrow, height)) {
			int row = (height - ROW_GAP) / 2, columns = whites(SPLIT, HIGH_NOTE);
			paintRow(g, x, y, width, row, SPLIT, HIGH_NOTE, columns, colors, names);
			paintRow(g, x, y + row + ROW_GAP, width, row, LOW_NOTE, SPLIT - 1, columns, colors, names);
		} else {
			paintRow(g, x, y, width, height, LOW_NOTE, HIGH_NOTE, whites(LOW_NOTE, HIGH_NOTE), colors, names);
		}
	}

	private static final Color WHITE_TOP = new Color(218, 218, 212), WHITE_LIP = new Color(206, 206, 198);
	private static final Color BLACK_FACE = new Color(66, 66, 76), BLACK_FRONT = new Color(4, 4, 6);
	private static final Color SHADOW = new Color(0, 0, 0, 120), NO_SHADOW = new Color(0, 0, 0, 0);
	private static final int LIP = 4;

	/**
	 * The keys {@code low} to {@code high} (both white) from the left of a row {@code columns} white keys wide. A key
	 * at rest has a light front lip (white) or a sheen (black) and a short shadow under the rail; a lit key is pressed
	 * down: flat in its color, no lip, a deeper shadow, its name on it.
	 */
	private static void paintRow(Graphics2D g, int x, int y, int width, int height, int low, int high, int columns,
			Color[] colors, String[] names) {
		int keyWidth = Math.max(4, width / columns);
		int left = x + (width - keyWidth * columns) / 2;
		int white = 0;
		Paint plain = g.getPaint();
		for (int note = low; note <= high; note++) {
			if (isBlack(note)) {
				continue;
			}
			int kx = left + white * keyWidth;
			boolean lit = colors[note] != null;
			if (lit) {
				g.setColor(colors[note]);
				g.fillRoundRect(kx + 1, y, keyWidth - 2, height, 6, 6);
			} else {
				g.setPaint(new GradientPaint(0, y, WHITE_TOP, 0, y + height * 2 / 3, WHITE_KEY));
				g.fillRoundRect(kx + 1, y, keyWidth - 2, height, 6, 6);
				g.setPaint(plain);
				g.setColor(WHITE_LIP);
				g.fillRoundRect(kx + 1, y + height - LIP, keyWidth - 2, LIP, 6, 6);
			}
			paintShadow(g, kx + 1, y, keyWidth - 2, lit ? 10 : 5, plain);
			String name = lit && names != null ? names[note] : null;
			if (name != null) {
				paintName(g, name, colors[note], kx + 1, y + height - 7, keyWidth - 2);
			} else if (note % 12 == 0) {
				g.setColor(DIM);
				g.setFont(new Font("SansSerif", Font.PLAIN, 10));
				g.drawString("C" + (note / 12 - 1), kx + 3, y + height - 6);
			}
			white++;
		}
		white = 0;
		int blackWidth = keyWidth * 3 / 5, blackHeight = height * 3 / 5;
		for (int note = low; note <= high; note++) {
			if (!isBlack(note)) {
				white++;
				continue;
			}
			int kx = left + white * keyWidth - blackWidth / 2;
			boolean lit = colors[note] != null;
			g.setColor(lit ? colors[note] : BLACK_FRONT);
			g.fillRoundRect(kx, y, blackWidth, blackHeight, 4, 4);
			if (!lit && blackWidth > 6) {
				// The top of a key at rest catches the light toward its front edge.
				g.setPaint(new GradientPaint(0, y, BLACK_KEY, 0, y + blackHeight - LIP, BLACK_FACE));
				g.fillRoundRect(kx + 2, y, blackWidth - 4, blackHeight - LIP - 1, 3, 3);
				g.setPaint(plain);
			}
			paintShadow(g, kx, y, blackWidth, lit ? 8 : 4, plain);
			String name = lit && names != null ? names[note] : null;
			if (name != null && !paintName(g, name, colors[note], kx, y + blackHeight - 5, blackWidth)
					&& height - blackHeight >= 30) {
				// Too thin a key for its name: write it on the white keys just under it.
				paintName(g, name, WHITE_KEY, kx - keyWidth, y + blackHeight + 12, blackWidth + 2 * keyWidth);
			}
		}
	}

	/** The shadow the rail over the keys throws down the top {@code depth} px of a key. */
	private static void paintShadow(Graphics2D g, int x, int y, int width, int depth, Paint plain) {
		g.setPaint(new GradientPaint(0, y, SHADOW, 0, y + depth, NO_SHADOW));
		g.fillRect(x, y, width, depth);
		g.setPaint(plain);
	}

	/**
	 * {@code name} centered on a key {@code width} wide, dark on a light key and white on a dark one. False, and
	 * nothing drawn, when it cannot fit.
	 */
	private static boolean paintName(Graphics2D g, String name, Color key, int x, int baseline, int width) {
		for (int size = 11; size >= 8; size--) {
			Font font = new Font("SansSerif", Font.BOLD, size);
			int w = g.getFontMetrics(font).stringWidth(name);
			if (w <= width - 2) {
				boolean light = key.getRed() * 299 + key.getGreen() * 587 + key.getBlue() * 114 > 150000;
				g.setFont(font);
				g.setColor(light ? BACKGROUND : Color.WHITE);
				g.drawString(name, x + (width - w) / 2, baseline);
				return true;
			}
		}
		return false;
	}

	/** The key of {@link #paintKeyboard} drawn in the same box under the point ({@code px}, {@code py}), or -1. */
	static int keyAt(int x, int y, int width, int height, boolean narrow, int px, int py) {
		if (twoRows(narrow, height)) {
			int row = (height - ROW_GAP) / 2, columns = whites(SPLIT, HIGH_NOTE);
			int top = rowKeyAt(x, y, width, row, SPLIT, HIGH_NOTE, columns, px, py);
			return top >= 0 ? top : rowKeyAt(x, y + row + ROW_GAP, width, row, LOW_NOTE, SPLIT - 1, columns, px, py);
		}
		return rowKeyAt(x, y, width, height, LOW_NOTE, HIGH_NOTE, whites(LOW_NOTE, HIGH_NOTE), px, py);
	}

	private static int rowKeyAt(int x, int y, int width, int height, int low, int high, int columns, int px, int py) {
		if (py < y || py >= y + height) {
			return -1;
		}
		int keyWidth = Math.max(4, width / columns);
		int left = x + (width - keyWidth * columns) / 2;
		int blackWidth = keyWidth * 3 / 5;
		int white = 0;
		int under = -1;
		for (int note = low; note <= high; note++) {
			if (isBlack(note)) {
				int kx = left + white * keyWidth - blackWidth / 2;
				if (py < y + height * 3 / 5 && px >= kx && px < kx + blackWidth) {
					return note;
				}
			} else {
				int kx = left + white * keyWidth;
				if (px >= kx && px < kx + keyWidth) {
					under = note;
				}
				white++;
			}
		}
		return under;
	}

	/** A SansSerif font of {@code style}, shrunk from {@code size} until {@code text} fits in {@code width}. */
	static Font fitFont(Graphics2D g, String text, int style, int size, int width) {
		Font font = new Font("SansSerif", style, size);
		while (size > 10 && g.getFontMetrics(font).stringWidth(text) > width) {
			font = new Font("SansSerif", style, --size);
		}
		return font;
	}

	/** {@code text} cut short with "..." to fit in {@code width} at the current font. */
	static String fit(Graphics2D g, String text, int width) {
		FontMetrics metrics = g.getFontMetrics();
		if (metrics.stringWidth(text) <= width) {
			return text;
		}
		while (text.length() > 1 && metrics.stringWidth(text + "...") > width) {
			text = text.substring(0, text.length() - 1);
		}
		return text + "...";
	}

	/** A treble clef drawn from arcs in the current color: the curl round the G line, the stem, and the hook at the foot. */
	static void paintTrebleClef(Graphics2D g, int x, int top, int gap) {
		g.setStroke(new BasicStroke(2f));
		int gLine = top + 3 * gap;
		g.drawArc(x - gap, gLine - gap, 2 * gap, 2 * gap, 90, 300);
		g.drawArc(x - gap - 3, gLine - 2 * gap, 2 * gap + 9, 3 * gap, 300, 220);
		g.drawLine(x + 2, top - 2 * gap + 2, x - 1, top + 5 * gap + 2);
		g.drawArc(x - 2, top - 2 * gap - 1, gap + 2, 2 * gap, 200, -220);
		g.fillOval(x - gap + 1, top + 5 * gap - 2, 5, 5);
		g.setStroke(new BasicStroke(1f));
	}

	/** A bass clef in the current color: the dot on the F line, the curve and the two dots either side of it. */
	static void paintBassClef(Graphics2D g, int x, int top, int gap) {
		g.fillOval(x - 1, top + gap - 3, 7, 7);
		g.setStroke(new BasicStroke(2f));
		g.drawArc(x, top, 2 * gap + 2, 2 * gap + 4, 180, -200);
		g.drawLine(x + 2 * gap + 1, top + gap + 6, x + 2, top + 4 * gap - 2);
		g.setStroke(new BasicStroke(1f));
		g.fillOval(x + 2 * gap + 8, top + gap / 2 - 2, 4, 4);
		g.fillOval(x + 2 * gap + 8, top + gap + gap / 2 - 2, 4, 4);
	}
}
