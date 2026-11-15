package musictheoryexpert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import musictheoryexpert.ChordNamer.Quality;
import musictheoryexpert.ChordNamer.Reading;

/** Self-check for the Chord finder's guitar shapes. Run with java -ea. */
public class GuitarCheck {
	public static void main(String[] args) {
		// The open chords every guitarist learns first, and the two barre chords after them.
		shape(0, Quality.MAJOR, "x32010");
		shape(7, Quality.MAJOR, "320003");
		shape(2, Quality.MAJOR, "xx0232");
		shape(9, Quality.MAJOR, "x02220");
		shape(4, Quality.MAJOR, "022100");
		shape(9, Quality.MINOR, "x02210");
		shape(4, Quality.MINOR, "022000");
		shape(2, Quality.MINOR, "xx0231");
		shape(5, Quality.MAJOR, "133211");
		shape(11, Quality.MINOR, "x24432");
		check(Guitar.barre(new int[] { 1, 3, 3, 2, 1, 1 }) == 1, "F is barred at 1");
		check(Guitar.barre(new int[] { -1, 0, 2, 2, 2, 0 }) == 0, "A is not a barre: the top string is open");
		check(Guitar.fingers(new int[] { 1, 3, 3, 2, 1, 1 }) == 4, "F takes four fingers");
		check(!Guitar.playable(new int[] { 1, 6, -1, -1, -1, -1 }), "a five-fret stretch is not playable");
		check(Guitar.text(new int[] { -1, 10, 12, 12, 11, 10 }).equals("x(10)(12)(12)(11)(10)"), "frets past 9 in brackets");

		// Every root of the usual types has a shape that rings its notes, keeps its root in the bass and names back.
		Quality[] usual = { Quality.MAJOR, Quality.MINOR, Quality.DOMINANT_7, Quality.MAJOR_7, Quality.MINOR_7,
				Quality.SUS2, Quality.SUS4, Quality.DIMINISHED, Quality.AUGMENTED, Quality.HALF_DIMINISHED,
				Quality.DIMINISHED_7, Quality.SIXTH, Quality.MINOR_6, Quality.ADD9, Quality.NINTH };
		for (Quality quality : usual) {
			for (int r = 0; r < 12; r++) {
				int root = r;
				List<Integer> chord = ChordNamer.build(root, quality);
				int[] frets = Guitar.shape(chord);
				check(frets != null, "no shape for " + root + " " + quality);
				List<Integer> notes = Guitar.notes(frets);
				check(Guitar.playable(frets), "unplayable " + Guitar.text(frets));
				check(ChordNamer.maskOf(notes) == ChordNamer.maskOf(chord), "wrong notes in " + Guitar.text(frets));
				check(notes.get(0) % 12 == root, "root not in the bass: " + Guitar.text(frets));
				List<Reading> named = ChordNamer.name(notes);
				check(!named.isEmpty() && named.stream().anyMatch(n -> n.root == root && n.quality == quality),
						Guitar.text(frets) + " does not name " + root + " " + quality);
			}
		}

		// Placing notes exactly: each on its own string, or null when a hand cannot hold them.
		check(Arrays.equals(Guitar.place(notes(48, 52, 55)), new int[] { -1, 3, 2, 0, -1, -1 }), "C3 E3 G3 low on the neck: "
				+ Guitar.text(Guitar.place(notes(48, 52, 55))));
		check(Arrays.equals(Guitar.place(new ArrayList<>()), new int[] { -1, -1, -1, -1, -1, -1 }), "nothing held, nothing played");
		check(Guitar.place(notes(40, 41, 42, 43, 44, 45, 46)) == null, "seven notes on six strings");
		check(Guitar.place(notes(40, 88)) == null, "E2 and E6 are too far apart");
		check(Guitar.notes(Guitar.place(notes(60, 64, 67))).equals(notes(60, 64, 67)), "C4 E4 G4 placed as held");
		System.out.println("GuitarCheck passed");
	}

	private static void shape(int root, Quality quality, String expected) {
		String got = Guitar.text(Guitar.shape(ChordNamer.build(root, quality)));
		check(got.equals(expected), root + " " + quality + ": expected " + expected + ", got " + got);
	}

	private static List<Integer> notes(Integer... notes) {
		return new ArrayList<>(Arrays.asList(notes));
	}

	private static void check(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}
}
