package simplealgorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Five ways to find a way from the start to the goal of a {@link Grid}. Each search records the order it looked at
 * cells in (took them off its list to try their neighbors) and when it first saw each one, so the page can replay
 * it cell by cell, and stops when it looks at the goal.
 */
final class Paths {
	static final int BREADTH = 0;
	static final int DEPTH = 1;
	static final int DIJKSTRA = 2;
	static final int GREEDY = 3;
	static final int A_STAR = 4;

	static final String[] NAMES = { "Breadth-first", "Depth-first", "Dijkstra", "Greedy", "A*" };
	/** The names on the bottom bar's button, short so the bar fits. */
	static final String[] SHORT_NAMES = { "Breadth", "Depth", "Dijkstra", "Greedy", "A*" };
	static final String[] IDEAS = {
			"Looks at cells in rings around the start, nearest first, using a queue. It finds the fewest steps,"
					+ " but mud costs it nothing to think about, so it walks straight through.",
			"Follows one way as far as it goes, then backs up, using a stack. Quick to write, and it finds a way"
					+ " if there is one, but rarely a short one.",
			"Always looks next at the cell that is cheapest to reach so far. It finds the cheapest way, going"
					+ " around mud when that costs less, but looks in every direction.",
			"Always looks next at the cell that seems nearest the goal, ignoring what it cost to get there. Fast"
					+ " in the open, fooled by walls between it and the goal.",
			"Dijkstra plus Greedy: cost so far plus the straight-line steps still to go. It still finds the"
					+ " cheapest way, while looking at far fewer cells." };

	/** What one search did: the cells it looked at, in order, and the way it found. */
	static final class Search {
		final int algorithm;
		/** Cells in the order they were looked at. */
		final int[] order;
		/** For each cell, how many cells had been looked at when this one was first seen (-1: never seen). */
		final int[] seenAt;
		/** For each cell, its place in {@link #order} (-1: never looked at). */
		final int[] lookedAt;
		/** Start to goal, or null when there is no way through. */
		final int[] path;
		final int cost;

		Search(int algorithm, int[] order, int[] seenAt, int[] path, int cost) {
			this.algorithm = algorithm;
			this.order = order;
			this.seenAt = seenAt;
			this.path = path;
			this.cost = cost;
			lookedAt = new int[seenAt.length];
			Arrays.fill(lookedAt, -1);
			for (int i = 0; i < order.length; i++) {
				lookedAt[order[i]] = i;
			}
		}

		boolean found() {
			return path != null;
		}

		int steps() {
			return path == null ? -1 : path.length - 1;
		}
	}

	private Paths() {
	}

	static Search[] all(Grid grid) {
		Search[] searches = new Search[NAMES.length];
		for (int i = 0; i < searches.length; i++) {
			searches[i] = run(i, grid);
		}
		return searches;
	}

	static Search run(int algorithm, Grid grid) {
		int n = grid.cells.length;
		int[] seenAt = new int[n];
		int[] parent = new int[n];
		boolean[] closed = new boolean[n];
		Arrays.fill(seenAt, -1);
		Arrays.fill(parent, -1);
		List<Integer> order = new ArrayList<>();
		seenAt[grid.start] = 0;
		if (algorithm == BREADTH) {
			ArrayDeque<Integer> queue = new ArrayDeque<>();
			queue.add(grid.start);
			while (!queue.isEmpty()) {
				int cell = queue.poll();
				order.add(cell);
				if (cell == grid.goal) {
					break;
				}
				for (int next : grid.neighbors(cell)) {
					if (seenAt[next] < 0) {
						seenAt[next] = order.size();
						parent[next] = cell;
						queue.add(next);
					}
				}
			}
		} else if (algorithm == DEPTH) {
			// Entries are {cell, the cell that pushed it}; the latest push of a cell is the one taken first.
			ArrayDeque<int[]> stack = new ArrayDeque<>();
			stack.push(new int[] { grid.start, -1 });
			while (!stack.isEmpty()) {
				int[] entry = stack.pop();
				int cell = entry[0];
				if (closed[cell]) {
					continue;
				}
				closed[cell] = true;
				parent[cell] = entry[1];
				order.add(cell);
				if (cell == grid.goal) {
					break;
				}
				int[] next = grid.neighbors(cell);
				// Pushed in reverse, so up is tried first, then right, down and left.
				for (int i = next.length - 1; i >= 0; i--) {
					if (!closed[next[i]]) {
						if (seenAt[next[i]] < 0) {
							seenAt[next[i]] = order.size();
						}
						stack.push(new int[] { next[i], cell });
					}
				}
			}
		} else {
			// Entries are {priority, tie-break, cell, cost so far}; ties go to the entry added first.
			int[] best = new int[n];
			Arrays.fill(best, Integer.MAX_VALUE);
			best[grid.start] = 0;
			PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0])
					: a[1] != b[1] ? Integer.compare(a[1], b[1]) : Integer.compare(a[4], b[4]));
			int[] added = { 0 };
			queue.add(entry(algorithm, grid, grid.start, 0, added));
			while (!queue.isEmpty()) {
				int[] entry = queue.poll();
				int cell = entry[2];
				if (closed[cell]) {
					continue;
				}
				closed[cell] = true;
				order.add(cell);
				if (cell == grid.goal) {
					break;
				}
				for (int next : grid.neighbors(cell)) {
					int cost = entry[3] + grid.cost(next);
					// Greedy keeps the first way it saw to a cell; the other two keep the cheapest.
					boolean better = algorithm == GREEDY ? seenAt[next] < 0 : cost < best[next];
					if (!closed[next] && better) {
						if (seenAt[next] < 0) {
							seenAt[next] = order.size();
						}
						best[next] = cost;
						parent[next] = cell;
						queue.add(entry(algorithm, grid, next, cost, added));
					}
				}
			}
		}
		int[] path = null;
		int cost = 0;
		if (!order.isEmpty() && order.get(order.size() - 1) == grid.goal) {
			List<Integer> way = new ArrayList<>();
			for (int cell = grid.goal; cell >= 0; cell = parent[cell]) {
				way.add(0, cell);
			}
			path = new int[way.size()];
			for (int i = 0; i < path.length; i++) {
				path[i] = way.get(i);
				cost += i > 0 ? grid.cost(path[i]) : 0;
			}
		}
		int[] looked = new int[order.size()];
		for (int i = 0; i < looked.length; i++) {
			looked[i] = order.get(i);
		}
		return new Search(algorithm, looked, seenAt, path, cost);
	}

	/**
	 * A queue entry. Dijkstra orders by cost so far, Greedy by steps left, and A* by their sum with ties going to
	 * the cell nearer the goal; after that, the cell added first.
	 */
	private static int[] entry(int algorithm, Grid grid, int cell, int cost, int[] added) {
		int left = grid.distanceToGoal(cell);
		int priority = algorithm == DIJKSTRA ? cost : algorithm == GREEDY ? left : cost + left;
		int tie = algorithm == A_STAR ? left : 0;
		return new int[] { priority, tie, cell, cost, added[0]++ };
	}
}
