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

		Lane(int algorithm, int[] numbers) {
			this.algorithm = algorithm;
			this.values = Arrays.copyOf(numbers, numbers.length);
			this.steps = Sorts.trace(algorithm, numbers);
			this.total = Sorts.cost(steps);
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

	final int[] numbers;
	final Lane[] lanes = new Lane[Sorts.NAMES.length];
	final int max;
	int clock;

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
	}

	/** Moves the clock on by the given number of steps. */
	void advance(int steps) {
		clock += steps;
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
