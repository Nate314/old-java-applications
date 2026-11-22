package simplealgorithms;

/** Self-check for the math estimates. Run with java -ea. */
public class MyMathCheck {
	public static void main(String[] args) {
		for (double x = -Math.PI; x <= Math.PI; x += 0.1) {
			check(Math.abs(MyMath.estimateSine(x, 10) - Math.sin(x)) < 1e-7, "sin(" + x + ")");
			check(Math.abs(MyMath.estimateCosine(x, 10) - Math.cos(x)) < 1e-7, "cos(" + x + ")");
		}
		check(Math.abs(MyMath.estimateSine(Math.PI / 4, 4) - Math.sqrt(0.5)) < 1e-6, "sin(pi/4) with 4 terms");
		check(Math.abs(MyMath.estimateCosine(Math.PI / 4, 4) - Math.sqrt(0.5)) < 1e-4, "cos(pi/4) with 4 terms");
		check(Math.abs(MyMath.estimateSquareRoot(42) - Math.sqrt(42)) < 1e-12, "sqrt(42)");
		check(MyMath.squareRootSteps(0).size() == 100, "sqrt(0) stops after 100 guesses");
		java.util.List<Double> steps = MyMath.squareRootSteps(2);
		check(steps.get(0) == 1 && steps.get(1) == 1.5 && Math.abs(steps.get(steps.size() - 1) - Math.sqrt(2)) < 1e-15, "sqrt(2) steps");
		// From any first guess above 0 the averaging settles on the root (the lab lets the visitor pick it; a guess may
		// then bounce between two neighbouring doubles until the 100-guess stop).
		for (double n : new double[] { 2, 42, 0.5, 1000, 123456789 }) {
			for (double start : new double[] { 1e-3, 0.5, 3, 77, 1e6 }) {
				java.util.List<Double> from = MyMath.squareRootSteps(n, start);
				check(from.get(0) == start && Math.abs(from.get(from.size() - 1) - Math.sqrt(n)) < 1e-12 * Math.sqrt(n),
						"sqrt(" + n + ") from " + start);
			}
		}
		check(MyMath.squareRootSteps(42, 1).equals(MyMath.squareRootSteps(42)), "the start of 1 is the author's loop");
		check(MyMath.factorial(5) == 120, "5!");
		check(MyMath.factorial(20) == 2432902008176640000L, "20!");
		check(MyMath.pow(5, 3) == 125, "5^3");
		check(MyMath.divide(4, 2) == 2, "4/2");
		System.out.println("MyMathCheck passed");
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
