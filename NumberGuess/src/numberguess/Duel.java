package numberguess;

import java.util.Random;

/**
 * A race against the computer. You hide a number from 1 to {@link #MAX} and it hides one too; then each round you
 * guess its number and it guesses yours, every answer given straight away. Finding the other's number in fewer guesses
 * wins and the same number is a draw: since the computer always answers in the round you found it in, going first is
 * no advantage. The computer guesses a random number from the middle half of what is left, so it is close to the
 * best search but has no hiding place it always finds last.
 */
public final class Duel {
	public static final int MAX = 100;

	public enum Result {
		NONE, WON, LOST, DRAWN
	}

	private final Random random;
	/** Your hunt for its number. */
	private final Game yours;
	/** Its hunt for yours: null until you have hidden one. */
	private Game its;

	public Duel(Random random) {
		this(random, random.nextInt(MAX) + 1);
	}

	/** A duel with a known computer's number, for the self-check. */
	Duel(Random random, int itsNumber) {
		this.random = random;
		// Ruled-out guesses are not counted, so no hunt can take more than MAX guesses.
		this.yours = new Game(Level.HUNDRED, itsNumber, MAX);
	}

	/** True until you have hidden your number. */
	public boolean isHiding() {
		return its == null;
	}

	/** Hides your number; false if it is not from 1 to {@link #MAX} or one is already hidden. */
	public boolean hide(int number) {
		if (!isHiding() || number < 1 || number > MAX) {
			return false;
		}
		its = new Game(Level.HUNDRED, number, MAX);
		return true;
	}

	/** Your number, or 0 before it is hidden. */
	public int hidden() {
		return isHiding() ? 0 : its.number();
	}

	public boolean isYourTurn() {
		return !isHiding() && !isOver() && yours.getGuesses() == its.getGuesses();
	}

	public boolean isComputersTurn() {
		return !isHiding() && !isOver() && yours.getGuesses() > its.getGuesses();
	}

	/** Your guess at its number (a guess out of range or ruled out is not counted, and it is still your turn). */
	public Game.Verdict guess(int number) {
		return isYourTurn() ? yours.guess(number) : Game.Verdict.OVER;
	}

	/** The computer's guess at yours: a random number from the middle half of what your answers leave. */
	public int computerGuess() {
		if (!isComputersTurn()) {
			return 0;
		}
		int low = its.low(), count = its.high() - low + 1;
		int quarter = count / 4;
		int guess = low + quarter + random.nextInt(count - 2 * quarter);
		its.guess(guess);
		return guess;
	}

	/** Over once a round ends with either number found. */
	public boolean isOver() {
		return !isHiding() && yours.getGuesses() == its.getGuesses() && (yours.isFound() || its.isFound());
	}

	public Result result() {
		if (!isOver()) {
			return Result.NONE;
		}
		return yours.isFound() && its.isFound() ? Result.DRAWN : yours.isFound() ? Result.WON : Result.LOST;
	}

	/** The round being played (or the last one, when over), from 1. */
	public int round() {
		return isHiding() ? 1 : its.getGuesses() + (isOver() ? 0 : 1);
	}

	/** Your hunt for its number. */
	public Game yours() {
		return yours;
	}

	/** Its hunt for your number, or null before you have hidden one. */
	public Game its() {
		return its;
	}
}
