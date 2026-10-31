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
