package musictheoryexpert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import musictheoryexpert.ChordNamer.InKey;
import musictheoryexpert.ChordNamer.KeyFit;
import musictheoryexpert.ChordNamer.Quality;
import musictheoryexpert.ChordNamer.Reading;

/** Self-check for the Chord finder's chord names, spelling, inversions, keys and key fitting. Run with java -ea. */
public class ChordNamerCheck {
	public static void main(String[] args) {
		check(best(60, 64, 67).equals("C"), "C E G is C: " + best(60, 64, 67));
		check(best(64, 67, 72).equals("C/E"), "E G C is C/E: " + best(64, 67, 72));
		check(first(64, 67, 72).description().equals("C major, first inversion"), first(64, 67, 72).description());
		check(first(67, 72, 76).description().equals("C major, second inversion"), first(67, 72, 76).description());
		check(best(57, 60, 64).equals("Am"), "A C E: " + best(57, 60, 64));
		check(best(67, 71, 74, 77).equals("G7"), "G B D F: " + best(67, 71, 74, 77));
		check(best(65, 67, 71, 74).equals("G7/F"), "F G B D: " + best(65, 67, 71, 74));
		check(first(65, 67, 71, 74).description().equals("G dominant seventh, third inversion"), first(65, 67, 71, 74).description());
		check(best(60, 64, 67, 71).equals("Cmaj7"), "Cmaj7: " + best(60, 64, 67, 71));
		check(best(60, 64, 67, 69).equals("C6"), "C E G A with C low is C6: " + best(60, 64, 67, 69));
		check(best(57, 60, 64, 67).equals("Am7"), "A C E G with A low is Am7: " + best(57, 60, 64, 67));
		check(names(60, 64, 67, 69).contains("Am7/C"), "C6 is also Am7/C: " + names(60, 64, 67, 69));
		check(best(59, 62, 65).equals("Bdim"), "B D F: " + best(59, 62, 65));
		check(best(59, 62, 65, 69).equals("Bm7b5"), "B D F A: " + best(59, 62, 65, 69));
		check(best(59, 62, 65, 68).equals("Bdim7"), "B D F Ab: " + best(59, 62, 65, 68));
		check(first(59, 62, 65, 68).spelling(8).equals("Ab"), "Bdim7 has Ab, not G#: " + first(59, 62, 65, 68).spelling(8));
		check(first(59, 62, 65, 68).label(8).equals("bb7"), "its seventh is bb7: " + first(59, 62, 65, 68).label(8));
		check(best(60, 64, 68).equals("Caug"), "C E G#: " + best(60, 64, 68));
		check(first(60, 64, 68).label(8).equals("#5"), "augmented fifth is #5");
		check(best(60, 62, 67).equals("Csus2"), "C D G: " + best(60, 62, 67));
		check(best(60, 65, 67).equals("Csus4"), "C F G: " + best(60, 65, 67));
		check(best(60, 64, 67, 74).equals("Cadd9"), "C E G D: " + best(60, 64, 67, 74));
		check(best(60, 64, 67, 70, 74).equals("C9"), "C E G Bb D: " + best(60, 64, 67, 70, 74));
		check(best(55, 59, 65).equals("G7"), "G B F is G7 with no fifth: " + best(55, 59, 65));
		check(first(55, 59, 65).noFifth && first(55, 59, 65).description().contains("no fifth"), "no fifth said");
		check(best(48, 55).equals("C5"), "C G is a power chord: " + best(48, 55));
		check(ChordNamer.name(list(60, 72)).isEmpty() && ChordNamer.name(list(60)).isEmpty(), "one pitch is no chord");
		check(ChordNamer.name(list(60, 61, 62)).isEmpty(), "a cluster has no name");
		// Notes held in more than one octave count once.
		check(best(48, 60, 64, 67, 72).equals("C"), "doubled C: " + best(48, 60, 64, 67, 72));

		// Spelling: by letter, the root's simpler name wins.
		check(spelled(68, 72, 75).equals("Ab C Eb"), "Ab major: " + spelled(68, 72, 75));
		check(spelled(61, 65, 68).equals("Db F Ab"), "Db major, not C#: " + spelled(61, 65, 68));
		check(spelled(66, 70, 73).equals("F# A# C#"), "F# major: " + spelled(66, 70, 73));
		check(spelled(68, 71, 75).equals("G# B D#"), "G#m, not Abm: " + spelled(68, 71, 75));
		check(spelled(63, 66, 70).equals("Eb Gb Bb"), "Ebm: " + spelled(63, 66, 70));
		check(ChordNamer.noteName(1).equals("Db") && ChordNamer.noteName(6).equals("F#") && ChordNamer.noteName(10).equals("Bb"),
				"single note names");

		// Intervals.
		check("Major 3rd: C up to E".equals(ChordNamer.interval(list(60, 64))), ChordNamer.interval(list(60, 64)));
		check("Tritone: C up to F#".equals(ChordNamer.interval(list(60, 66))), ChordNamer.interval(list(60, 66)));
		check("Perfect 4th: G up to C".equals(ChordNamer.interval(list(55, 60))), ChordNamer.interval(list(55, 60)));
		check("Minor 3rd: Eb up to Gb".equals(ChordNamer.interval(list(63, 66))), ChordNamer.interval(list(63, 66)));
		check(ChordNamer.interval(list(60, 64, 67)) == null, "three notes are no interval");

		// Every quality on every root names itself back, and builds on the keyboard.
		for (Quality quality : Quality.values()) {
			for (int root = 0; root < 12; root++) {
				List<Integer> notes = ChordNamer.build(root, quality);
				check(notes.get(0) >= 53 && notes.get(0) <= 64 && notes.get(notes.size() - 1) <= Look.HIGH_NOTE,
						"built in range: " + quality + " on " + root);
				boolean found = false;
				for (Reading r : ChordNamer.name(notes)) {
					found |= r.root == root && r.quality == quality && !r.noFifth;
				}
				check(found, "names back " + quality + " on " + root);
				Reading reading = new Reading(root, quality, root, false);
				for (int note : notes) {
					String name = reading.spelling(note);
					check(name != null && !name.contains("##") && !name.contains("bb") || quality == Quality.DIMINISHED_7
							|| quality == Quality.AUGMENTED || quality == Quality.AUGMENTED_7,
							"plain spelling of " + reading.name() + ": " + name);
				}
			}
		}

		// Keys.
		check(texts(ChordNamer.keysOf(7, Quality.MAJOR)).equals(
				"[I in G major, V in C major, V in C minor, IV in D major, VI in B minor, III in E minor, VII in A minor]"),
				"G major's keys: " + texts(ChordNamer.keysOf(7, Quality.MAJOR)));
		check(texts(ChordNamer.keysOf(7, Quality.DOMINANT_7)).equals("[V7 in C major, V7 in C minor, VII7 in A minor]"),
				"G7's keys: " + texts(ChordNamer.keysOf(7, Quality.DOMINANT_7)));
		check(texts(ChordNamer.keysOf(11, Quality.HALF_DIMINISHED)).equals("[iiø7 in A minor, viiø7 in C major]"),
				"Bm7b5's keys: " + texts(ChordNamer.keysOf(11, Quality.HALF_DIMINISHED)));
		check(texts(ChordNamer.keysOf(11, Quality.DIMINISHED_7)).equals("[vii°7 in C minor]"),
				"Bdim7's keys: " + texts(ChordNamer.keysOf(11, Quality.DIMINISHED_7)));
		check(texts(ChordNamer.keysOf(5, Quality.MAJOR_7)).contains("IVmaj7 in C major"), "Fmaj7 is IVmaj7 in C");
		check(texts(ChordNamer.keysOf(2, Quality.MINOR_7)).contains("ii7 in C major"), "Dm7 is ii7 in C");
		check(ChordNamer.keysOf(0, Quality.AUGMENTED).size() == 1 &&texts(ChordNamer.keysOf(0, Quality.AUGMENTED)).contains("III+ in A minor"),
				"Caug is III+ in A, E and G# minor: " + texts(ChordNamer.keysOf(0, Quality.AUGMENTED)));

		// Fitting a list of chords to a key.
		List<Reading> song = new ArrayList<>();
		song.add(new Reading(7, Quality.MAJOR, 7, false));
		song.add(new Reading(4, Quality.MINOR, 4, false));
		song.add(new Reading(0, Quality.MAJOR, 0, false));
		song.add(new Reading(2, Quality.MAJOR, 2, false));
		List<KeyFit> fits = ChordNamer.fitKeys(song, 3);
		check(fits.get(0).keyName().equals("G major") && fits.get(0).fits == 4, "G Em C D is in G major: " + fits.get(0).keyName());
		check(Arrays.equals(fits.get(0).degrees, new int[] { 1, 6, 4, 5 }), "as I vi IV V: " + Arrays.toString(fits.get(0).degrees));
		check(fits.get(1).keyName().equals("E minor") && fits.get(1).fits == 4, "or E minor: " + fits.get(1).keyName());
		song.clear();
		song.add(new Reading(9, Quality.MINOR, 9, false));
		song.add(new Reading(2, Quality.MINOR, 2, false));
		song.add(new Reading(4, Quality.DOMINANT_7, 4, false));
		song.add(new Reading(9, Quality.MINOR, 9, false));
		fits = ChordNamer.fitKeys(song, 3);
		check(fits.get(0).keyName().equals("A minor") && fits.get(0).fits == 4, "Am Dm E7 Am is A minor: " + fits.get(0).keyName());
		check(ChordNamer.fitKeys(new ArrayList<>(), 3).isEmpty(), "no chords, no keys");

		// Every key's songwriter position comes back.
		for (int tonic = 0; tonic < 12; tonic++) {
			check(Progression.tonicAt(ChordNamer.positionOf(tonic, false), false) == tonic
					&& Progression.tonicAt(ChordNamer.positionOf(tonic, true), true) == tonic, "position of " + tonic);
		}
		System.out.println("ChordNamerCheck: all checks passed");
	}

	private static List<Integer> list(int... notes) {
		List<Integer> list = new ArrayList<>();
		for (int note : notes) {
			list.add(note);
		}
		return list;
	}

	private static Reading first(int... notes) {
		return ChordNamer.name(list(notes)).get(0);
	}

	private static String best(int... notes) {
		List<Reading> readings = ChordNamer.name(list(notes));
		return readings.isEmpty() ? "(none)" : readings.get(0).name();
	}

	private static List<String> names(int... notes) {
		List<String> names = new ArrayList<>();
		for (Reading reading : ChordNamer.name(list(notes))) {
			names.add(reading.name());
		}
		return names;
	}

	private static String spelled(int... notes) {
		Reading reading = first(notes);
		StringBuilder text = new StringBuilder();
		for (int note : notes) {
			text.append(text.length() == 0 ? "" : " ").append(reading.spelling(note));
		}
		return text.toString();
	}

	private static String texts(List<InKey> keys) {
		List<String> texts = new ArrayList<>();
		for (InKey key : keys) {
			texts.add(key.text());
		}
		return texts.toString();
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}
}
