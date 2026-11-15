package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * Sort by weighing: the visitor is the sorting algorithm. Boxes that look alike can only be compared two at a time
 * on a balance; a picture lays out what the weighings prove (a line from a lighter box to a heavier one) and the
 * side counts the orders still possible, which one weighing can at best halve. Once only one order is left, the
 * boxes open and the count is set against the fewest weighings that always suffice and the lab's sorts.
 */
class WeighPage extends Page {
	private static final int MIN = 3;
	private static final int MAX = 8;
	private static final int BOX = 32;
	private static final int ROW = 38;
	private static final int DIAGRAM_X = 16;
	private static final int DIAGRAM_W = 440;
	private static final int DIAGRAM_TOP = 78;
	private static final int DIAGRAM_BOTTOM = 384;
	private static final int SIDE_X = 472;
	private static final int SIDE_W = 232;
	private static final int PIVOT_Y = 92;
	private static final int ARM = 80;
	private static final Color CARD = new Color(222, 192, 142);
	private static final Color CARD_EDGE = new Color(150, 112, 66);
	private static final Color HINT = new Color(232, 146, 40);

	private final Random random = new Random();
	private Weighing weighing;
	private int count = 6;
	/** The boxes on the left and right pan, or -1. */
	private int left = -1;
	private int right = -1;
	/** True once both pans were weighed: the next pick starts a new pair. */
	private boolean weighed;
	private boolean showWeights;
	private boolean peeked;
	private int[] hint;
	private String status;
	private double[] boxX;
	private double[] boxY;
	private double tilt;

	WeighPage() {
		super("Weigh");
		button("Hint", '?', this::showHint);
		button("Weigh for me", ' ', this::weighHint);
		button("Show weights", 'w', () -> {
			showWeights = !showWeights;
			peeked |= showWeights && !weighing.sorted();
		});
		button("New boxes", 'n', () -> deal(count));
		button("Fewer", '-', () -> deal(count - 1));
		button("More", '+', () -> deal(count + 1));
		deal(count);
	}

	private void deal(int n) {
		count = Math.max(MIN, Math.min(MAX, n));
		weighing = Weighing.random(count, random);
		left = -1;
		right = -1;
		weighed = false;
		showWeights = false;
		peeked = false;
		hint = null;
		tilt = 0;
		status = "Click two boxes, or press their letters.";
		boxX = null;
		place(true);
	}

	private static String name(int box) {
		return String.valueOf((char) ('A' + box));
	}

	/** Puts a box on the next free pan (or takes it off), and weighs once both pans hold one. */
	private void pick(int box) {
		if (weighing.sorted()) {
			return; // keep the "Sorted in n weighings" line
		}
		if (weighed) {
			left = -1;
			right = -1;
			weighed = false;
		}
		if (box == left) {
			left = -1;
		} else if (box == right) {
			right = -1;
		} else if (left < 0) {
			left = box;
		} else if (right < 0) {
			right = box;
		}
		if (left >= 0 && right >= 0) {
			weigh();
		} else {
			status = "Now a second box for the other pan.";
		}
	}

	private void weigh() {
		Weighing.Result result = weighing.weigh(left, right);
		weighed = true;
		hint = null;
		String what = name(result.lighter) + " is lighter than " + name(result.heavier) + ".";
		if (weighing.sorted()) {
			status = "Sorted in " + weighing.log.size() + (weighing.log.size() == 1 ? " weighing." : " weighings.")
					+ (peeked ? " (You peeked.)" : "") + " N: new boxes.";
		} else if (result.after == result.before) {
			status = "Nothing new: you knew " + name(result.lighter) + " < " + name(result.heavier) + ".";
		} else {
			status = what;
		}
	}

	private void showHint() {
		hint = weighing.bestPair();
		if (hint == null) {
			return;
		}
		long worst = Math.max(weighing.orders(hint[0], hint[1]), weighing.orders(hint[1], hint[0]));
		status = "Weigh " + name(hint[0]) + " and " + name(hint[1]) + ": " + Math.min(worst, weighing.orders() - worst)
				+ " or " + worst + " left.";
	}

	/** Weighs the pair the hint would name. */
	private void weighHint() {
		int[] pair = weighing.bestPair();
		if (pair == null) {
			return;
		}
		left = pair[0];
		right = pair[1];
		weigh();
	}

	/** Where each box belongs in the picture: its column, and its place among the boxes of that column. */
	private void place(boolean jump) {
		int[] columns = weighing.columns();
		double[] targetX = new double[count];
		double[] targetY = new double[count];
		int columnW = DIAGRAM_W / count;
		for (int box = 0; box < count; box++) {
			int inColumn = 0;
			int before = 0;
			for (int other = 0; other < count; other++) {
				if (columns[other] == columns[box]) {
					inColumn++;
					if (other < box) {
						before++;
					}
				}
			}
			targetX[box] = DIAGRAM_X + columns[box] * columnW + (columnW - BOX) / 2;
			targetY[box] = (DIAGRAM_TOP + DIAGRAM_BOTTOM) / 2 - (inColumn * ROW - (ROW - BOX)) / 2 + before * ROW;
		}
		if (jump || boxX == null) {
			boxX = targetX;
			boxY = targetY;
			return;
		}
		for (int box = 0; box < count; box++) {
			boxX[box] += (targetX[box] - boxX[box]) * 0.2;
			boxY[box] += (targetY[box] - boxY[box]) * 0.2;
		}
	}

	private double targetTilt() {
		if (!weighed) {
			return 0;
		}
		return weighing.weight(left) > weighing.weight(right) ? -0.22 : 0.22;
	}

	@Override
	boolean tick() {
		double[] oldX = boxX.clone();
		double[] oldY = boxY.clone();
		double oldTilt = tilt;
		place(false);
		tilt += (targetTilt() - tilt) * 0.2;
		boolean moved = Math.abs(tilt - oldTilt) > 0.001;
		for (int box = 0; box < count; box++) {
			moved |= Math.abs(boxX[box] - oldX[box]) > 0.2 || Math.abs(boxY[box] - oldY[box]) > 0.2;
		}
		return moved;
	}

	@Override
	void click(int x, int y) {
		for (int box = 0; box < count; box++) {
			if (x >= boxX[box] && x < boxX[box] + BOX && y >= boxY[box] && y < boxY[box] + BOX) {
				pick(box);
				return;
			}
		}
		int[] pans = { left, right };
		for (int side = 0; side < 2; side++) {
			if (pans[side] >= 0 && Math.abs(x - panX(side)) < BOX / 2 + 8 && Math.abs(y - panY(side) + BOX / 2) < BOX / 2 + 8) {
				pick(pans[side]);
				return;
			}
		}
	}

	@Override
	boolean key(KeyEvent event) {
		char key = Character.toLowerCase(event.getKeyChar());
		if (key >= 'a' && key < 'a' + count) {
			pick(key - 'a');
			return true;
		}
		if (key == '=') {
			deal(count + 1);
			return true;
		}
		if (event.getKeyCode() == KeyEvent.VK_ESCAPE && !weighed && (left >= 0 || right >= 0)) {
			left = -1;
			right = -1;
			status = "Pans cleared.";
			return true;
		}
		return false;
	}

	private int panX(int side) {
		return SIDE_X + SIDE_W / 2 + (int) Math.round((side == 0 ? -ARM : ARM) * Math.cos(tilt));
	}

	/** The top of the pan's plate on side 0 (left) or 1 (right). */
	private int panY(int side) {
		return PIVOT_Y + (int) Math.round((side == 0 ? -ARM : ARM) * Math.sin(tilt)) + 48;
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		text(g, "Sort by weighing", TITLE, INK, 20, 30);
		text(g, "The boxes look alike. Weigh two at a time to find their order, lightest first, in few weighings.", TEXT, PENCIL, 20, 50);
		text(g, "A line runs from a box proven lighter to one proven heavier. One straight row is sorted.", SMALL, PENCIL, 20, 68);
		paintDiagram(g);
		paintScale(g);
		paintSide(g);
	}

	private void paintDiagram(Graphics2D g) {
		boolean sorted = weighing.sorted();
		g.setStroke(new BasicStroke(2f));
		g.setColor(sorted ? GREEN : PENCIL);
		for (int a = 0; a < count; a++) {
			for (int b = 0; b < count; b++) {
				if (weighing.direct(a, b)) {
					g.drawLine((int) boxX[a] + BOX, (int) boxY[a] + BOX / 2, (int) boxX[b], (int) boxY[b] + BOX / 2);
				}
			}
		}
		g.setStroke(new BasicStroke(1f));
		for (int box = 0; box < count; box++) {
			int ring = 0;
			Color ringColor = null;
			if (box == left || box == right) {
				ring = 3;
				ringColor = HIGHLIGHT.darker();
			} else if (hint != null && (box == hint[0] || box == hint[1])) {
				ring = 3;
				ringColor = HINT;
			}
			paintBox(g, box, (int) boxX[box], (int) boxY[box], ring, ringColor);
		}
	}

	private void paintBox(Graphics2D g, int box, int x, int y, int ring, Color ringColor) {
		if (ring > 0) {
			g.setColor(ringColor);
			g.fillRoundRect(x - ring, y - ring, BOX + 2 * ring, BOX + 2 * ring, 10, 10);
		}
		g.setColor(CARD);
		g.fillRoundRect(x, y, BOX, BOX, 6, 6);
		g.setColor(CARD_EDGE);
		g.drawRoundRect(x, y, BOX, BOX, 6, 6);
		g.drawLine(x + 4, y + 7, x + BOX - 4, y + 7);
		boolean open = showWeights || weighing.sorted();
		centered(g, name(box), BOLD, INK, x + BOX / 2, y + (open ? 20 : 24));
		if (open) {
			centered(g, weighing.weight(box) + " g", SMALL, RED, x + BOX / 2, y + 31);
		}
	}

	private void paintScale(Graphics2D g) {
		int cx = SIDE_X + SIDE_W / 2;
		g.setColor(CARD_EDGE);
		g.fillRect(cx - 3, PIVOT_Y, 6, 92);
		g.fillRoundRect(cx - 34, PIVOT_Y + 88, 68, 8, 6, 6);
		g.setColor(INK);
		g.setStroke(new BasicStroke(4f));
		int dx = (int) Math.round(ARM * Math.cos(tilt));
		int dy = (int) Math.round(ARM * Math.sin(tilt));
		g.drawLine(cx - dx, PIVOT_Y - dy, cx + dx, PIVOT_Y + dy);
		g.setStroke(new BasicStroke(1f));
		g.fillOval(cx - 6, PIVOT_Y - 6, 12, 12);
		int[] pans = { left, right };
		for (int side = 0; side < 2; side++) {
			int px = panX(side);
			int py = panY(side);
			int top = PIVOT_Y + (side == 0 ? -dy : dy);
			g.setColor(PENCIL);
			g.drawLine(px, top, px - 26, py);
			g.drawLine(px, top, px + 26, py);
			g.setColor(INK);
			g.fillRoundRect(px - 30, py, 60, 6, 6, 6);
			if (pans[side] >= 0) {
				paintBox(g, pans[side], px - BOX / 2, py - BOX, 0, null);
			}
		}
	}

	private void paintSide(Graphics2D g) {
		int x = SIDE_X;
		long all = Weighing.factorial(count);
		long orders = weighing.orders();
		text(g, "Orders still possible", SMALL, PENCIL, x, 210);
		String big = String.format("%,d", orders);
		text(g, big, TITLE, INK, x, 234);
		text(g, "of " + String.format("%,d", all), TEXT, PENCIL, x + width(g, big, TITLE) + 8, 234);
		// The bar fills by information found: each weighing that halves the orders adds the same length.
		int filled = (int) Math.round(SIDE_W * (1 - Math.log(orders) / Math.log(all)));
		g.setColor(GRID);
		g.fillRoundRect(x, 242, SIDE_W, 8, 8, 8);
		g.setColor(weighing.sorted() ? GREEN : INK);
		g.fillRoundRect(x, 242, filled, 8, 8, 8);
		text(g, "Weighings: " + weighing.log.size() + "    always enough: " + Weighing.fewestSure(count), TEXT, INK, x, 268);
		// The status in up to two lines of the side's width.
		String line = "";
		int y = 290;
		for (String word : status.split(" ")) {
			if (!line.isEmpty() && width(g, line + " " + word, BOLD) > SIDE_W) {
				text(g, line, BOLD, weighing.sorted() ? GREEN : INK, x, y);
				line = word;
				y += 16;
			} else {
				line = line.isEmpty() ? word : line + " " + word;
			}
		}
		text(g, line, BOLD, weighing.sorted() ? GREEN : INK, x, y);
		if (weighing.sorted()) {
			paintResults(g, x);
		} else {
			paintLog(g, x);
		}
	}

	private void paintLog(Graphics2D g, int x) {
		int shown = Math.min(4, weighing.log.size());
		for (int i = 0; i < shown; i++) {
			int index = weighing.log.size() - shown + i;
			Weighing.Result result = weighing.log.get(index);
			int y = 326 + i * 14;
			boolean wasted = result.after == result.before;
			text(g, (index + 1) + ".  " + name(result.lighter) + " < " + name(result.heavier), CODE, wasted ? PENCIL : INK, x, y);
			String counts = String.format("%,d", result.before) + " → " + String.format("%,d", result.after);
			text(g, wasted ? "nothing new" : counts, SMALL, PENCIL, x + SIDE_W - width(g, wasted ? "nothing new" : counts, SMALL), y);
		}
	}

	private void paintResults(Graphics2D g, int x) {
		String[] labels = { "You", "Merge sort, same boxes", "Insertion sort", "Bubble sort" };
		int[] values = { weighing.log.size(), weighing.compares(3), weighing.compares(2), weighing.compares(0) };
		for (int i = 0; i < labels.length; i++) {
			int y = 326 + i * 14;
			Font font = i == 0 ? BOLD : TEXT;
			text(g, labels[i], font, i == 0 ? INK : PENCIL, x, y);
			String value = String.valueOf(values[i]);
			text(g, value, font, INK, x + SIDE_W - width(g, value, font), y);
		}
	}
}
