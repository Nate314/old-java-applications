package numberguess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The game turned around: you think of a number from 1 to {@link #MAX} and the computer guesses it, always in the
 * middle of what your answers leave possible, so it never needs more than {@link #par()} guesses. Answers that
 * leave nothing possible mean the number changed (or a slip of the finger), which {@link #isCaught()} reports.
 * <p>
 * With one lie allowed, a number stays possible until it breaks two answers, and each guess is the one that leaves
 * the least work for the worst answer (Berlekamp's volume: a number that may still be lied about counts as one per
 * way the questions left could hide the lie). "That's it" is always believed.
 */
public class MindReader {
	public static final int MAX = 100;
	/** The most guesses lie mode needs, for any number and any one lie (the self-check plays them all). */
	public static final int LIAR_PAR = 11;

	/** The truthful interval, kept as before for the honest game and its "caught" message. */
	private int low = 1;
	private int high = MAX;
	private final int lies;
	/** How many answers each number breaks (index 1 to MAX). */
	private final int[] broken = new int[MAX + 1];
	/** Each answer: 1 higher, -1 lower. */
	private final List<Integer> answers = new ArrayList<>();
	private int guess;
	private boolean found;
	private final List<Integer> guesses = new ArrayList<>();

	public MindReader() {
		this(0);
	}

	/** {@code lies}: 0 for the honest game, 1 to allow one lie. */
	public MindReader(int lies) {
		this.lies = lies;
		guess = nextGuess();
		guesses.add(guess);
	}

	private static int middle(int low, int high) {
		return (low + high) / 2;
	}

	public static int par() {
		return Level.HUNDRED.par();
	}

	public boolean allowsLie() {
		return lies > 0;
	}

	/** The computer's current guess. */
	public int guess() {
		return guess;
	}

	public List<Integer> guesses() {
		return Collections.unmodifiableList(guesses);
	}

	/** Each answer so far: 1 for higher, -1 for lower. */
	public List<Integer> answers() {
		return Collections.unmodifiableList(answers);
	}

	/** How many answers {@code number} breaks. */
	public int broken(int number) {
		return broken[number];
	}

	public boolean isPossible(int number) {
		return number >= 1 && number <= MAX && broken[number] <= lies;
	}

	/** The lowest possible number (once found, the number; once caught, the honest game's empty interval as before). */
	public int low() {
		for (int n = 1; n <= MAX && lies > 0 && !found; n++) {
			if (isPossible(n)) {
				return n;
			}
		}
		return lies > 0 && !found ? MAX + 1 : low;
	}

	public int high() {
		for (int n = MAX; n >= 1 && lies > 0 && !found; n--) {
			if (isPossible(n)) {
				return n;
			}
		}
		return lies > 0 && !found ? 0 : high;
	}

	/** Your number is higher than the guess. */
	public void higher() {
		answer(1);
	}

	/** Your number is lower than the guess. */
	public void lower() {
		answer(-1);
	}

	/** The guess is your number. */
	public void correct() {
		if (!isOver()) {
			low = high = guess;
			found = true;
		}
	}

	/** The answer that was a lie about the number found ({@code answers()} index), or -1 for none. */
	public int lie() {
		for (int i = 0; found && i < answers.size(); i++) {
			if (Integer.compare(guess, guesses.get(i)) != answers.get(i)) {
				return i;
			}
		}
		return -1;
	}

	private void answer(int direction) {
		if (isOver()) {
			return;
		}
		answers.add(direction);
		for (int n = 1; n <= MAX; n++) {
			if (Integer.compare(n, guess) != direction) {
				broken[n]++;
			}
		}
		if (direction > 0) {
			low = guess + 1;
		} else {
			high = guess - 1;
		}
		if (!isCaught()) {
			guess = nextGuess();
			guesses.add(guess);
		}
	}

	private int nextGuess() {
		if (lies == 0) {
			return middle(low, high);
		}
		int questions = 0;
		while (volume(0, 0, questions) > 1L << questions) {
			questions++;
		}
		int best = -1;
		long bestWorst = Long.MAX_VALUE;
		for (int g = 1; g <= MAX; g++) {
			if (isPossible(g)) {
				long worst = Math.max(volume(g, -1, questions - 1), volume(g, 1, questions - 1));
				if (worst < bestWorst) {
					bestWorst = worst;
					best = g;
				}
			}
		}
		return best;
	}

	/**
	 * The work left after answering {@code direction} to {@code g} (or now, for direction 0), with {@code questions}
	 * questions to go: a number that may still be lied about weighs {@code questions + 1}, one that may not weighs 1.
	 */
	private long volume(int g, int direction, int questions) {
		long volume = 0;
		for (int n = 1; n <= MAX; n++) {
			int b = broken[n] + (direction != 0 && Integer.compare(n, g) != direction ? 1 : 0);
			if (b <= lies) {
				volume += b < lies ? questions + 1 : 1;
			}
		}
		return volume;
	}

	public boolean isFound() {
		return found;
	}

	/** True when the answers so far leave no number possible. */
	public boolean isCaught() {
		if (lies == 0) {
			return low > high;
		}
		return low() > MAX;
	}

	public boolean isOver() {
		return found || isCaught();
	}
}
