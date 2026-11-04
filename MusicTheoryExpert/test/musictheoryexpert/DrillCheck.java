package musictheoryexpert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Random;

/** Self-check for the ear training drills, rounds and saved records. Run with java -ea. */
public class DrillCheck {
	public static void main(String[] args) throws IOException {
		Random random = new Random(7);
		for (Drill drill : Drill.values()) {
			boolean[] asked = new boolean[drill.choiceCount()];
			for (int i = 0; i < 500; i++) {
				Drill.Question question = drill.ask(random);
				int answer = question.answer();
				check(answer >= 0 && answer < drill.choiceCount(), drill + " answer in range");
				asked[answer] = true;
				int[][] steps = question.steps();
				for (int[] step : steps) {
					for (int note : step) {
						check(note >= 48 && note <= 96, drill + " note " + note + " is on the keyboard");
					}
				}
				checkSound(drill, answer, steps);
			}
			for (int choice = 0; choice < asked.length; choice++) {
				check(asked[choice], drill + " asks " + drill.choice(choice));
			}
		}
		check(EarTrainer.noteNames(new int[][] { { 67 }, { 71 }, { 74 }, { 67, 71, 74 } }).equals("G4 B4 D5"), "note names");
		check(EarTrainer.noteNames(new int[][] { { 66 } }).equals("F#4"), "sharp note name");

		Round round = new Round(Drill.MAJOR_MINOR, new Random(1));
		check(!round.answered() && round.index() == 0, "new round");
		round.next();
		check(round.index() == 0, "no moving on before answering");
		for (int i = 0; i < Round.QUESTIONS; i++) {
			int right = round.question().answer();
			boolean correct = round.answer(i < 3 ? 1 - right : right);
			check(correct == (i >= 3), "answer " + i);
			check(!round.answer(right), "a question is answered once");
			check(round.isOver() == (i == Round.QUESTIONS - 1), "over only after the last question");
			round.next();
		}
		check(round.score() == 7 && round.bestStreak() == 7 && round.streak() == 7, "score and streak");
		check(round.stars() == 1, "7 right is one star");
		check(Round.starsFor(10) == 3 && Round.starsFor(9) == 2 && Round.starsFor(8) == 2 && Round.starsFor(6) == 1
				&& Round.starsFor(5) == 0, "star rules");

		File file = File.createTempFile("drills", ".txt");
		file.deleteOnExit();
		try (Writer writer = new FileWriter(file)) {
			writer.write("NOT_A_DRILL=3,10\nSCALES=x,y\nCHORD_QUALITY=3,11\n");
		}
		DrillRecords records = DrillRecords.load(file);
		check(records.score(Drill.CHORD_QUALITY) == -1 && records.totalStars() == 0, "bad lines skipped");
		check(records.isOpen(Drill.MAJOR_MINOR) && !records.isOpen(Drill.CHORD_QUALITY), "only the first drill is open");
		check(records.suggested() == Drill.MAJOR_MINOR, "suggests the first drill");
		check(records.add(round), "first finished round is kept");
		check(!records.add(round), "the same score is not a new best");
		check(records.isOpen(Drill.CHORD_QUALITY) && !records.isOpen(Drill.BIG_INTERVALS), "a star opens the next drill");
		records = DrillRecords.load(file);
		check(records.score(Drill.MAJOR_MINOR) == 7 && records.stars(Drill.MAJOR_MINOR) == 1, "saved and read back");
		check(records.suggested() == Drill.MAJOR_MINOR, "suggests the drill without three stars");
		System.out.println("DrillCheck passed");
	}

	/** The last step (or, for scales and chords in a key, the whole question) sounds like the right answer. */
	private static void checkSound(Drill drill, int answer, int[][] steps) {
		int[] last = steps[steps.length - 1];
		switch (drill) {
		case MAJOR_MINOR:
		case CHORD_QUALITY:
			check(Arrays.equals(new int[] { last[1] - last[0], last[2] - last[0] }, Drill.TRIADS[answer]), drill + " triad");
			break;
		case BIG_INTERVALS:
			check(last[1] - last[0] == Drill.BIG_INTERVAL_STEPS[answer], drill + " interval");
			break;
		case ALL_INTERVALS:
			check(last[1] - last[0] == answer + 1, drill + " interval");
			break;
		case SCALES:
			int[] pattern = new int[][] { Notes.majorSteps, Notes.naturalMinorSteps, Notes.harmonicMinorSteps, Notes.melodicMinorSteps }[answer];
			check(steps.length == 8, "scale has eight notes");
			for (int i = 0; i < 7; i++) {
				check(steps[i + 1][0] - steps[i][0] == pattern[i], drill + " step " + i);
			}
			break;
		case CHORDS_IN_KEY:
			check(steps.length == 2, "home chord then the question");
			int degree = Drill.DEGREES[answer];
			// The home chord's root is the key; the question's root is that degree of the major scale above it.
			int[] major = { 0, 2, 4, 5, 7, 9, 11 };
			check(last[0] - steps[0][0] == major[degree - 1], drill + " degree " + degree);
			break;
		default:
			throw new AssertionError("unchecked drill " + drill);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
