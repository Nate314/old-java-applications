package simplealgorithms;

import java.util.List;
import java.util.Random;

/** Self-check for the Division page's reading of MyMath.divide's steps. Run with java -ea. */
public class DivideCheck {
	public static void main(String[] args) {
		check(kinds(4, 2).equals("UP DONE"), "4/2 counts 1, 2 and hits 4 exactly: " + kinds(4, 2));
		check(kinds(22, 7).equals("UP UP UP OVER BACK UP OVER BACK UP UP UP UP OVER DONE"), "22/7: " + kinds(22, 7));
		List<DividePage.Step> sevenHalves = DividePage.steps(7, 2);
		check(sevenHalves.get(9).kind == DividePage.Kind.OVER && sevenHalves.get(9).guess * 2 > 7
				&& Math.abs(sevenHalves.get(9).guess - 3.5) < 1e-12, "7/2 drifts past 3.5 when adding tenths");
		Random random = new Random(138);
		for (int i = 0; i < 2000; i++) {
			double a = 1 + random.nextInt(500) / (random.nextBoolean() ? 1.0 : 10.0);
			double b = 1 + random.nextInt(60) / (random.nextBoolean() ? 1.0 : 4.0);
			if (a / b > 100) {
				continue;
			}
			checkSteps(a, b);
		}
		System.out.println("DivideCheck passed");
	}

	private static String kinds(double a, double b) {
		StringBuilder kinds = new StringBuilder();
		for (DividePage.Step step : DividePage.steps(a, b)) {
			kinds.append(kinds.length() == 0 ? "" : " ").append(step.kind);
		}
		return kinds.toString();
	}

	/** Every step is one hop of its level's size, a step back drops a level, and the end is divide's own answer. */
	private static void checkSteps(double a, double b) {
		String name = a + "/" + b;
		List<DividePage.Step> steps = DividePage.steps(a, b);
		DividePage.Step last = steps.get(steps.size() - 1);
		check(last.kind == DividePage.Kind.DONE && last.guess == MyMath.divide(a, b, line -> { }), name + " ends on what divide returns");
		check(last.guess <= a / b + 1e-9 && last.guess > a / b - 0.01 - 1e-9, name + " is a / b to two places or better");
		double before = 0;
		int level = 0;
		for (DividePage.Step step : steps) {
			check(step.level == level && level <= 3, name + " level " + step.level);
			double size = Math.pow(10, -level);
			check(Math.abs(Math.abs(step.guess - before) - size) < 1e-9, name + " hops " + size + " to " + step.guess);
			check((step.kind == DividePage.Kind.OVER) == (step.guess > before && step.guess * b > a), name + " over at " + step.guess);
			if (step.kind == DividePage.Kind.BACK) {
				check(step.line != null && step.guess < before, name + " steps back");
				level++;
			}
			before = step.guess;
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
