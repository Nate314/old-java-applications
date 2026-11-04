package simplealgorithms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pathfinding: draw walls and mud on a grid, drag the start and goal, and watch one of five searches look for the way,
 * cell by cell. A table beside the grid runs all five on the same map every time it changes, so drawing a wall shows
 * at once which searches find the cheapest way and how many cells each had to look at.
 */
class PathPage extends Page {
	private static final int CELL = 16;
	private static final int GX = 16;
	private static final int GY = 64;
	private static final int PX = GX + Grid.COLS * CELL + 16;
	private static final int ROW_TOP = GY + 16;
	private static final int ROW = 24;
	private static final double[] SPEEDS = { 0.5, 2, 6, 20 };
	private static final int[] PATH_SPEEDS = { 1, 1, 2, 4 };
	private static final String[] SPEED_NAMES = { "Slow", "Normal", "Fast", "Turbo" };
	private static final Color MUD = new Color(170, 128, 84);
	private static final Color MUD_DOT = new Color(120, 86, 52);
	private static final Color LOOKED_NEW = new Color(150, 190, 236, 170);
	private static final Color LOOKED_OLD = new Color(105, 150, 215, 170);
	private static final Color SEEN = new Color(255, 214, 90, 200);
	private static final Color NOW = new Color(232, 146, 40);

	/** What a press on the grid started: moving an end, or painting one kind of cell. */
	private static final int NONE = -1;
	private static final int MOVE_START = -2;
	private static final int MOVE_GOAL = -3;

	private final Random random = new Random();
	private Grid grid;
	private Paths.Search[] searches;
	private int algorithm = Paths.A_STAR;
	private int tool = Grid.WALL;
	private int speed = 1;
	private boolean playing = true;
	/** Cells of the shown search looked at so far, and steps of its way drawn so far. */
	private double shown;
	private int pathShown;

	private int dragging = NONE;
	private int lastCell = -1;
	private boolean edited;
	private int hoverRow = -1;

	private final Button play;
	private final Button algorithmButton;
	private final Button toolButton;
	private final Button speedButton;

	PathPage() {
		super("Pathfinding");
		play = button("Pause", ' ', this::togglePlay);
		algorithmButton = button("", 'a', () -> pick((algorithm + 1) % Paths.NAMES.length));
		toolButton = button("", 'd', () -> {
			tool = tool == Grid.WALL ? Grid.MUD : Grid.WALL;
			labels();
		});
		button("New maze", 'm', () -> replace(Grid.maze(random)));
		button("Example", 'x', () -> replace(Grid.sample()));
		button("Clear", 'c', this::clear);
		speedButton = button("", 'f', () -> {
			speed = (speed + 1) % SPEEDS.length;
			labels();
		});
		Grid.Saved saved = Grid.Saved.load(Grid.Saved.FILE);
		grid = saved != null ? saved.grid : Grid.sample();
		if (saved != null && saved.search >= 0 && saved.search < Paths.NAMES.length) {
			algorithm = saved.search;
		}
		searches = Paths.all(grid);
		restart();
	}

	private Paths.Search search() {
		return searches[algorithm];
	}

	private void labels() {
		play.label = playing ? "Pause" : done() ? "Again" : "Play";
		algorithmButton.label = Paths.SHORT_NAMES[algorithm];
		toolButton.label = tool == Grid.WALL ? "Walls" : "Mud";
		speedButton.label = SPEED_NAMES[speed];
	}

	private int pathLength() {
		return search().found() ? search().path.length : 0;
	}

	private boolean done() {
		return shown >= search().order.length && pathShown >= pathLength();
	}

	/** Plays the shown search from its first cell. */
	private void restart() {
		shown = 0;
		pathShown = 0;
		playing = true;
		labels();
	}

	/** Shows the whole of the shown search at once. */
	private void finish() {
		shown = search().order.length;
		pathShown = pathLength();
		playing = false;
		labels();
	}

	private void pick(int index) {
		algorithm = index;
		save();
		restart();
	}

	private void togglePlay() {
		if (done()) {
			restart();
			return;
		}
		playing = !playing;
		labels();
	}

	private void step() {
		playing = false;
		if (shown < search().order.length) {
			shown = Math.floor(shown) + 1;
		} else if (pathShown < pathLength()) {
			pathShown = pathLength();
		}
		labels();
	}

	private void replace(Grid next) {
		grid = next;
		searches = Paths.all(grid);
		save();
		restart();
	}

	private void clear() {
		Grid empty = grid.copy();
		java.util.Arrays.fill(empty.cells, Grid.OPEN);
		replace(empty);
	}

	private void changed() {
		searches = Paths.all(grid);
		edited = true;
		finish();
	}

	private void save() {
		new Grid.Saved(grid, algorithm).save(Grid.Saved.FILE);
	}

	@Override
	boolean tick() {
		if (!playing) {
			return false;
		}
		if (shown < search().order.length) {
			shown = Math.min(search().order.length, shown + SPEEDS[speed]);
		} else if (pathShown < pathLength()) {
			pathShown = Math.min(pathLength(), pathShown + PATH_SPEEDS[speed]);
		} else {
			playing = false;
			labels();
		}
		return true;
	}

	@Override
	boolean key(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_RIGHT || Character.toLowerCase(event.getKeyChar()) == 's') {
			step();
			return true;
		}
		return false;
	}

	private static int cellAt(int x, int y) {
		if (x < GX || y < GY) {
			return -1;
		}
		int cx = (x - GX) / CELL;
		int cy = (y - GY) / CELL;
		return Grid.inside(cx, cy) ? Grid.at(cx, cy) : -1;
	}

	private static int rowAt(int x, int y) {
		if (x < PX || x >= LabPanel.WIDTH - 8 || y < ROW_TOP) {
			return -1;
		}
		int row = (y - ROW_TOP) / ROW;
		return row < Paths.NAMES.length ? row : -1;
	}

	@Override
	void click(int x, int y) {
		int row = rowAt(x, y);
		if (row >= 0) {
			pick(row);
			return;
		}
		int cell = cellAt(x, y);
		if (cell < 0) {
			return;
		}
		if (cell == grid.start) {
			dragging = MOVE_START;
		} else if (cell == grid.goal) {
			dragging = MOVE_GOAL;
		} else {
			// Pressing a cell already of the tool's kind rubs out; anything else paints the tool.
			dragging = grid.cells[cell] == tool ? Grid.OPEN : tool;
			grid.cells[cell] = dragging;
			changed();
		}
		lastCell = cell;
	}

	@Override
	void drag(int x, int y) {
		int cell = cellAt(x, y);
		if (dragging == NONE || cell < 0 || cell == lastCell) {
			return;
		}
		if (dragging == MOVE_START || dragging == MOVE_GOAL) {
			if (grid.cells[cell] != Grid.WALL && cell != grid.start && cell != grid.goal) {
				if (dragging == MOVE_START) {
					grid.start = cell;
				} else {
					grid.goal = cell;
				}
				changed();
			}
			lastCell = cell;
			return;
		}
		// Every cell on the line from the last one, so a quick drag leaves no gaps.
		int x0 = lastCell % Grid.COLS;
		int y0 = lastCell / Grid.COLS;
		int x1 = cell % Grid.COLS;
		int y1 = cell / Grid.COLS;
		int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
		for (int i = 1; i <= n; i++) {
			int c = Grid.at(x0 + Math.round((float) (x1 - x0) * i / n), y0 + Math.round((float) (y1 - y0) * i / n));
			if (c != grid.start && c != grid.goal) {
				grid.cells[c] = dragging;
			}
		}
		lastCell = cell;
		changed();
	}

	@Override
	void release() {
		dragging = NONE;
		lastCell = -1;
		if (edited) {
			edited = false;
			save();
		}
	}

	@Override
	void hover(int x, int y) {
		hoverRow = rowAt(x, y);
	}

	@Override
	void paint(Graphics2D g, int w, int h) {
		text(g, "Pathfinding", TITLE, INK, 20, 32);
		text(g, status(), BOLD, search().found() || shown < search().order.length ? INK : RED,
				w - 20 - width(g, status(), BOLD), 32);
		text(g, "Drag on the grid to draw " + (tool == Grid.WALL ? "walls" : "mud") + " (D switches), drag S or G to move"
				+ " them. Every search reruns as you draw.", TEXT, PENCIL, 20, 54);
		paintGrid(g);
		paintPath(g);
		paintEnd(g, grid.start, GREEN, "S");
		paintEnd(g, grid.goal, RED, "G");
		paintTable(g);
	}

	private String status() {
		Paths.Search search = search();
		int looked = (int) Math.min(shown, search.order.length);
		if (looked < search.order.length) {
			return Paths.NAMES[algorithm] + " is looking: " + looked + " cells";
		}
		if (!search.found()) {
			return "No way through: the goal is walled off";
		}
		return Paths.NAMES[algorithm] + ": " + search.steps() + " steps, cost " + search.cost;
	}

	private void paintGrid(Graphics2D g) {
		Paths.Search search = search();
		int looked = (int) Math.min(shown, search.order.length);
		int latest = looked > 0 && looked < search.order.length ? search.order[looked - 1] : -1;
		g.setColor(Color.WHITE);
		g.fillRect(GX, GY, Grid.COLS * CELL, Grid.ROWS * CELL);
		for (int cell = 0; cell < grid.cells.length; cell++) {
			int x = GX + cell % Grid.COLS * CELL;
			int y = GY + cell / Grid.COLS * CELL;
			int kind = grid.cells[cell];
			if (kind == Grid.WALL) {
				g.setColor(INK);
				g.fillRect(x, y, CELL, CELL);
				continue;
			}
			if (kind == Grid.MUD) {
				g.setColor(MUD);
				g.fillRect(x, y, CELL, CELL);
			}
			int at = search.lookedAt[cell];
			if (cell == latest) {
				g.setColor(NOW);
				g.fillRect(x, y, CELL, CELL);
			} else if (at >= 0 && at < looked) {
				// Cells looked at early are pale, late ones deep, so the order of the search shows.
				g.setColor(blend(LOOKED_NEW, LOOKED_OLD, search.order.length <= 1 ? 0 : (double) at / (search.order.length - 1)));
				g.fillRect(x, y, CELL, CELL);
			} else if (search.seenAt[cell] >= 0 && search.seenAt[cell] <= looked && (looked < search.order.length || !search.found())) {
				g.setColor(SEEN);
				g.fillRect(x, y, CELL, CELL);
			}
			if (kind == Grid.MUD) {
				// The dots go over the search's colors, so mud still shows where a search has been.
				g.setColor(MUD_DOT);
				g.fillOval(x + 3, y + 4, 4, 4);
				g.fillOval(x + 10, y + 9, 4, 4);
			}
		}
		g.setColor(new Color(31, 58, 104, 40));
		for (int i = 1; i < Grid.COLS; i++) {
			g.drawLine(GX + i * CELL, GY, GX + i * CELL, GY + Grid.ROWS * CELL);
		}
		for (int i = 1; i < Grid.ROWS; i++) {
			g.drawLine(GX, GY + i * CELL, GX + Grid.COLS * CELL, GY + i * CELL);
		}
		g.setColor(INK);
		g.setStroke(new BasicStroke(1.5f));
		g.drawRect(GX, GY, Grid.COLS * CELL, Grid.ROWS * CELL);
		g.setStroke(new BasicStroke(1f));
	}

	private static Color blend(Color a, Color b, double t) {
		return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t), (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				(int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t), a.getAlpha());
	}

	private void paintPath(Graphics2D g) {
		int[] path = search().path;
		if (path == null || pathShown < 2) {
			return;
		}
		g.setColor(RED);
		g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		for (int i = 1; i < pathShown; i++) {
			g.drawLine(centerX(path[i - 1]), centerY(path[i - 1]), centerX(path[i]), centerY(path[i]));
		}
		g.setStroke(new BasicStroke(1f));
	}

	private static int centerX(int cell) {
		return GX + cell % Grid.COLS * CELL + CELL / 2;
	}

	private static int centerY(int cell) {
		return GY + cell / Grid.COLS * CELL + CELL / 2;
	}

	private static void paintEnd(Graphics2D g, int cell, Color color, String label) {
		int x = centerX(cell);
		int y = centerY(cell);
		g.setColor(color);
		g.fillOval(x - 8, y - 8, 16, 16);
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(1.5f));
		g.drawOval(x - 8, y - 8, 16, 16);
		g.setStroke(new BasicStroke(1f));
		centered(g, label, SMALL_BOLD, Color.WHITE, x, y + 4);
	}

	private static final Font SMALL_BOLD = new Font("SansSerif", Font.BOLD, 11);
	private static final int[] COLUMNS = { PX + 122, PX + 160, PX + 196 };

	private void paintTable(Graphics2D g) {
		String[] heads = { "looked", "steps", "cost" };
		for (int i = 0; i < heads.length; i++) {
			text(g, heads[i], SMALL, PENCIL, COLUMNS[i] - width(g, heads[i], SMALL), GY + 10);
		}
		int fewestLooked = Integer.MAX_VALUE;
		int fewestSteps = Integer.MAX_VALUE;
		int cheapest = Integer.MAX_VALUE;
		for (Paths.Search search : searches) {
			if (search.found()) {
				fewestLooked = Math.min(fewestLooked, search.order.length);
				fewestSteps = Math.min(fewestSteps, search.steps());
				cheapest = Math.min(cheapest, search.cost);
			}
		}
		for (int i = 0; i < searches.length; i++) {
			Paths.Search search = searches[i];
			int y = ROW_TOP + i * ROW;
			if (i == algorithm || i == hoverRow) {
				g.setColor(i == algorithm ? Color.WHITE : new Color(255, 252, 236));
				g.fillRoundRect(PX, y, LabPanel.WIDTH - 8 - PX, ROW - 3, 10, 10);
				if (i == algorithm) {
					g.setColor(INK);
					g.setStroke(new BasicStroke(1.5f));
					g.drawRoundRect(PX, y, LabPanel.WIDTH - 8 - PX, ROW - 3, 10, 10);
					g.setStroke(new BasicStroke(1f));
				}
			}
			text(g, Paths.NAMES[i], BOLD, INK, PX + 8, y + 16);
			String[] cells = { String.valueOf(search.order.length), search.found() ? String.valueOf(search.steps()) : "-",
					search.found() ? String.valueOf(search.cost) : "-" };
			boolean[] best = { search.found() && search.order.length == fewestLooked, search.found() && search.steps() == fewestSteps,
					search.found() && search.cost == cheapest };
			for (int c = 0; c < cells.length; c++) {
				Font font = best[c] ? BOLD : TEXT;
				text(g, cells[c], font, best[c] ? GREEN : INK, COLUMNS[c] - width(g, cells[c], font), y + 16);
			}
		}
		int y = ROW_TOP + Paths.NAMES.length * ROW + 14;
		for (String line : wrap(g, Paths.IDEAS[algorithm], TEXT, LabPanel.WIDTH - 12 - PX)) {
			text(g, line, TEXT, INK, PX + 4, y);
			y += 17;
		}
		y += 4;
		for (String line : wrap(g, "Green: best in its column. A step onto mud costs " + Grid.MUD_COST + ", onto open ground 1.",
				SMALL, LabPanel.WIDTH - 12 - PX)) {
			text(g, line, SMALL, PENCIL, PX + 4, y);
			y += 14;
		}
	}

	/** Splits text into lines no wider than width, at spaces. */
	private static List<String> wrap(Graphics2D g, String text, Font font, int width) {
		List<String> lines = new ArrayList<>();
		String line = "";
		for (String word : text.split(" ")) {
			String longer = line.isEmpty() ? word : line + " " + word;
			if (!line.isEmpty() && width(g, longer, font) > width) {
				lines.add(line);
				line = word;
			} else {
				line = longer;
			}
		}
		if (!line.isEmpty()) {
			lines.add(line);
		}
		return lines;
	}
}
