package simplealgorithms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The Pathfinding page's map: a grid of open, wall and mud cells with a start and a goal. Stepping onto an open cell
 * costs 1 and onto mud {@link #MUD_COST}; walls can't be entered. Cells are numbered {@code y * COLS + x}.
 */
final class Grid {
	static final int COLS = 29;
	static final int ROWS = 19;
	static final int OPEN = 0;
	static final int WALL = 1;
	static final int MUD = 2;
	static final int MUD_COST = 5;

	final int[] cells = new int[COLS * ROWS];
	int start;
	int goal;

	/**
	 * The map the page opens on: the goal behind a cup that opens toward the start, and a band of mud to cross with a
	 * dry way round at the top that is two steps longer but cheaper.
	 */
	static Grid sample() {
		Grid grid = new Grid();
		grid.start = at(3, 9);
		grid.goal = at(25, 9);
		for (int i = 11; i <= 19; i++) {
			grid.cells[at(i, 3)] = WALL;
			grid.cells[at(i, 15)] = WALL;
		}
		for (int y = 3; y <= 15; y++) {
			grid.cells[at(19, y)] = WALL;
		}
		for (int x = 6; x <= 8; x++) {
			for (int y = 2; y < ROWS; y++) {
				grid.cells[at(x, y)] = MUD;
			}
		}
		return grid;
	}

	/**
	 * A random maze: a recursive-backtracker maze on the odd cells, then a tenth of the inner walls knocked out so
	 * there is more than one way through, and a few mud patches. Start is top left, goal bottom right.
	 */
	static Grid maze(Random random) {
		Grid grid = new Grid();
		java.util.Arrays.fill(grid.cells, WALL);
		List<Integer> stack = new ArrayList<>();
		stack.add(at(1, 1));
		grid.cells[at(1, 1)] = OPEN;
		int[][] dirs = { { 2, 0 }, { -2, 0 }, { 0, 2 }, { 0, -2 } };
		while (!stack.isEmpty()) {
			int cell = stack.get(stack.size() - 1);
			int x = cell % COLS;
			int y = cell / COLS;
			List<int[]> options = new ArrayList<>();
			for (int[] d : dirs) {
				int nx = x + d[0];
				int ny = y + d[1];
				if (nx > 0 && ny > 0 && nx < COLS - 1 && ny < ROWS - 1 && grid.cells[at(nx, ny)] == WALL) {
					options.add(d);
				}
			}
			if (options.isEmpty()) {
				stack.remove(stack.size() - 1);
				continue;
			}
			int[] d = options.get(random.nextInt(options.size()));
			grid.cells[at(x + d[0] / 2, y + d[1] / 2)] = OPEN;
			grid.cells[at(x + d[0], y + d[1])] = OPEN;
			stack.add(at(x + d[0], y + d[1]));
		}
		// Knock out inner walls that sit between two open cells, so routes loop.
		for (int y = 1; y < ROWS - 1; y++) {
			for (int x = 1; x < COLS - 1; x++) {
				boolean across = grid.cells[at(x - 1, y)] == OPEN && grid.cells[at(x + 1, y)] == OPEN;
				boolean down = grid.cells[at(x, y - 1)] == OPEN && grid.cells[at(x, y + 1)] == OPEN;
				if (grid.cells[at(x, y)] == WALL && (across ^ down) && random.nextInt(10) == 0) {
					grid.cells[at(x, y)] = OPEN;
				}
			}
		}
		for (int patch = 0; patch < 4; patch++) {
			int cx = 2 + random.nextInt(COLS - 4);
			int cy = 2 + random.nextInt(ROWS - 4);
			for (int y = cy - 1; y <= cy + 1; y++) {
				for (int x = cx - 2; x <= cx + 2; x++) {
					if (grid.cells[at(x, y)] == OPEN) {
						grid.cells[at(x, y)] = MUD;
					}
				}
			}
		}
		grid.start = at(1, 1);
		grid.goal = at(COLS - 2, ROWS - 2);
		grid.cells[grid.start] = OPEN;
		grid.cells[grid.goal] = OPEN;
		return grid;
	}

	static int at(int x, int y) {
		return y * COLS + x;
	}

	static boolean inside(int x, int y) {
		return x >= 0 && y >= 0 && x < COLS && y < ROWS;
	}

	/** The cost of stepping onto a cell (walls are never stepped onto). */
	int cost(int cell) {
		return cells[cell] == MUD ? MUD_COST : 1;
	}

	/** The cells next to {@code cell} that can be entered, in the order up, right, down, left. */
	int[] neighbors(int cell) {
		int x = cell % COLS;
		int y = cell / COLS;
		int[] out = new int[4];
		int n = 0;
		int[][] dirs = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
		for (int[] d : dirs) {
			int nx = x + d[0];
			int ny = y + d[1];
			if (inside(nx, ny) && cells[at(nx, ny)] != WALL) {
				out[n++] = at(nx, ny);
			}
		}
		return java.util.Arrays.copyOf(out, n);
	}

	/** Manhattan distance from a cell to the goal: never more than the real cost, since every step costs 1 or more. */
	int distanceToGoal(int cell) {
		return Math.abs(cell % COLS - goal % COLS) + Math.abs(cell / COLS - goal / COLS);
	}

	Grid copy() {
		Grid grid = new Grid();
		System.arraycopy(cells, 0, grid.cells, 0, cells.length);
		grid.start = start;
		grid.goal = goal;
		return grid;
	}

	/** One line per row: {@code .} open, {@code #} wall, {@code ~} mud, {@code S} start, {@code G} goal. */
	List<String> lines() {
		List<String> lines = new ArrayList<>();
		for (int y = 0; y < ROWS; y++) {
			StringBuilder row = new StringBuilder();
			for (int x = 0; x < COLS; x++) {
				int cell = at(x, y);
				row.append(cell == start ? 'S' : cell == goal ? 'G' : ".#~".charAt(cells[cell]));
			}
			lines.add(row.toString());
		}
		return lines;
	}

	/** The grid {@link #lines} wrote, or null if the lines are not one. */
	static Grid parse(List<String> lines) {
		if (lines.size() != ROWS) {
			return null;
		}
		Grid grid = new Grid();
		grid.start = -1;
		grid.goal = -1;
		for (int y = 0; y < ROWS; y++) {
			String row = lines.get(y);
			if (row.length() != COLS) {
				return null;
			}
			for (int x = 0; x < COLS; x++) {
				char c = row.charAt(x);
				if (c == 'S') {
					grid.start = at(x, y);
				} else if (c == 'G') {
					grid.goal = at(x, y);
				} else if (".#~".indexOf(c) >= 0) {
					grid.cells[at(x, y)] = ".#~".indexOf(c);
				} else {
					return null;
				}
			}
		}
		return grid.start >= 0 && grid.goal >= 0 && grid.start != grid.goal ? grid : null;
	}

	/** The saved map and the picked search, kept in {@code ~/.simplealgorithms-paths.txt}. */
	static final class Saved {
		static final File FILE = new File(System.getProperty("user.home"), ".simplealgorithms-paths.txt");

		final Grid grid;
		final int search;

		Saved(Grid grid, int search) {
			this.grid = grid;
			this.search = search;
		}

		/** What {@link #save} wrote, or null when the file is missing or not a map. */
		static Saved load(File file) {
			int search = -1;
			List<String> rows = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				for (String line; (line = reader.readLine()) != null;) {
					if (line.startsWith("search=")) {
						try {
							search = Integer.parseInt(line.substring(7).trim());
						} catch (NumberFormatException e) {
							search = -1;
						}
					} else if (!line.trim().isEmpty()) {
						rows.add(line.trim());
					}
				}
			} catch (IOException e) {
				return null;
			}
			Grid grid = parse(rows);
			return grid == null ? null : new Saved(grid, search);
		}

		void save(File file) {
			try (Writer writer = new FileWriter(file)) {
				writer.write("search=" + search + "\n");
				for (String line : grid.lines()) {
					writer.write(line + "\n");
				}
			} catch (IOException e) {
				// Could not save: the map lasts until the window closes.
			}
		}
	}
}