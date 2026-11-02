package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Tower of Hanoi: plays the moves {@link Hanoi#moves} prints, showing for each one the move number in binary (the
 * lowest 1 bit names the disk), or lets the visitor play by clicking pegs. Off the printed path, Step and Play
 * continue with the shortest solution from wherever the disks are.
 */
class HanoiPage extends Page {
	private static final Color[] DISK_COLORS = { new Color(200, 69, 58), new Color(224, 140, 46),
			new Color(214, 182, 50), new Color(92, 160, 72), new Color(46, 140, 150), new Color(60, 100, 180),
			new Color(110, 80, 170), new Color(160, 70, 130) };
	private static final String[] PEG_NAMES = { "left", "middle", "right" };
	private static final double[] SPEEDS = { 0.035, 0.07, 0.16, 0.5 };
	private static final String[] SPEED_NAMES = { "Slow", "Normal", "Fast", "Turbo" };
	private static final int BASE = 350;

	private HanoiTower tower;
	private List<Hanoi.Move> printed;
	/** Moves made so far that match the printed list, or -1 once a hand move left it. */
	private int onPath;
	private int moves;
	private int lifted = -1;
	private String warning;
	private boolean playing = true;
	private int speed = 1;
	private int disks = 5;

	/** The move being animated: its disk, pegs and progress from 0 to 1. */
	private int animDisk = -1;
	private int animFrom;
	private int animTo;
	private double animT;

	private final Button play;
	private final Button speedButton;

	HanoiPage() {
		super("Tower of Hanoi");
		play = button("Pause", ' ', this::togglePlay);
		button("Step", 's', this::step);
		button("Reset", 'r', this::reset);
		button("Fewer disks", '-', () -> resize(disks - 1));
		button("More disks", '+', () -> resize(disks + 1));
		speedButton = button("Normal", 'f', this::nextSpeed);
		reset();
	}

	private void reset() {
		tower = new HanoiTower(disks);
		printed = Hanoi.moves(disks);
		onPath = 0;
		moves = 0;
		lifted = -1;
		warning = null;
		animDisk = -1;
	}

	private void resize(int count) {
		if (count >= 1 && count <= DISK_COLORS.length) {
			disks = count;
			reset();
		}
	}

	private void togglePlay() {
		if (tower.isSolved() && animDisk < 0) {
			reset();
			playing = true;
		} else {
			playing = !playing;
		}
		lifted = -1;
		updatePlay();
	}

	private void updatePlay() {
		play.label = playing ? "Pause" : "Play";
	}

	private void nextSpeed() {
		speed = (speed + 1) % SPEEDS.length;
		speedButton.label = SPEED_NAMES[speed];
	}

	/** One move of the solution: finishes a move in flight first. */
	private void step() {
		playing = false;
		updatePlay();
		if (animDisk >= 0) {
			finishAnimation();
		}
		startNextMove();
		if (animDisk >= 0) {
			finishAnimation();
		}
	}

	/** Starts animating the next printed move, or the solver's move off the printed path. */
	private void startNextMove() {
		lifted = -1;
		int[] next = tower.nextMove();
		if (next == null) {
			return;
		}
		if (onPath >= 0) {
			Hanoi.Move move = printed.get(onPath);
			next = new int[] { tower.pegOf(move.disk), tower.target(move) };
		}
		animDisk = tower.top(next[0]);
		animFrom = next[0];
		animTo = next[1];
		animT = 0;
		warning = null;
	}

	private void finishAnimation() {
		apply(animFrom, animTo);
		animDisk = -1;
	}

	/** Makes a move, and keeps track of whether it is still the printed one. */
	private void apply(int from, int to) {
		boolean printedMove = onPath >= 0 && onPath < printed.size() && tower.pegOf(printed.get(onPath).disk) == from
				&& tower.top(from) == printed.get(onPath).disk && tower.target(printed.get(onPath)) == to;
		if (tower.move(from, to) >= 0) {
			moves++;
			onPath = printedMove ? onPath + 1 : -1;
		}
	}

	@Override
	boolean tick() {
		if (animDisk >= 0) {
			animT += SPEEDS[speed];
			if (animT >= 1) {
				finishAnimation();
			}
			return true;
		}
		if (playing) {
			if (tower.isSolved()) {
				playing = false;
				updatePlay();
				return true;
			}
			startNextMove();
			return true;
		}
		return false;
	}

	@Override
	void click(int x, int y) {
		if (y < 150) {
			return;
		}
		if (playing || animDisk >= 0) {
			playing = false;
			updatePlay();
			if (animDisk >= 0) {
				finishAnimation();
			}
		}
		int peg = Math.max(0, Math.min(2, x * 3 / LabPanel.WIDTH));
		if (lifted < 0) {
			if (tower.top(peg) >= 0) {
				lifted = peg;
				warning = null;
			}
		} else if (lifted == peg) {
			lifted = -1;
		} else if (tower.canMove(lifted, peg)) {
			apply(lifted, peg);
			lifted = -1;
			warning = null;
		} else {
			warning = "Disk " + tower.top(lifted) + " can't go on disk " + tower.top(peg) + ", which is smaller.";
		}
	}

	@Override
	boolean key(KeyEvent event) {
		if (event.getKeyChar() == '=') {
			resize(disks + 1);
			return true;
		}
		if (event.getKeyCode() == KeyEvent.VK_RIGHT) {
			step();
			return true;
		}
		return false;
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		text(g, "Tower of Hanoi", TITLE, INK, 20, 32);
		text(g, "Stack every disk on the right peg, never a disk on a smaller one. Click a peg to lift its top disk.",
				TEXT, PENCIL, 20, 54);
		text(g, "Moves " + moves, BOLD, INK, w - 20 - width(g, "Moves " + moves, BOLD), 32);
		paintExplanation(g, w);
		paintPegs(g, w);
	}

	private void paintExplanation(Graphics2D g, int w) {
		g.setColor(new Color(255, 255, 255, 200));
		g.fillRoundRect(16, 68, w - 32, 76, 12, 12);
		g.setColor(GRID);
		g.drawRoundRect(16, 68, w - 32, 76, 12, 12);
		int fewest = (1 << disks) - 1;
		if (warning != null) {
			text(g, warning, BOLD, RED, 30, 96);
			text(g, "Drop it on an empty peg or a bigger disk, or click its own peg to put it back.", TEXT, INK, 30, 120);
			return;
		}
		if (tower.isSolved() && animDisk < 0) {
			String done = "Solved in " + moves + " moves" + (moves == fewest ? ", the fewest possible." : ". The fewest is " + fewest + ".");
			text(g, done, BOLD, GREEN, 30, 96);
			text(g, onPath >= 0 ? "That is exactly the list Hanoi.solve(" + disks + ", true) prints. Play runs it again."
					+ (disks < DISK_COLORS.length ? " " + (disks + 1) + " disks take " + (2 * fewest + 1) + " moves." : "")
					: "Reset and press Play to watch the printed solution, or try " + (disks + 1) + " disks.", TEXT, INK, 30, 120);
			return;
		}
		int index = onPath;
		if (index >= 0 && index < printed.size()) {
			int number = index + 1;
			text(g, "Move " + number + " of " + fewest + ".  " + number + " in binary:", TEXT, INK, 30, 96);
			int x = 30 + width(g, "Move " + number + " of " + fewest + ".  " + number + " in binary:", TEXT) + 10;
			int lowest = Integer.numberOfTrailingZeros(number);
			for (int bit = disks - 1; bit >= 0; bit--) {
				boolean one = (number >> bit & 1) == 1;
				if (bit == lowest) {
					g.setColor(HIGHLIGHT);
					g.fillRoundRect(x - 3, 80, 20, 22, 6, 6);
				}
				text(g, one ? "1" : "0", CODE_BOLD, bit < lowest ? PENCIL : bit == lowest ? RED : INK, x + 3, 96);
				x += 20;
			}
			Hanoi.Move move = printed.get(index);
			text(g, "  The lowest 1 is bit " + lowest + ", so disk " + lowest + " moves.", TEXT, INK, x, 96);
			text(g, "Hanoi.solve prints:", TEXT, PENCIL, 30, 122);
			int x2 = 30 + width(g, "Hanoi.solve prints:", TEXT) + 8;
			text(g, move.toString(), CODE_BOLD, RED, x2, 122);
			String where = "(one peg " + (move.right ? "right" : "left") + ", wrapping around: " + PEG_NAMES[tower.pegOf(move.disk)]
					+ " to " + PEG_NAMES[tower.target(move)] + ")";
			text(g, where, TEXT, PENCIL, x2 + width(g, move.toString(), CODE_BOLD) + 8, 122);
		} else {
			int[] next = tower.nextMove();
			text(g, "Off the printed path: you moved by hand.  Fewest moves from here: " + tower.movesLeft() + ".", TEXT, INK, 30, 96);
			if (next != null) {
				text(g, "Best next move: disk " + tower.top(next[0]) + " from " + PEG_NAMES[next[0]] + " to " + PEG_NAMES[next[1]]
						+ ".  Step or Play finishes from here.", TEXT, PENCIL, 30, 122);
			}
		}
	}

	private int diskHeight() {
		return Math.min(24, 170 / (disks + 1));
	}

	private int diskWidth(int disk) {
		return disks == 1 ? 120 : 44 + disk * 156 / (disks - 1);
	}

	private static int pegX(int peg) {
		return LabPanel.WIDTH / 6 + peg * LabPanel.WIDTH / 3;
	}

	private void paintPegs(Graphics2D g, int w) {
		int dh = diskHeight();
		int pegTop = BASE - (disks + 1) * dh - 12;
		g.setColor(new Color(225, 240, 228));
		g.fillRoundRect(pegX(2) - 115, pegTop - 8, 230, BASE - pegTop + 12, 16, 16);
		g.setColor(new Color(130, 96, 60));
		g.fillRoundRect(30, BASE, w - 60, 8, 6, 6);
		for (int peg = 0; peg < 3; peg++) {
			g.setColor(new Color(130, 96, 60));
			g.fillRoundRect(pegX(peg) - 4, pegTop, 8, BASE - pegTop, 6, 6);
			centered(g, PEG_NAMES[peg] + (peg == tower.goal ? " (goal)" : ""), BOLD, peg == tower.goal ? GREEN : PENCIL, pegX(peg), BASE + 26);
		}
		for (int peg = 0; peg < 3; peg++) {
			int level = 0;
			for (int disk = disks - 1; disk >= 0; disk--) {
				if (tower.pegOf(disk) != peg || disk == animDisk) {
					continue;
				}
				int y = BASE - (level + 1) * dh;
				if (peg == lifted && disk == tower.top(peg)) {
					y = pegTop - dh - 14;
				}
				paintDisk(g, disk, pegX(peg), y, dh);
				level++;
			}
		}
		if (animDisk >= 0) {
			double t = animT * animT * (3 - 2 * animT);
			int y0 = BASE - tower.height(animFrom) * dh;
			int y1 = BASE - (tower.height(animTo) + 1) * dh;
			int lift = pegTop - dh - 14;
			double x = pegX(animFrom) + (pegX(animTo) - pegX(animFrom)) * t;
			double y = y0 + (y1 - y0) * t - Math.sin(Math.PI * t) * (Math.min(y0, y1) - lift);
			paintDisk(g, animDisk, (int) x, (int) y, dh);
		}
	}

	private void paintDisk(Graphics2D g, int disk, int centerX, int y, int dh) {
		int dw = diskWidth(disk);
		g.setColor(DISK_COLORS[disk]);
		g.fillRoundRect(centerX - dw / 2, y + 1, dw, dh - 2, dh - 2, dh - 2);
		g.setColor(DISK_COLORS[disk].darker());
		g.setStroke(new BasicStroke(1.5f));
		g.drawRoundRect(centerX - dw / 2, y + 1, dw, dh - 2, dh - 2, dh - 2);
		g.setStroke(new BasicStroke(1f));
		centered(g, String.valueOf(disk), dh >= 18 ? BOLD : SMALL, Color.WHITE, centerX, y + dh / 2 + 5);
	}
}
