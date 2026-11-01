package numberguess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One secret number in a {@link Level}'s range. Each guess narrows what is still possible ({@link #low()} to
 * {@link #high()}). A guess outside the range, or one already ruled out by earlier answers, is not counted. The game
 * is over when the number is found or the level's guesses run out.
 */
public class Game {

	public enum Verdict {
		TOO_LOW, TOO_HIGH, CORRECT, OUT_OF_RANGE, RULED_OUT, OVER
	}

	private final Level level;
	private final int number;
	private int low = 1;
	private int high;
	private boolean found;
	private final List<Integer> guesses = new ArrayList<>();

	public Game(Level level, Random random) {
		this(level, random.nextInt(level.max()) + 1);
	}

	/** A game with a known number, for the self-check. */
	Game(Level level, int number) {
		this.level = level;
		this.number = number;
		this.high = level.max();
	}

	public Verdict guess(int guess) {
		if (isOver()) {
			return Verdict.OVER;
		}
		if (guess < 1 || guess > level.max()) {
			return Verdict.OUT_OF_RANGE;
		}
		if (guess < low || guess > high) {
			return Verdict.RULED_OUT;
		}
		guesses.add(guess);
		if (guess < number) {
			low = guess + 1;
			return Verdict.TOO_LOW;
		}
		if (guess > number) {
			high = guess - 1;
			return Verdict.TOO_HIGH;
		}
		low = high = guess;
		found = true;
		return Verdict.CORRECT;
	}

	public Level level() {
		return level;
	}

	/** The secret number; only shown once the game is over. */
	public int number() {
		return number;
	}

	/** The lowest number still possible. */
	public int low() {
		return low;
	}

	/** The highest number still possible. */
	public int high() {
		return high;
	}

	public int getGuesses() {
		return guesses.size();
	}

	public List<Integer> guesses() {
		return Collections.unmodifiableList(guesses);
	}

	public int guessesLeft() {
		return level.limit() - guesses.size();
	}

	public boolean isFound() {
		return found;
	}

	public boolean isOver() {
		return found || guessesLeft() <= 0;
	}

	/** Stars won: 0 until the number is found. */
	public int stars() {
		return found ? level.starsFor(guesses.size()) : 0;
	}
}
