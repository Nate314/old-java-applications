package musictheoryexpert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;

import musictheoryexpert.Progression.Preset;
import musictheoryexpert.Progression.Style;
import musictheoryexpert.Progression.Voicing;

/** Self-check for the Songwriter's keys, chord names, voicing and saved song. Run with java -ea. */
public class ProgressionCheck {
	public static void main(String[] args) throws IOException {
		Progression song = new Progression();
		check(song.keyName().equals("G major") && !song.isMinor(), "opens in G major: " + song.keyName());
		check(Arrays.equals(song.degrees(), new int[] { 1, 4, 5, 1 }), "opens on I-IV-V-I");
		song.setVoicing(Voicing.ROOT);
		int[][] demo = song.voiced();
		check(Arrays.equals(demo[0], new int[] { 67, 71, 74 }) && Arrays.equals(demo[1], new int[] { 72, 76, 79 })
				&& Arrays.equals(demo[2], new int[] { 74, 78, 81 }), "root position is the original demo's chords");
		check(song.matchingPreset() == Preset.ORIGINAL, "the original demo preset");

		String[] gNames = { "G", "Am", "Bm", "C", "D", "Em", "F#dim" };
		String[] majorNumerals = { "I", "ii", "iii", "IV", "V", "vi", "vii°" };
		for (int d = 1; d <= 7; d++) {
			check(song.chordName(d).equals(gNames[d - 1]), "G major chord " + d + ": " + song.chordName(d));
			check(song.numeral(d).equals(majorNumerals[d - 1]), "major numeral " + d + ": " + song.numeral(d));
		}
		check(song.noteNames(7).equals("F# A C"), "vii of G: " + song.noteNames(7));

		song.setKey(11, false);
		check(song.keyName().equals("F major") && song.chordName(4).equals("Bb"), "F major has B flat: " + song.chordName(4));
		check(Progression.signature(11).equals("1 flat") && Progression.signature(1).equals("1 sharp")
				&& Progression.signature(0).equals("no sharps or flats") && Progression.signature(6).equals("6 sharps")
				&& Progression.signature(9).equals("3 flats"), "key signatures");
		check(Progression.keyName(9, false).equals("Eb major") && Progression.keyName(9, true).equals("c minor"),
				"three flats: Eb major, c minor");

		song.setKey(0, true);
		check(song.keyName().equals("A minor"), "C's relative minor: " + song.keyName());
		String[] aNames = { "Am", "Bdim", "C", "Dm", "Em", "F", "G" };
		String[] minorNumerals = { "i", "ii°", "III", "iv", "v", "VI", "VII" };
		for (int d = 1; d <= 7; d++) {
			check(song.chordName(d).equals(aNames[d - 1]), "A minor chord " + d + ": " + song.chordName(d));
			check(song.numeral(d).equals(minorNumerals[d - 1]), "minor numeral " + d + ": " + song.numeral(d));
		}

		// Every key: the circle's neighbors hold its major and minor chords, and the chord qualities follow the mode.
		String[] majorQualities = { "", "m", "m", "", "", "m", "dim" };
		String[] minorQualities = { "m", "dim", "", "m", "m", "", "" };
		for (int p = 0; p < 12; p++) {
			for (int m = 0; m < 2; m++) {
				boolean minor = m == 1;
				song.setKey(p, minor);
				check(song.tonic() == Progression.tonicAt(p, minor), "tonic");
				for (int d = 1; d <= 7; d++) {
					String expected = (minor ? minorQualities : majorQualities)[d - 1];
					check(song.quality(d).equals(expected), song.keyName() + " degree " + d + " is " + song.quality(d));
					int root = Math.floorMod(song.triad(d)[0], 12);
					if (!expected.equals("dim")) {
						boolean found = false;
						for (int q = p - 1; q <= p + 1; q++) {
							found |= Progression.tonicAt(q, expected.equals("m")) == root;
						}
						check(found, song.keyName() + " chord " + d + " sits next to the key on the circle");
					}
				}
			}
		}

		// Smooth voicing: the same notes as the chord, kept in range, moving no more than root position does.
		song.setVoicing(Voicing.SMOOTH);
		for (int p = 0; p < 12; p++) {
			for (int a = 1; a <= 7; a++) {
				for (int b = 1; b <= 7; b++) {
					song.setKey(p, p % 2 == 0);
					song.clear();
					song.add(a);
					song.add(b);
					int[][] chords = song.voiced();
					int[] root = song.triad(b);
					int[] pitches = pitchClasses(chords[1]);
					check(Arrays.equals(pitches, pitchClasses(root)), "smooth voicing keeps the chord's notes");
					check(chords[1][0] >= 52 && chords[1][2] <= 84 && chords[1][0] < chords[1][1] && chords[1][1] < chords[1][2],
							"smooth voicing in range and in order " + Arrays.toString(chords[1]));
					check(chords[1][2] - chords[1][0] < 12, "close position");
					check(chords[0][0] >= 52 && chords[0][2] <= 84, "first chord in range");
					if (root[0] >= 52 && root[2] <= 84) {
						check(move(chords[0], chords[1]) <= move(chords[0], root), "moves no more than root position");
					}
				}
			}
		}
		song.setKey(0, false);
		song.clear();
		song.add(1);
		song.add(4);
		song.add(5);
		int[][] cChords = song.voiced();
		check(Arrays.equals(cChords[1], new int[] { 60, 65, 69 }), "C to F keeps C: " + Arrays.toString(cChords[1]));
		check(Arrays.equals(cChords[2], new int[] { 62, 67, 71 }), "F to G: " + Arrays.toString(cChords[2]));
		check(song.bassOf(4) == 53 && song.bassOf(5) == 55, "bass notes are the roots in the third octave");

		// Editing and presets.
		song.clear();
		for (int i = 0; i < 9; i++) {
			check(song.add(1 + i % 7) == i < Progression.MAX_CHORDS, "eight chords at most");
		}
		check(!song.add(0) && !song.add(8), "degrees 1 to 7 only");
		song.set(0, 6);
		song.remove(7);
		check(song.size() == 7 && song.degree(0) == 6, "set and remove");
		song.setKey(3, false);
		song.apply(Preset.MINOR);
		check(song.isMinor() && song.position() == 3 && song.keyName().equals("F# minor"), "minor preset goes to the relative minor: " + song.keyName());
		song.apply(Preset.CANON);
		check(!song.isMinor() && song.size() == 8 && song.matchingPreset() == Preset.CANON, "canon preset");
		check(Arrays.equals(Progression.usualNext(5), new int[] { 1, 6 }) && Progression.usualNext(0).length == 0, "usual next chords");
		song.setTempo(500);
		check(song.tempo() == Progression.MAX_TEMPO, "tempo capped");
		song.setTempo(10);
		check(song.tempo() == Progression.MIN_TEMPO, "tempo floor");

		// Saving and loading.
		File file = File.createTempFile("songwriter", ".txt");
		file.deleteOnExit();
		song.setKey(10, true);
		song.clear();
		song.add(1);
		song.add(6);
		song.add(7);
		song.setTempo(120);
		song.setStyle(Style.ARPEGGIO);
		song.setVoicing(Voicing.ROOT);
		song.save(file);
		Progression loaded = Progression.load(file);
		check(loaded.position() == 10 && loaded.isMinor() && Arrays.equals(loaded.degrees(), new int[] { 1, 6, 7 })
				&& loaded.tempo() == 120 && loaded.style() == Style.ARPEGGIO && loaded.voicing() == Voicing.ROOT, "saved song loads");
		try (Writer writer = new FileWriter(file)) {
			writer.write("key=nine\nminor=true\nchords=2,x,9,5,1,1,1,1,1,1,1,1\nstyle=JAZZ\ntempo=72\nnonsense\n");
		}
		loaded = Progression.load(file);
		check(loaded.position() == 1 && loaded.isMinor() && loaded.tempo() == 72 && loaded.style() == Style.BLOCK,
				"bad values keep the defaults");
		check(Progression.load(new File(file.getPath() + ".missing")).matchingPreset() == Preset.ORIGINAL, "no file: the demo");
		System.out.println("ProgressionCheck passed");
	}

	private static int[] pitchClasses(int[] chord) {
		int[] pitches = new int[chord.length];
		for (int i = 0; i < chord.length; i++) {
			pitches[i] = Math.floorMod(chord[i], 12);
		}
		Arrays.sort(pitches);
		return pitches;
	}

	private static int move(int[] from, int[] to) {
		int total = 0;
		for (int i = 0; i < from.length; i++) {
			total += Math.abs(from[i] - to[i]);
		}
		return total;
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
