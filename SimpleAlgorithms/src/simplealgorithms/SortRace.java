package simplealgorithms;

import java.util.Arrays;
import java.util.List;

/**
 * A race of every sort in {@link Sorts} on the same numbers. A shared clock counts steps; each lane plays its
 * recorded steps until it has spent as many as the clock, so every sort gets the same amount of work per tick.
 */
final class SortRace {
	/** One sort's run: its own copy of the numbers, its steps, and how far it has got. */
	static final class Lane {
		final int algorithm;
		final int[] values;
		final List<Sorts.Step> steps;
		/** The whole run's cost in steps, known from the start; it decides the places. */
		final int total;
		int next;
		int spent;
		int compares;
		int writes;
		Sorts.Step last;
		/** The value the last step touched, for the sound. */
		int touched;
		/** After the sort ends, a check runs along the bars: how many it has passed, or -1 before it starts. */
		int sweep = -1;
		/** The values at {@link #ROWS} moments spread evenly over the race, from the start to the last sort's end. */
		int[][] print;

		Lane(int algorithm, int[] numbers) {
			this.algorithm = algorithm;
			this.values = Arrays.copyOf(numbers, numbers.length);
			this.steps = Sorts.trace(algorithm, numbers);
			this.total = Sorts.cost(steps);
		}

		/** Back to the numbers before the first step. */
		void reset(int[] numbers) {
			System.arraycopy(numbers, 0, values, 0, values.length);
			next = spent = compares = writes = touched = 0;
			last = null;
			sweep = -1;
		}

		boolean finished() {
			return next >= steps.size();
		}

		boolean swept() {
			return sweep >= values.length;
		}

		/** Plays steps while it has spent less than the clock. Returns true if it did anything. */
		boolean catchUp(int clock) {
			boolean moved = false;
			while (!finished() && spent < clock) {
				Sorts.Step step = steps.get(next++);
				step.apply(values);
				spent += step.cost();
				if (step.kind == Sorts.COMPARE) {
					compares++;
				} else {
					writes += step.cost();
				}
				last = step;
				touched = values[step.a];
				moved = true;
			}
			if (finished()) {
				last = null;
			}
			return moved;
		}
	}

	/** Rows in each lane's print: the moments of the race it shows. */
	static final int ROWS = 100;

	final int[] numbers;
	final Lane[] lanes = new Lane[Sorts.NAMES.length];
	final int max;
	/** The steps the slowest sort takes: the length of the race. */
	final int span;
	int clock;
	/** The furthest the clock has been, so a print stays drawn after going back. */
	int reached;

	SortRace(int[] numbers) {
		this.numbers = Arrays.copyOf(numbers, numbers.length);
		int biggest = 1;
		for (int value : numbers) {
			biggest = Math.max(biggest, value);
		}
		this.max = biggest;
		for (int i = 0; i < lanes.length; i++) {
			lanes[i] = new Lane(i, numbers);
		}
		this.span = Math.max(1, last().total);
		for (Lane lane : lanes) {
			lane.print = new int[ROWS][];
			for (int row = 0; row < ROWS; row++) {
				lane.catchUp(timeOf(row));
				lane.print[row] = lane.values.clone();
			}
			lane.reset(numbers);
		}
	}

	/** The first clock at a print row (rounded up, so {@link #rowAt} gives the row back): 0 the start, the last row the end. */
	int timeOf(int row) {
		return (int) (((long) span * row + ROWS - 2) / (ROWS - 1));
	}

	/** The last print row the clock has passed. */
	int rowAt(int time) {
		return (int) Math.min(ROWS - 1, (long) time * (ROWS - 1) / span);
	}

	/** Sets the clock to any time from 0 to the end, as if the race had run there; finished sorts count as checked. */
	void seek(int time) {
		clock = Math.max(0, Math.min(span, time));
		reached = Math.max(reached, clock);
		for (Lane lane : lanes) {
			lane.reset(numbers);
			lane.catchUp(clock);
			if (lane.finished()) {
				lane.sweep = lane.values.length;
			}
		}
	}

	/** Moves the clock on by the given number of steps. */
	void advance(int steps) {
		clock += steps;
		reached = Math.max(reached, clock);
		for (Lane lane : lanes) {
			lane.catchUp(clock);
		}
	}

	/** 1 for the cheapest run; equal runs share a place. */
	int place(Lane lane) {
		int place = 1;
		for (Lane other : lanes) {
			if (other.total < lane.total) {
				place++;
			}
		}
		return place;
	}

	boolean finished() {
		for (Lane lane : lanes) {
			if (!lane.finished()) {
				return false;
			}
		}
		return true;
	}

	/** The lane that came first (the first of equals), or the last one home. */
	Lane first() {
		Lane best = lanes[0];
		for (Lane lane : lanes) {
			if (lane.total < best.total) {
				best = lane;
			}
		}
		return best;
	}

	Lane last() {
		Lane worst = lanes[0];
		for (Lane lane : lanes) {
			if (lane.total > worst.total) {
				worst = lane;
			}
		}
		return worst;
	}
}
