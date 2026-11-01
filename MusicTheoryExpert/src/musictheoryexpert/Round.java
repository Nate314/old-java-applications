package musictheoryexpert;

import java.util.Random;

/**
 * One round of a drill: ten questions, each answered once. Tracks the score and the streak of right answers in a
 * row. 10 right is three stars, 8 or more two, 6 or more one; one star passes the drill and opens the next one.
 */
public final class Round {
	public static final int QUESTIONS = 10;
	public static final int UNANSWERED = 0, RIGHT = 1, WRONG = 2;

	private final Drill drill;
	private final Random random;
	private final int[] results = new int[QUESTIONS];
	private Drill.Question question;
	private int index;
	private int score;
	private int streak;
	private int bestStreak;

	public Round(Drill drill, Random random) {
		this.drill = drill;
		this.random = random;
		this.question = drill.ask(random);
	}

	public Drill drill() {
		return drill;
	}

	public Drill.Question question() {
		return question;
	}

	/** The current question, from 0. */
	public int index() {
		return index;
	}

	public int result(int question) {
		return results[question];
	}

	public boolean answered() {
		return results[index] != UNANSWERED;
	}

	/** Answers the current question (once); returns true when {@code choice} is right. */
	public boolean answer(int choice) {
		if (answered()) {
			return false;
		}
		boolean right = choice == question.answer();
		results[index] = right ? RIGHT : WRONG;
		if (right) {
			score++;
			streak++;
			bestStreak = Math.max(bestStreak, streak);
		} else {
			streak = 0;
		}
		return right;
	}

	/** True after the last question is answered. */
	public boolean isOver() {
		return index == QUESTIONS - 1 && answered();
	}

	/** Moves on to a new question once the current one is answered. */
	public void next() {
		if (answered() && !isOver()) {
			index++;
			question = drill.ask(random);
		}
	}

	public int score() {
		return score;
	}

	public int streak() {
		return streak;
	}

	public int bestStreak() {
		return bestStreak;
	}

	public int stars() {
		return starsFor(score);
	}

	public static int starsFor(int score) {
		return score >= QUESTIONS ? 3 : score >= 8 ? 2 : score >= 6 ? 1 : 0;
	}
}
