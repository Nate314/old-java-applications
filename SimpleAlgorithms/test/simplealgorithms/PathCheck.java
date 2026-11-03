package simplealgorithms;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

/** Self-check for Grid and Paths. Run with java -ea. */
public class PathCheck {
	public static void main(String[] args) throws Exception {
		Random random = new Random(11);
		int reachable = 0;
		for (int round = 0; round < 300; round++) {
			Grid grid = randomGrid(random, round % 3 == 0 ? 45 : 25);
			int[] cheapest = cheapestCosts(grid);
			int fewest = fewestSteps(grid);
			Paths.Search[] searches = Paths.all(grid);
			boolean way = cheapest[grid.goal] != Integer.MAX_VALUE;
			reachable += way ? 1 : 0;
			for (Paths.Search search : searches) {
				String name = Paths.NAMES[search.algorithm] + " on map " + round;
				check(search.found() == way, name + ": finds a way exactly when there is one");
				check(search.order[0] == grid.start, name + ": looks at the start first");
				check(distinct(search.order), name + ": looks at each cell once");
				for (int i = 0; i < search.order.length; i++) {
					int cell = search.order[i];
					check(grid.cells[cell] != Grid.WALL, name + ": never looks at a wall");
					check(search.seenAt[cell] >= 0 && search.seenAt[cell] <= i, name + ": sees a cell before looking at it");
				}
				if (!way) {
					continue;
				}
				check(search.order[search.order.length - 1] == grid.goal, name + ": stops at the goal");
				check(isWay(grid, search.path, search.cost), name + ": the way is a real one and its cost adds up");
				check(search.cost >= cheapest[grid.goal], name + ": no way is cheaper than the cheapest");
				check(search.steps() >= fewest, name + ": no way is shorter than the shortest");
			}
			if (way) {
				check(searches[Paths.BREADTH].steps() == fewest, "Breadth-first finds the fewest steps on map " + round);
				check(searches[Paths.DIJKSTRA].cost == cheapest[grid.goal], "Dijkstra finds the cheapest way on map " + round);
				check(searches[Paths.A_STAR].cost == cheapest[grid.goal], "A* finds the cheapest way on map " + round);
				check(searches[Paths.A_STAR].order.length <= searches[Paths.DIJKSTRA].order.length,
						"A* looks at no more cells than Dijkstra on map " + round);
			}
		}
		check(reachable > 100 && reachable < 300, "the random maps include ones with and without a way (" + reachable + ")");

		// The lessons the example map shows.
		Paths.Search[] sample = Paths.all(Grid.sample());
		check(sample[Paths.BREADTH].cost > sample[Paths.DIJKSTRA].cost, "on the example, Breadth-first walks through mud and pays for it");
		check(sample[Paths.BREADTH].steps() < sample[Paths.DIJKSTRA].steps(), "on the example, Dijkstra takes more steps around the mud");
		check(sample[Paths.A_STAR].order.length * 2 < sample[Paths.DIJKSTRA].order.length, "on the example, A* looks at under half the cells Dijkstra does");
		check(sample[Paths.GREEDY].cost > sample[Paths.A_STAR].cost, "on the example, Greedy is fooled into a dearer way");
		check(sample[Paths.DEPTH].steps() > sample[Paths.BREADTH].steps() * 2, "on the example, Depth-first wanders");

		// Every new maze has a way through, with its ends on open ground.
		for (int seed = 0; seed < 100; seed++) {
			Grid maze = Grid.maze(new Random(seed));
			check(maze.cells[maze.start] == Grid.OPEN && maze.cells[maze.goal] == Grid.OPEN, "maze " + seed + ": ends on open ground");
			check(Paths.run(Paths.BREADTH, maze).found(), "maze " + seed + ": has a way through");
		}

		// Saving and loading give back the same map and search; a broken file gives nothing.
		File file = File.createTempFile("paths", ".txt");
		file.deleteOnExit();
		Grid maze = Grid.maze(new Random(5));
		new Grid.Saved(maze, Paths.GREEDY).save(file);
		Grid.Saved loaded = Grid.Saved.load(file);
		check(loaded != null && loaded.search == Paths.GREEDY, "the saved search comes back");
		check(loaded.grid.lines().equals(maze.lines()) && loaded.grid.start == maze.start && loaded.grid.goal == maze.goal,
				"the saved map comes back");
		java.nio.file.Files.write(file.toPath(), Arrays.asList("search=2", "S..G"));
		check(Grid.Saved.load(file) == null, "a file that is not a map loads as nothing");
		System.out.println("PathCheck passed: 300 random maps (" + reachable + " with a way), the example and 100 mazes.");
	}

	private static Grid randomGrid(Random random, int wallPercent) {
		Grid grid = new Grid();
		for (int i = 0; i < grid.cells.length; i++) {
			int roll = random.nextInt(100);
			grid.cells[i] = roll < wallPercent ? Grid.WALL : roll < wallPercent + 15 ? Grid.MUD : Grid.OPEN;
		}
		grid.start = random.nextInt(grid.cells.length);
		do {
			grid.goal = random.nextInt(grid.cells.length);
		} while (grid.goal == grid.start);
		grid.cells[grid.start] = Grid.OPEN;
		grid.cells[grid.goal] = random.nextBoolean() ? Grid.OPEN : Grid.MUD;
		return grid;
	}

	/** The cheapest cost to every cell, by relaxing every step until nothing changes (Bellman-Ford). */
	private static int[] cheapestCosts(Grid grid) {
		int[] cost = new int[grid.cells.length];
		Arrays.fill(cost, Integer.MAX_VALUE);
		cost[grid.start] = 0;
		for (boolean changed = true; changed;) {
			changed = false;
			for (int cell = 0; cell < cost.length; cell++) {
				if (cost[cell] == Integer.MAX_VALUE || grid.cells[cell] == Grid.WALL) {
					continue;
				}
				for (int next : grid.neighbors(cell)) {
					if (cost[cell] + grid.cost(next) < cost[next]) {
						cost[next] = cost[cell] + grid.cost(next);
						changed = true;
					}
				}
			}
		}
		return cost;
	}

	/** The fewest steps from start to goal, found the same slow way with every step costing 1. */
	private static int fewestSteps(Grid grid) {
		Grid flat = grid.copy();
		for (int i = 0; i < flat.cells.length; i++) {
			if (flat.cells[i] == Grid.MUD) {
				flat.cells[i] = Grid.OPEN;
			}
		}
		int steps = cheapestCosts(flat)[grid.goal];
		return steps == Integer.MAX_VALUE ? -1 : steps;
	}

	private static boolean isWay(Grid grid, int[] path, int cost) {
		if (path[0] != grid.start || path[path.length - 1] != grid.goal || !distinct(path)) {
			return false;
		}
		int total = 0;
		for (int i = 1; i < path.length; i++) {
			int a = path[i - 1];
			int b = path[i];
			boolean next = Math.abs(a % Grid.COLS - b % Grid.COLS) + Math.abs(a / Grid.COLS - b / Grid.COLS) == 1;
			if (!next || grid.cells[b] == Grid.WALL) {
				return false;
			}
			total += grid.cost(b);
		}
		return total == cost;
	}

	private static boolean distinct(int[] cells) {
		int[] sorted = cells.clone();
		Arrays.sort(sorted);
		for (int i = 1; i < sorted.length; i++) {
			if (sorted[i] == sorted[i - 1]) {
				return false;
			}
		}
		return true;
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("FAILED: " + what);
		}
	}
}
