package numberguess;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;

import javax.swing.Timer;

/**
 * Draws the card trick on the board: the card being asked about on the left and the six cards on the right (answered
 * ones face up with Yes or No, the rest face down). When all six are answered the last card flips over to show your
 * number in a burst of sparks, and How it works lists the key numbers that add up to it.
 */
final class MagicView {
	private static final Color[] INKS = { new Color(206, 64, 60), new Color(222, 130, 30), new Color(46, 150, 84),
			new Color(52, 110, 206), new Color(134, 74, 186), new Color(20, 140, 150) };
	private static final Color FACE = new Color(250, 245, 232);
	private static final Color FACE_TEXT = new Color(70, 64, 60);
	private static final Color BACK = new Color(40, 48, 92);
	private static final Color BACK_PATTERN = new Color(92, 100, 150);
	private static final Color GOLD = new Color(255, 198, 64);
	private static final Color YES = new Color(46, 150, 84);
	private static final Color NO = new Color(150, 146, 140);
	private static final Color SPARK = new Color(255, 214, 90);

	private static final int MAIN_WIDTH = 380, GAP = 16;
	private static final int DEAL_MS = 260, HOLD_MS = 420, FLIP_MS = HOLD_MS + 520, SPARKS_MS = 1400;

	private final Timer timer;
	private long dealtAt;
	private boolean revealing;
	private boolean explaining;

	MagicView(Runnable repaint) {
		timer = new Timer(16, event -> {
			if (elapsed() > (revealing ? FLIP_MS + SPARKS_MS : DEAL_MS)) {
				((Timer) event.getSource()).stop();
			}
			repaint.run();
		});
	}

	private long elapsed() {
		return System.currentTimeMillis() - dealtAt;
	}

	/** A card was just dealt ({@code reveal} false) or the last answer was given and the number is shown. */
	void dealt(boolean reveal) {
		dealtAt = System.currentTimeMillis();
		revealing = reveal;
		explaining = false;
		timer.restart();
	}

	/** True while the last card is still turning over, before the number shows. */
	boolean isFlipping() {
		return revealing && elapsed() < FLIP_MS;
	}

	boolean isExplaining() {
		return explaining;
	}

	void toggleExplaining() {
		explaining = !explaining;
	}

	static Color ink(int index) {
		return INKS[index % INKS.length];
	}

	void paint(Graphics2D g, MagicCards cards, Rectangle area) {
		Rectangle main = new Rectangle(area.x, area.y, MAIN_WIDTH, area.height);
		if (!cards.isDone()) {
			double t = Math.min(1, elapsed() / (double) DEAL_MS);
			int drop = (int) Math.round(10 * (1 - t) * (1 - t));
			paintCard(g, cards, cards.current(), new Rectangle(main.x, main.y + drop, main.width, main.height));
		} else if (cards.number() == 0) {
			paintBack(g, main, "?");
		} else if (explaining) {
			paintHow(g, cards, main);
		} else {
			paintReveal(g, cards.number(), main);
		}
		paintStack(g, cards, new Rectangle(main.x + main.width + GAP, area.y, area.width - main.width - GAP, area.height));
	}

	/** Card {@code index} face up: a band in its ink, then its 32 numbers in 4 rows of 8. */
	private void paintCard(Graphics2D g, MagicCards cards, int index, Rectangle r) {
		paintFace(g, r, ink(index), "CARD " + (index + 1));
		List<Integer> numbers = cards.numbers(index);
		int columns = 8, rows = numbers.size() / columns;
		int top = r.y + 34, cellWidth = (r.width - 16) / columns, cellHeight = (r.height - 40) / rows;
		g.setFont(new Font("SansSerif", Font.BOLD, 19));
		FontMetrics metrics = g.getFontMetrics();
		g.setColor(ink(index).darker());
		for (int i = 0; i < numbers.size(); i++) {
			String text = Integer.toString(numbers.get(i));
			int x = r.x + 8 + (i % columns) * cellWidth + (cellWidth - metrics.stringWidth(text)) / 2;
			int y = top + (i / columns) * cellHeight + (cellHeight + metrics.getAscent()) / 2 - 3;
			g.drawString(text, x, y);
		}
	}

	/** A cream card with a coloured band along the top carrying {@code title}. */
	private static void paintFace(Graphics2D g, Rectangle r, Color ink, String title) {
		g.setColor(new Color(0, 0, 0, 70));
		g.fillRoundRect(r.x + 3, r.y + 4, r.width, r.height, 16, 16);
		g.setColor(FACE);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
		g.setColor(ink);
		g.fillRoundRect(r.x, r.y, r.width, 26, 16, 16);
		g.fillRect(r.x, r.y + 14, r.width, 12);
		if (r.width > 40) {
			g.setColor(Color.WHITE);
			g.setFont(new Font("SansSerif", Font.BOLD, 12));
			g.drawString(title, r.x + 10, r.y + 18);
			Board.paintStar(g, r.x + r.width - 16, r.y + 13, 6, new Color(255, 255, 255, 190));
		}
	}

	/** Face down: dark blue with a gold diamond pattern, and {@code mark} in the middle if not empty. */
	private static void paintBack(Graphics2D g, Rectangle r, String mark) {
		g.setColor(new Color(0, 0, 0, 70));
		g.fillRoundRect(r.x + 3, r.y + 4, r.width, r.height, 16, 16);
		g.setColor(BACK);
		g.fillRoundRect(r.x, r.y, r.width, r.height, 16, 16);
		g.setColor(BACK_PATTERN);
		int step = 14;
		for (int y = r.y + 10; y + 4 <= r.y + r.height - 6; y += step) {
			for (int x = r.x + 10 + ((y - r.y) / step % 2) * step / 2; x + 4 <= r.x + r.width - 6; x += step) {
				g.fillPolygon(new int[] { x, x + 4, x, x - 4 }, new int[] { y - 4, y, y + 4, y }, 4);
			}
		}
		if (r.width > 12) {
			g.setColor(GOLD);
			g.drawRoundRect(r.x + 4, r.y + 4, r.width - 9, r.height - 9, 12, 12);
		}
		if (!mark.isEmpty() && r.width > 60) {
			g.setFont(new Font("SansSerif", Font.BOLD, Math.min(96, r.height / 2)));
			FontMetrics metrics = g.getFontMetrics();
			g.setColor(GOLD);
			g.drawString(mark, r.x + (r.width - metrics.stringWidth(mark)) / 2, r.y + (r.height + metrics.getAscent()) / 2 - 8);
		}
	}

	/** A pause on the back, then the last card turns over (the back narrows, the face widens) and sparks fly. */
	private void paintReveal(Graphics2D g, int number, Rectangle main) {
		long t = revealing ? elapsed() : FLIP_MS + SPARKS_MS;
		int half = (FLIP_MS - HOLD_MS) / 2;
		long turn = Math.max(0, t - HOLD_MS);
		double scale = turn < half ? 1 - turn / (double) half : Math.min(1, (turn - half) / (double) half);
		int width = Math.max(2, (int) Math.round(main.width * scale));
		Rectangle r = new Rectangle(main.x + (main.width - width) / 2, main.y, width, main.height);
		if (turn < half) {
			paintBack(g, r, "");
			return;
		}
		paintFace(g, r, GOLD.darker(), "YOUR NUMBER");
		String text = Integer.toString(number);
		g.setFont(new Font("SansSerif", Font.BOLD, 120));
		FontMetrics metrics = g.getFontMetrics();
		int textWidth = metrics.stringWidth(text);
		int centerX = main.x + main.width / 2, centerY = main.y + 26 + (main.height - 26) / 2;
		if (textWidth <= width) {
			g.setColor(INKS[0]);
			g.drawString(text, centerX - textWidth / 2, centerY + metrics.getAscent() / 2 - 10);
		}
		if (t > FLIP_MS && t < FLIP_MS + SPARKS_MS) {
			double p = (t - FLIP_MS) / (double) SPARKS_MS;
			int alpha = (int) Math.round(255 * (1 - p));
			for (int i = 0; i < 14; i++) {
				double angle = i * Math.PI * 2 / 14 + p * 0.8;
				double distance = 50 + 150 * Math.sqrt(p) * (i % 2 == 0 ? 1 : 0.7);
				int x = (int) Math.round(centerX + Math.cos(angle) * distance * 1.2);
				int y = (int) Math.round(centerY + Math.sin(angle) * distance * 0.6);
				Board.paintStar(g, x, y, (int) Math.round(9 * (1 - p)) + 3, new Color(SPARK.getRed(), SPARK.getGreen(), SPARK.getBlue(), alpha));
			}
		}
	}

	/** How it works: one row per card, biggest key first, so the digits read as your number in binary. */
	private void paintHow(Graphics2D g, MagicCards cards, Rectangle r) {
		paintFace(g, r, GOLD.darker(), "HOW IT WORKS");
		g.setFont(new Font("SansSerif", Font.PLAIN, 11));
		g.setColor(NO);
		int keyX = r.x + 14, cardX = r.x + 72, answerX = r.x + 150, digitX = r.x + r.width - 30;
		g.drawString("Key", keyX, r.y + 42);
		g.drawString("Smallest on", cardX, r.y + 42);
		g.drawString("Your number on it?", answerX, r.y + 42);
		g.drawString("Bit", digitX - 4, r.y + 42);
		int rowHeight = 22, top = r.y + 50;
		StringBuilder sum = new StringBuilder();
		StringBuilder binary = new StringBuilder();
		for (int row = 0; row < MagicCards.CARDS; row++) {
			int key = 1 << (MagicCards.CARDS - 1 - row);
			int index = indexOf(cards, key);
			boolean yes = cards.answer(index);
			int y = top + row * rowHeight;
			g.setColor(yes ? ink(index) : new Color(222, 216, 204));
			g.fillRoundRect(keyX, y, 44, rowHeight - 4, 10, 10);
			g.setFont(new Font("SansSerif", Font.BOLD, 13));
			g.setColor(yes ? Color.WHITE : NO);
			String keyText = Integer.toString(key);
			g.drawString(keyText, keyX + 22 - g.getFontMetrics().stringWidth(keyText) / 2, y + 13);
			g.setFont(new Font("SansSerif", Font.PLAIN, 12));
			g.setColor(FACE_TEXT);
			g.drawString("Card " + (index + 1), cardX, y + 13);
			g.setColor(yes ? YES : NO);
			g.drawString(yes ? "Yes: add " + key : "No", answerX, y + 13);
			g.setFont(new Font("SansSerif", Font.BOLD, 15));
			g.drawString(yes ? "1" : "0", digitX, y + 14);
			binary.append(yes ? '1' : '0');
			if (yes) {
				sum.append(sum.length() == 0 ? "" : " + ").append(key);
			}
		}
		g.setFont(new Font("SansSerif", Font.BOLD, 16));
		g.setColor(INKS[0]);
		g.drawString(sum + " = " + cards.number(), keyX, r.y + r.height - 12);
		g.setFont(new Font("SansSerif", Font.PLAIN, 12));
		g.setColor(FACE_TEXT);
		String bits = "binary " + binary.substring(binary.indexOf("1"));
		g.drawString(bits, r.x + r.width - 14 - g.getFontMetrics().stringWidth(bits), r.y + r.height - 13);
	}

	private static int indexOf(MagicCards cards, int key) {
		for (int i = 0; i < MagicCards.CARDS; i++) {
			if (cards.key(i) == key) {
				return i;
			}
		}
		throw new IllegalArgumentException("no card for " + key);
	}

	/** The six cards, two by three: answered ones face up with Yes or No, the one being asked ringed in gold. */
	private void paintStack(Graphics2D g, MagicCards cards, Rectangle r) {
		int gap = 10;
		int width = (r.width - gap) / 2, height = (r.height - 2 * gap) / 3;
		for (int i = 0; i < MagicCards.CARDS; i++) {
			Rectangle card = new Rectangle(r.x + (i % 2) * (width + gap), r.y + (i / 2) * (height + gap), width, height);
			Boolean answer = cards.answer(i);
			if (i == cards.current()) {
				paintFace(g, card, ink(i), Integer.toString(i + 1));
				g.setColor(GOLD);
				g.drawRoundRect(card.x - 2, card.y - 2, card.width + 3, card.height + 3, 18, 18);
				g.setFont(new Font("SansSerif", Font.BOLD, 22));
				g.setColor(ink(i));
				g.drawString("?", card.x + card.width / 2 - 6, card.y + 26 + (card.height - 26) / 2 + 8);
			} else if (answer == null) {
				paintBack(g, card, "");
			} else {
				paintFace(g, card, ink(i), Integer.toString(i + 1));
				int centerY = card.y + 26 + (card.height - 26) / 2;
				if (explaining) {
					g.setFont(new Font("SansSerif", Font.BOLD, 18));
					g.setColor(answer ? ink(i) : NO);
					String key = Integer.toString(cards.key(i));
					g.drawString(key, card.x + (card.width - g.getFontMetrics().stringWidth(key)) / 2, centerY + 7);
				} else {
					g.setFont(new Font("SansSerif", Font.BOLD, 14));
					String text = answer ? "Yes" : "No";
					int x = card.x + (card.width - g.getFontMetrics().stringWidth(text) - 16) / 2;
					g.setColor(answer ? YES : NO);
					paintMark(g, x + 5, centerY, answer);
					g.drawString(text, x + 16, centerY + 5);
				}
			}
		}
	}

	/** A tick for yes, a cross for no, centred on x, y. */
	private static void paintMark(Graphics2D g, int x, int y, boolean yes) {
		Stroke stroke = g.getStroke();
		g.setStroke(new BasicStroke(2.5f));
		if (yes) {
			g.drawLine(x - 5, y, x - 2, y + 4);
			g.drawLine(x - 2, y + 4, x + 5, y - 5);
		} else {
			g.drawLine(x - 4, y - 4, x + 4, y + 4);
			g.drawLine(x - 4, y + 4, x + 4, y - 4);
		}
		g.setStroke(stroke);
	}
}
