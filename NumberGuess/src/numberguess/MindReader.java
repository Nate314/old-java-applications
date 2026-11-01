package numberguess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The game turned around: you think of a number from 1 to {@link #MAX} and the computer guesses it, always in the
 * middle of what your answers leave possible, so it never needs more than {@link #par()} guesses. Answers that
 * leave nothing possible mean the number changed (or a slip of the finger), which {@link #isCaught()} reports.
 */
public class MindReader {
	public static final int MAX = 100;

	private int low = 1;
	private int high = MAX;
	private int guess = middle(1, MAX);
	private boolean found;
	private final List<Integer> guesses = new ArrayList<>(Collections.singletonList(guess));

	private static int middle(int low, int high) {
		return (low + high) / 2;
	}

	public static int par() {
		return Level.HUNDRED.par();
	}

	/** The computer's current guess. */
	public int guess() {
		return guess;
	}

	public List<Integer> guesses() {
		return Collections.unmodifiableList(guesses);
	}

	public int low() {
		return low;
	}

	public int high() {
		return high;
	}

	/** Your number is higher than the guess. */
	public void higher() {
		answer(guess + 1, high);
	}

	/** Your number is lower than the guess. */
	public void lower() {
		answer(low, guess - 1);
	}

	/** The guess is your number. */
	public void correct() {
		if (!isOver()) {
			low = high = guess;
			found = true;
		}
	}

	private void answer(int newLow, int newHigh) {
		if (isOver()) {
			return;
		}
		low = newLow;
		high = newHigh;
		if (!isCaught()) {
			guess = middle(low, high);
			guesses.add(guess);
		}
	}

	public boolean isFound() {
		return found;
	}

	/** True when the answers so far leave no number possible. */
	public boolean isCaught() {
		return low > high;
	}

	public boolean isOver() {
		return found || isCaught();
	}
}
