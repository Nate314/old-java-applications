package musictheoryexpert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import musictheoryexpert.LeadSheet.Mark;

/** Self-check for the Songwriter's sheet music: note values, rests, ties and accidentals. Run with java -ea. */
public class LeadSheetCheck {
	public static void main(String[] args) {
		// No melody: a whole rest a bar.
		check(written(LeadSheet.marks(notes(), 2)).equals("r8 r8"), "empty bars are whole rests");
		// A quarter, then the rest of the bar as a dotted half rest.
		check(written(LeadSheet.marks(notes(0, 67, 2), 1)).equals("67:2 r6"), "a quarter and a dotted half rest");
		// Five eighths have no single note: a half tied to an eighth.
		check(written(LeadSheet.marks(notes(0, 67, 5), 1)).equals("67:4~ 67:1 r3"), "five eighths are tied");
		// A note over the bar line is tied into the next bar.
		check(written(LeadSheet.marks(notes(6, 72, 4), 2)).equals("r6 72:2~ 72:2 r6"), "tied over the bar line");
		// A note still held when the next starts is written up to it; notes that start together are a chord.
		check(written(LeadSheet.marks(notes(0, 64, 4, 2, 67, 2, 2, 72, 2), 1)).equals("64:2 67,72:2 r4"),
				"cut at the next note, chords share a stem");
		// Notes past the last bar are not written.
		check(written(LeadSheet.marks(notes(9, 60, 1), 1)).equals("r8"), "notes past the end are left out");

		// Random melodies: the marks fill every bar exactly, in written lengths, never over a bar line.
		Random random = new Random(7);
		for (int trial = 0; trial < 500; trial++) {
			int bars = 1 + random.nextInt(Progression.MAX_CHORDS);
			List<int[]> melody = new ArrayList<>();
			for (int n = random.nextInt(20); n > 0; n--) {
				melody.add(new int[] { random.nextInt(bars * 8 + 4), 55 + random.nextInt(30), 1 + random.nextInt(8) });
			}
			int at = 0;
			for (Mark mark : LeadSheet.marks(melody, bars)) {
				check(mark.slot == at, "marks follow on");
				check(Arrays.stream(LeadSheet.VALUES).anyMatch(v -> v == mark.length), "a written length");
				check(mark.slot / 8 == (mark.slot + mark.length - 1) / 8, "inside one bar");
				check(!mark.tied || !mark.isRest(), "only notes are tied");
				at += mark.length;
			}
			check(at == bars * 8, "every bar filled");
		}

		// Spelling: steps from C4 (28), sharps or flats by the key.
		check(Arrays.equals(LeadSheet.spell(60, 0), new int[] { 28, 0 }), "C4");
		check(Arrays.equals(LeadSheet.spell(77, 0), new int[] { 38, 0 }), "F5 is the top line");
		check(Arrays.equals(LeadSheet.spell(70, 11), new int[] { 34, -1 }), "Bb4 in F major");
		check(Arrays.equals(LeadSheet.spell(66, 1), new int[] { 31, 1 }), "F#4 in G major");
		check(Arrays.equals(LeadSheet.signature(1), new int[] { 0, 0, 0, 1, 0, 0, 0 }), "G major: F sharp");
		check(Arrays.equals(LeadSheet.signature(11), new int[] { 0, 0, 0, 0, 0, 0, -1 }), "F major: B flat");
		check(Arrays.equals(LeadSheet.signature(9), new int[] { 0, 0, -1, 0, 0, -1, -1 }), "Eb major: B, E, A flat");
		check(Arrays.equals(LeadSheet.signature(4), new int[] { 1, 1, 0, 1, 1, 0, 0 }), "E major: F, C, G, D sharp");

		// Accidentals in G major: F natural needs a sign, F# none; a sign lasts the bar, not the next one.
		check(signs(notes(0, 65, 1, 1, 65, 1, 2, 66, 1, 8, 65, 1), 2, 1).equals("0 . 1 r r 0 r r"),
				"natural, kept in the bar, sharp again, natural again next bar");
		check(signs(notes(0, 66, 1), 1, 1).startsWith("."), "F# in G major needs no sign");
		// The other octave is another line of the staff.
		check(signs(notes(0, 65, 1, 1, 77, 1), 1, 1).startsWith("0 0"), "each octave its own sign");
		// A tied note carries its sign over the bar line.
		check(signs(notes(6, 65, 4), 2, 1).equals("r 0 . r"), "tied over: no second sign");
		// C major has no signs but its sharps.
		check(signs(notes(0, 61, 1, 1, 60, 1), 1, 0).startsWith("1 0"), "C# then C natural");
		System.out.println("LeadSheetCheck: all checks passed.");
	}

	/** Notes from triples of slot, pitch, length. */
	private static List<int[]> notes(int... values) {
		List<int[]> notes = new ArrayList<>();
		for (int i = 0; i < values.length; i += 3) {
			notes.add(new int[] { values[i], values[i + 1], values[i + 2] });
		}
		return notes;
	}

	/** For example "67:4~ 67:1 r2": pitches and length, ~ for a tie, r for a rest. */
	private static String written(List<Mark> marks) {
		StringBuilder text = new StringBuilder();
		for (Mark mark : marks) {
			StringBuilder pitches = new StringBuilder();
			for (int p : mark.pitches) {
				pitches.append(pitches.length() == 0 ? "" : ",").append(p);
			}
			text.append(text.length() == 0 ? "" : " ").append(mark.isRest() ? "r" : pitches + ":").append(mark.length)
					.append(mark.tied ? "~" : "");
		}
		return text.toString();
	}

	/** The signs before each mark's pitches: "." for none, the sign's number otherwise, "r" for a rest. */
	private static String signs(List<int[]> melody, int bars, int position) {
		List<Mark> marks = LeadSheet.marks(melody, bars);
		List<int[]> signs = LeadSheet.accidentals(marks, position);
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < marks.size(); i++) {
			text.append(text.length() == 0 ? "" : " ");
			if (marks.get(i).isRest()) {
				text.append("r");
			}
			for (int sign : signs.get(i)) {
				text.append(sign == LeadSheet.NONE ? "." : String.valueOf(sign));
			}
		}
		return text.toString();
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
