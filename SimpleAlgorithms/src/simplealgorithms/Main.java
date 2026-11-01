package simplealgorithms;

public class Main {
	public static void main(String[] args)
	{
//		new Hanoi().solve(5, true);
		System.out.println("sqrt(42) ~= " + MyMath.estimateSquareRoot(42));
		System.out.println("sin(pi/4) ~= " + MyMath.estimateSine(Math.PI / 4, 4));
		System.out.println("cos(pi/4) ~= " + MyMath.estimateCosine(Math.PI / 4, 4));
		System.out.println("5! = " + MyMath.factorial(5));
		System.out.println("5^3 = " + MyMath.pow(5, 3));
		System.out.println("4/2 = " + MyMath.divide(4, 2));
	}
}
