package musictheoryexpert;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

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

	private Look() {
	}

	static boolean isBlack(int note) {
		int pitch = note % 12;
		return pitch == 1 || pitch == 3 || pitch == 6 || pitch == 8 || pitch == 10;
	}

	/**
	 * The keyboard from C3 to C7 in the given box. {@code colors[note]} paints a key in that color (a white key's
	 * color is used as is; give black keys a darker one); null leaves the key plain.
	 */
	static void paintKeyboard(Graphics2D g, int x, int y, int width, int height, Color[] colors) {
		int whites = 0;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			whites += isBlack(note) ? 0 : 1;
		}
		int keyWidth = Math.max(4, width / whites);
		int left = x + (width - keyWidth * whites) / 2;
		int white = 0;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			if (isBlack(note)) {
				continue;
			}
			int kx = left + white * keyWidth;
			g.setColor(colors[note] != null ? colors[note] : WHITE_KEY);
			g.fillRoundRect(kx + 1, y, keyWidth - 2, height, 6, 6);
			if (note % 12 == 0) {
				g.setColor(DIM);
				g.setFont(new Font("SansSerif", Font.PLAIN, 10));
				g.drawString("C" + (note / 12 - 1), kx + 3, y + height - 6);
			}
			white++;
		}
		white = 0;
		int blackWidth = keyWidth * 3 / 5;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			if (!isBlack(note)) {
				white++;
				continue;
			}
			int kx = left + white * keyWidth - blackWidth / 2;
			g.setColor(colors[note] != null ? colors[note] : BLACK_KEY);
			g.fillRoundRect(kx, y, blackWidth, height * 3 / 5, 4, 4);
		}
	}

	/** The key of {@link #paintKeyboard} drawn in the same box under the point ({@code px}, {@code py}), or -1. */
	static int keyAt(int x, int y, int width, int height, int px, int py) {
		if (py < y || py >= y + height) {
			return -1;
		}
		int whites = 0;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
			whites += isBlack(note) ? 0 : 1;
		}
		int keyWidth = Math.max(4, width / whites);
		int left = x + (width - keyWidth * whites) / 2;
		int blackWidth = keyWidth * 3 / 5;
		int white = 0;
		int under = -1;
		for (int note = LOW_NOTE; note <= HIGH_NOTE; note++) {
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
}
