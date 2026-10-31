package musictheoryexpert;

import java.util.Arrays;

import musictheoryexpert.Notes.MusicalTerms;

/** Self-check for note names, scales and triads. Run with java -ea. */
public class NotesCheck {
	public static void main(String[] args) {
		for (int i = 0; i < 12; i++) {
			check(Notes.getNoteID(Notes.notesSharpBias[i]) == i, "sharp name " + Notes.notesSharpBias[i]);
			check(Notes.getNoteID(Notes.notesFlatBias[i]) == i, "flat name " + Notes.notesFlatBias[i]);
		}
		check(Notes.getNoteID(new StringBuilder("B").append("f").toString()) == 10, "name built at run time");
		check(Notes.getNoteID("H") == 400, "unknown name");

		check(Notes.getNoteName(-12, MusicalTerms.SHARP).equals("C"), "-12 is a C");
		check(Notes.getNoteName(-1, MusicalTerms.SHARP).equals("B"), "-1 is a B");
		check(Notes.getNoteName(70, MusicalTerms.FLAT).equals("Bf"), "70 is B flat");
		check(Notes.moveToOctave(67, 5) == 79, "G5");

		int[] gMajor = Notes.getScale("G", MusicalTerms.MAJOR);
		check(Arrays.equals(gMajor, new int[] { 67, 69, 71, 72, 74, 76, 78 }), "G major " + Arrays.toString(gMajor));
		check(Arrays.equals(Notes.getScale("Bf", MusicalTerms.MAJOR), new int[] { 70, 72, 74, 75, 77, 79, 81 }), "B flat major");
		check(Arrays.equals(Notes.getScale("A", MusicalTerms.NATURAL_MINOR), new int[] { 69, 71, 72, 74, 76, 77, 79 }), "A natural minor");
		check(Arrays.equals(Notes.getScale("A", MusicalTerms.HARMONIC_MINOR), new int[] { 69, 71, 72, 74, 76, 77, 80 }), "A harmonic minor");
		check(Arrays.equals(Notes.getScale("A", MusicalTerms.MELODIC_MINOR), new int[] { 69, 71, 72, 74, 76, 78, 80 }), "A melodic minor");

		check(Arrays.equals(Notes.getTriadByRomanNumeral(gMajor, 1), new int[] { 67, 71, 74 }), "I of G");
		check(Arrays.equals(Notes.getTriadByRomanNumeral(gMajor, 4), new int[] { 72, 76, 79 }), "IV of G");
		check(Arrays.equals(Notes.getTriadByRomanNumeral(gMajor, 5), new int[] { 74, 78, 81 }), "V of G");
		System.out.println("NotesCheck passed");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
