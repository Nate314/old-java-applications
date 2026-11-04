package simplealgorithms;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * The original console program: the lines {@link Main#printout} prints at start, as a terminal would show them.
 * A button adds what main's commented-out Hanoi call would print (for 3 disks, so it fits).
 */
class PrintoutPage extends Page {
	private static final int HANOI_DISKS = 3;
	private final List<String> lines = Main.printout();
	private boolean hanoi;

	PrintoutPage() {
		super("Printout");
		button("Add Hanoi.solve(" + HANOI_DISKS + ", true)", 'h', () -> hanoi = !hanoi);
	}

	private List<String> shown() {
		List<String> shown = new ArrayList<>(lines);
		if (hanoi) {
			shown.add("");
			for (Hanoi.Move move : Hanoi.moves(HANOI_DISKS)) {
				shown.add(move.toString());
			}
		}
		return shown;
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		text(g, "What the original program prints", TITLE, INK, 20, 32);
		text(g, "Main still prints these lines to the console when it starts, before this window opens.", TEXT, PENCIL, 20, 54);
		int top = 70;
		g.setColor(new Color(24, 28, 36));
		g.fillRoundRect(20, top, w - 40, h - top - 12, 12, 12);
		List<String> shown = shown();
		int rowsPerColumn = (h - top - 30) / 17;
		int columnWidth = (w - 40) / 2;
		for (int i = 0; i < shown.size(); i++) {
			int x = 36 + i / rowsPerColumn * columnWidth;
			int y = top + 24 + i % rowsPerColumn * 17;
			String line = shown.get(i);
			boolean result = line.contains("=");
			text(g, line, result ? CODE_BOLD : CODE, result ? new Color(140, 220, 150) : new Color(190, 198, 210), x, y);
		}
	}
}
