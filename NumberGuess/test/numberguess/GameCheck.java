package numberguess;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Random;

/** Self-check for the ranges, games, You pick, the Magic cards and saved records. Run with java -ea. */
public class GameCheck {
	public static void main(String[] args) throws IOException {
		int[] pars = { 4, 7, 10, 14, 20 };
		for (Level level : Level.values()) {
			check(level.par() == pars[level.ordinal()], level + " par");
			check(level.starsFor(level.par()) == 3 && level.starsFor(level.par() + 2) == 2
					&& level.starsFor(level.limit()) == 1 && level.starsFor(level.limit() + 1) == 0, level + " stars");
			// Guessing the middle always finds the number within par.
			Random random = new Random(level.ordinal());
			for (int i = 0; i < 2000; i++) {
				Game game = new Game(level, random);
				check(game.number() >= 1 && game.number() <= level.max(), "number in range");
				while (!game.isOver()) {
					game.guess((game.low() + game.high()) / 2);
				}
				check(game.isFound() && game.getGuesses() <= level.par() && game.stars() == 3, level + " halving wins at par");
			}
			for (int number : new int[] { 1, level.max() }) {
				Game game = new Game(level, number);
				while (!game.isOver()) {
					game.guess((game.low() + game.high()) / 2);
				}
				check(game.isFound() && game.getGuesses() <= level.par(), level + " finds " + number);
			}
		}
		check(Level.commas(1000000).equals("1,000,000") && Level.commas(100).equals("100") && Level.commas(1000).equals("1,000"), "commas");
		check(Level.THOUSAND.label().equals("1-1,000"), "label");

		Game game = new Game(Level.HUNDRED, 40);
		check(game.guess(0) == Game.Verdict.OUT_OF_RANGE && game.guess(101) == Game.Verdict.OUT_OF_RANGE, "out of range");
		check(game.getGuesses() == 0, "out of range not counted");
		check(game.guess(50) == Game.Verdict.TOO_HIGH && game.high() == 49 && game.low() == 1, "too high narrows");
		check(game.guess(20) == Game.Verdict.TOO_LOW && game.low() == 21, "too low narrows");
		check(game.guess(60) == Game.Verdict.RULED_OUT && game.guess(20) == Game.Verdict.RULED_OUT, "ruled out");
		check(game.getGuesses() == 2 && game.guessesLeft() == Level.HUNDRED.limit() - 2, "ruled out not counted");
		check(game.guess(40) == Game.Verdict.CORRECT && game.isOver() && game.stars() == 3, "correct");
		check(game.guess(41) == Game.Verdict.OVER, "over");

		// Running out of guesses gives the number away.
		game = new Game(Level.TEN, 10);
		for (int i = 1; i <= Level.TEN.limit(); i++) {
			check(game.guess(i) == Game.Verdict.TOO_LOW, "walking up");
		}
		check(game.isOver() && !game.isFound() && game.stars() == 0, "out of guesses");

		// You pick: the computer finds every number within par, and catches answers that contradict.
		for (int number = 1; number <= MindReader.MAX; number++) {
			MindReader reader = new MindReader();
			while (!reader.isOver()) {
				if (reader.guess() < number) {
					reader.higher();
				} else if (reader.guess() > number) {
					reader.lower();
				} else {
					reader.correct();
				}
			}
			check(reader.isFound() && reader.guess() == number && reader.guesses().size() <= MindReader.par(), "reads " + number);
		}
		MindReader reader = new MindReader();
		check(reader.guess() == 50, "first guess");
		reader.higher();
		check(reader.low() == 51 && reader.guess() == 75, "higher");
		while (!reader.isOver()) {
			reader.lower();
		}
		check(reader.isCaught() && reader.low() == 51 && reader.high() == 50, "caught");
		int asked = reader.guesses().size();
		reader.higher();
		check(reader.guesses().size() == asked, "no more guesses once caught");
		reader = new MindReader();
		while (!reader.isOver()) {
			reader.higher();
		}
		check(reader.isCaught() && reader.low() == MindReader.MAX + 1, "higher than 100");

		// Magic: six cards of 32, each card's smallest number is its key, and honest answers add up to the number.
		Random shuffle = new Random(7);
		for (int number = 0; number <= MagicCards.MAX; number++) {
			MagicCards cards = new MagicCards(shuffle);
			int keys = 0;
			for (int i = 0; i < MagicCards.CARDS; i++) {
				check(cards.numbers(i).size() == 32 && java.util.Collections.min(cards.numbers(i)) == cards.key(i), "card " + i);
				keys |= cards.key(i);
				check(cards.current() == i && cards.answer(i) == null, "asks in order");
				cards.answer(cards.numbers(i).contains(number));
			}
			check(keys == MagicCards.MAX, "one card per key");
			check(cards.isDone() && cards.number() == number, "magic reads " + number);
			cards.answer(true);
			check(cards.number() == number, "no answers after the last card");
		}

		File file = File.createTempFile("numberguess", ".txt");
		file.deleteOnExit();
		try (Writer writer = new FileWriter(file)) {
			writer.write("TEN=3,3\nHUNDRED=1,99\nBOGUS=3,1\nTHOUSAND=x\nMILLION=2,0\n");
		}
		Records records = Records.load(file);
		check(records.best(Level.TEN) == 3 && records.stars(Level.TEN) == 3, "reads a record");
		check(records.best(Level.HUNDRED) == 0 && records.best(Level.MILLION) == 0, "skips bad lines");
		check(records.isOpen(Level.TEN) && records.isOpen(Level.HUNDRED) && !records.isOpen(Level.THOUSAND), "open ranges");
		check(records.suggested() == Level.HUNDRED, "suggested");
		Game slow = new Game(Level.HUNDRED, 1);
		for (int i = 100; i >= 1 && !slow.isOver(); i -= 10) {
			slow.guess(i);
		}
		check(!slow.isFound() || slow.stars() < Records.STARS_TO_OPEN, "slow game");
		Game won = new Game(Level.HUNDRED, 30);
		won.guess(50);
		won.guess(25);
		won.guess(37);
		won.guess(31);
		won.guess(30);
		check(records.add(won) && records.best(Level.HUNDRED) == 5 && records.isOpen(Level.THOUSAND), "adds a win and opens the next range");
		Game worse = new Game(Level.HUNDRED, 30);
		for (int i = 21; i <= 30; i++) {
			worse.guess(i);
		}
		check(worse.isFound() && !records.add(worse) && records.best(Level.HUNDRED) == 5, "keeps the best");
		check(!records.add(slow), "a lost game is not a record");
		Records again = Records.load(file);
		check(again.best(Level.TEN) == 3 && again.best(Level.HUNDRED) == 5 && again.totalStars() == 6, "saved and read back");

		checkDuels(file);
		System.out.println("GameCheck: all checks passed");
	}

	/** Duel: turns, the same-round rule, the computer's guesses, its odds against halving, and the saved tally. */
	private static void checkDuels(File file) throws IOException {
		Random random = new Random(7);
		Duel duel = new Duel(random, 40);
		check(duel.isHiding() && !duel.isYourTurn() && duel.guess(50) == Game.Verdict.OVER, "hide first");
		check(!duel.hide(0) && !duel.hide(101) && duel.hide(70) && !duel.hide(5) && duel.hidden() == 70, "hide once, in range");
		check(duel.isYourTurn() && duel.round() == 1, "you start");
		check(duel.guess(101) == Game.Verdict.OUT_OF_RANGE && duel.isYourTurn(), "out of range keeps the turn");
		check(duel.guess(50) == Game.Verdict.TOO_HIGH && duel.isComputersTurn() && duel.guess(30) == Game.Verdict.OVER, "turns");
		int guess = duel.computerGuess();
		check(guess >= 26 && guess <= 75 && duel.isYourTurn() && duel.round() == 2, "the computer guesses the middle half");
		check(duel.guess(60) == Game.Verdict.RULED_OUT && duel.isYourTurn(), "ruled out keeps the turn");
		check(duel.guess(40) == Game.Verdict.CORRECT && !duel.isOver() && duel.isComputersTurn(), "it answers in the same round");
		duel.computerGuess();
		check(duel.isOver() && duel.result() == (duel.its().isFound() ? Duel.Result.DRAWN : Duel.Result.WON), "won or drawn");
		check(duel.computerGuess() == 0 && duel.guess(40) == Game.Verdict.OVER, "nothing after the end");

		// Played out: the computer always finds a number, and someone who halves wins more often than it does.
		int[] results = new int[4];
		for (int i = 0; i < 20000; i++) {
			duel = new Duel(random);
			duel.hide(random.nextInt(Duel.MAX) + 1);
			while (!duel.isOver()) {
				Game yours = duel.yours();
				duel.guess((yours.low() + yours.high()) / 2);
				int its = duel.computerGuess();
				check(its >= 1 && its <= Duel.MAX, "a guess in range");
			}
			check(duel.yours().getGuesses() <= Level.HUNDRED.par(), "halving finds it within par");
			results[duel.result().ordinal()]++;
		}
		check(results[Duel.Result.WON.ordinal()] > results[Duel.Result.LOST.ordinal()] && results[Duel.Result.LOST.ordinal()] > 4000,
				"halving beats the computer, but not always");
		// Only a computer left to guess: it finds every number.
		for (int number = 1; number <= Duel.MAX; number++) {
			duel = new Duel(random, 1);
			duel.hide(number);
			for (int i = 100; i > 1 && !duel.isOver(); i--) {
				duel.guess(i);
				duel.computerGuess();
			}
			check(duel.isOver() && duel.its().isFound(), "the computer finds " + number);
		}

		Records records = Records.load(file);
		records.add(Duel.Result.WON);
		records.add(Duel.Result.WON);
		records.add(Duel.Result.LOST);
		records.add(Duel.Result.NONE);
		Records back = Records.load(file);
		check(back.duels(Duel.Result.WON) == 2 && back.duels(Duel.Result.LOST) == 1 && back.duels(Duel.Result.DRAWN) == 0
				&& back.best(Level.HUNDRED) == 5, "duels saved beside the ranges");
		try (Writer writer = new FileWriter(file)) {
			writer.write("duel=x,1,2\nTEN=3,3\n");
		}
		back = Records.load(file);
		check(back.duels(Duel.Result.LOST) == 0 && back.best(Level.TEN) == 3, "a bad duel line is skipped");
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
