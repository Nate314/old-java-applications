package simplealgorithms;
/**
 * 
 * 
 * <html>
 * <a href="https://www.youtube.com/watch?v=2SUvWfNJSsM&t=747s">this</a>
 * is the idea that I am using for the solve() algorithm
 * </html>
 * @author Nathan Gawith
 *
 */
public class Hanoi {
	// @formatter:off
	//the code below is used for a base of not 10 later in the code.
	/*
	private int log(int x, int base) {
		return (int) (Math.log(x) / Math.log(base));
	}
	*/
	// @formatter:on

	/**
	 * this function will print out instructions on how to solve a Hanoi puzzle
	 * of any size.
	 * 
	 * @param numberOfDisks
	 *            number of disks in the Hanoi puzzle you want to solve.
	 * @param a should be set to <html><code>true</code></html>.
	 */
	public static void solve(int numberOfDisks, boolean a) {
		for (int i = 1; i < Math.pow(2, numberOfDisks); i++) {
			a = true;
			for (int j = (int) Math.pow(2, numberOfDisks - 1); j >= 2; j = j / 2) {
				if (a)
					if (i % j == 0) {
						System.out.println("Move " + ((int) (Math.log(j) / Math.log(2))) + " "
								+ ((((int) (Math.log(j) / Math.log(2))) % 2 == 0)
										? ((numberOfDisks % 2 == 0) ? "right" : "left")
										: ((numberOfDisks % 2 == 0) ? "left" : "right")));
						a = false;
					}
			}
			if (a)
				System.out.println("Move 0 right");
		}
	}
}

// below is the basic idea that I used to make the more general form above which
// works for any number of disks.

// @formatter:off
//public void solve1() {
//	for (int i = 1; i < Math.pow(2, numberOfDisks); i++) {
//		System.out.println(Integer.toBinaryString(i));
//		if (i % 8 == 0) {
//			System.out.println("Move 3 left");
//		} else if (i % 4 == 0) {
//			System.out.println("Move 2 right");
//		} else if (i % 2 == 0) {
//			System.out.println("Move 1 left");
//		} else {
//			System.out.println("Move 0 right");
//		}
//	}
//}
// @formatter:on
