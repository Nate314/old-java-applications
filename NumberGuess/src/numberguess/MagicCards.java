package numberguess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The binary card trick: think of a number from 1 to {@link #MAX}, say whether it is on each of six cards, and the
 * number is the sum of the smallest numbers (the key numbers 1, 2, 4, 8, 16 and 32) on the cards you said yes to.
 * Each card holds the 32 numbers whose binary digit for its key number is 1. The cards come in a shuffled order with
 * their numbers shuffled, so the pattern is not easy to see.
 */
public class MagicCards {
	public static final int CARDS = 6;
	public static final int MAX = (1 << CARDS) - 1;

	/** The binary digit each card stands for, in the order the cards are shown. */
	private final List<Integer> order = new ArrayList<>();
	private final List<List<Integer>> numbers = new ArrayList<>();
	private final List<Boolean> answers = new ArrayList<>();

	public MagicCards(Random random) {
		for (int bit = 0; bit < CARDS; bit++) {
			order.add(bit);
		}
		Collections.shuffle(order, random);
		for (int bit : order) {
			List<Integer> card = new ArrayList<>();
			for (int n = 1; n <= MAX; n++) {
				if ((n >> bit & 1) == 1) {
					card.add(n);
				}
			}
			Collections.shuffle(card, random);
			numbers.add(card);
		}
	}

	/** The numbers on card {@code index} (0 is the first shown), in the order they are printed. */
	public List<Integer> numbers(int index) {
		return Collections.unmodifiableList(numbers.get(index));
	}

	/** The key number of card {@code index}: its smallest number, a power of two. */
	public int key(int index) {
		return 1 << order.get(index);
	}

	/** The card being asked about, or {@link #CARDS} when all are answered. */
	public int current() {
		return answers.size();
	}

	/** Your number is ({@code yes}) or is not on the current card. */
	public void answer(boolean yes) {
		if (!isDone()) {
			answers.add(yes);
		}
	}

	/** The answer for card {@code index}, or null if it has not been asked yet. */
	public Boolean answer(int index) {
		return index < answers.size() ? answers.get(index) : null;
	}

	public boolean isDone() {
		return answers.size() == CARDS;
	}

	/** The number read so far: the sum of the key numbers of the cards you said yes to. 0 means on no card. */
	public int number() {
		int sum = 0;
		for (int i = 0; i < answers.size(); i++) {
			if (answers.get(i)) {
				sum += key(i);
			}
		}
		return sum;
	}
}
