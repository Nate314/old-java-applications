package musictheoryexpert;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A guitar in standard tuning: finds how to finger notes on it. A shape is one fret per string, low E first, with -1
 * for a string not played and 0 for an open string.
 */
final class Guitar {
	/** E2 A2 D3 G3 B3 E4, low string first. */
	static final int[] TUNING = { 40, 45, 50, 55, 59, 64 };
	static final int FRETS = 12;
	static final String[] STRING_NAMES = { "E", "A", "D", "G", "B", "E" };

	private Guitar() {
	}

	/** The notes a shape sounds, low to high. */
	static List<Integer> notes(int[] frets) {
		TreeSet<Integer> notes = new TreeSet<>();
		for (int s = 0; s < frets.length; s++) {
			if (frets[s] >= 0) {
				notes.add(TUNING[s] + frets[s]);
			}
		}
		return new ArrayList<>(notes);
	}

	/**
	 * The easiest full shape for the chord in {@code notes}: every note name of it rings, nothing else, on four strings
	 * or more, and its lowest note keeps the bass. Null when no hand can play it.
	 */
	static int[] shape(List<Integer> notes) {
		if (notes.isEmpty()) {
			return null;
		}
		int mask = ChordNamer.maskOf(notes);
		int bass = notes.get(0) % 12;
		int[][] candidates = new int[6][];
		for (int s = 0; s < 6; s++) {
			List<Integer> frets = new ArrayList<>();
			for (int f = 0; f <= FRETS; f++) {
				if ((mask >> ((TUNING[s] + f) % 12) & 1) != 0) {
					frets.add(f);
				}
			}
			candidates[s] = toArray(frets);
		}
		return best(candidates, frets -> {
			List<Integer> rung = notes(frets);
			return !rung.isEmpty() && rung.get(0) % 12 == bass && ChordNamer.maskOf(rung) == mask
					&& sounding(frets) >= Math.max(4, Integer.bitCount(mask));
		});
	}

	/** The easiest way to finger exactly {@code notes}, each on its own string; null when they cannot be held at once. */
	static int[] place(List<Integer> notes) {
		int[][] candidates = new int[6][];
		for (int s = 0; s < 6; s++) {
			List<Integer> frets = new ArrayList<>();
			for (int note : notes) {
				if (note - TUNING[s] >= 0 && note - TUNING[s] <= FRETS) {
					frets.add(note - TUNING[s]);
				}
			}
			candidates[s] = toArray(frets);
		}
		List<Integer> wanted = new ArrayList<>(new TreeSet<>(notes));
		return best(candidates, frets -> notes(frets).equals(wanted) && sounding(frets) == wanted.size());
	}

	private interface Test {
		boolean ok(int[] frets);
	}

	/** Of every shape with a string's fret from its candidates (or not played), the cheapest playable one that passes. */
	private static int[] best(int[][] candidates, Test test) {
		int[] frets = new int[6];
		int[][] found = { null };
		int[] lowest = { Integer.MAX_VALUE };
		search(candidates, 0, frets, test, found, lowest);
		return found[0];
	}

	private static void search(int[][] candidates, int string, int[] frets, Test test, int[][] found, int[] lowest) {
		if (string == 6) {
			if (playable(frets) && test.ok(frets)) {
				int cost = cost(frets);
				if (cost < lowest[0]) {
					lowest[0] = cost;
					found[0] = frets.clone();
				}
			}
			return;
		}
		frets[string] = -1;
		search(candidates, string + 1, frets, test, found, lowest);
		for (int f : candidates[string]) {
			frets[string] = f;
			search(candidates, string + 1, frets, test, found, lowest);
		}
	}

	/** At most four fingers (a barre is one) within four frets. */
	static boolean playable(int[] frets) {
		int low = Integer.MAX_VALUE, high = 0;
		for (int f : frets) {
			if (f > 0) {
				low = Math.min(low, f);
				high = Math.max(high, f);
			}
		}
		return high == 0 || high - low <= 3 && fingers(frets) <= 4;
	}

	/**
	 * The fret one finger lies across, or 0: the lowest fretted fret, held on two or more strings, with every string
	 * from the first of them up to the top one fretted at it or higher.
	 */
	static int barre(int[] frets) {
		int low = Integer.MAX_VALUE, count = 0, first = -1;
		for (int s = 0; s < 6; s++) {
			if (frets[s] > 0 && frets[s] < low) {
				low = frets[s];
			}
		}
		for (int s = 0; s < 6; s++) {
			if (frets[s] == low) {
				count++;
				first = first < 0 ? s : first;
			}
		}
		if (count < 2) {
			return 0;
		}
		for (int s = first; s < 6; s++) {
			if (frets[s] < low) {
				return 0;
			}
		}
		return low;
	}

	static int fingers(int[] frets) {
		int barre = barre(frets), fingers = barre > 0 ? 1 : 0;
		for (int f : frets) {
			fingers += f > 0 && f != barre ? 1 : 0;
		}
		return fingers;
	}

	private static int sounding(int[] frets) {
		int count = 0;
		for (int f : frets) {
			count += f >= 0 ? 1 : 0;
		}
		return count;
	}

	/**
	 * Lower is easier: low on the neck, few fingers, no barre, every string played and none skipped in the middle;
	 * an empty shape costs nothing;
	 * ties go to the shape with more strings ringing.
	 */
	private static int cost(int[] frets) {
		int position = Integer.MAX_VALUE, high = 0, muted = 0;
		boolean open = false;
		for (int s = 0; s < 6; s++) {
			if (frets[s] > 0) {
				position = Math.min(position, frets[s]);
				high = Math.max(high, frets[s]);
			}
			open |= frets[s] == 0;
			if (frets[s] < 0) {
				boolean below = false, above = false;
				for (int t = 0; t < 6; t++) {
					below |= t < s && frets[t] >= 0;
					above |= t > s && frets[t] >= 0;
				}
				// Leaving out bass strings is usual, the top strings less so, and skipping one in the middle is hard.
				muted += below && above ? 8 : below ? 3 : 1;
			}
		}
		position = position == Integer.MAX_VALUE ? 0 : position;
		// Open strings belong with the first three frets; reaching past them while strings ring open is awkward.
		int cost = 3 * position + 2 * fingers(frets) + (barre(frets) > 0 ? 3 : 0) + muted + (open && high > 3 ? 10 : 0);
		return cost * 10 - sounding(frets);
	}

	/** A shape the way guitarists write it, low string first: "x32010", with frets past 9 in brackets. */
	static String text(int[] frets) {
		StringBuilder text = new StringBuilder();
		for (int f : frets) {
			text.append(f < 0 ? "x" : f > 9 ? "(" + f + ")" : String.valueOf(f));
		}
		return text.toString();
	}

	private static int[] toArray(List<Integer> list) {
		int[] array = new int[list.size()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}
}
