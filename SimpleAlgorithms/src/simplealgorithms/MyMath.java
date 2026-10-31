package simplealgorithms;

public class MyMath {
	public static double estimateSquareRoot(double num)
	{
		double result = 1;
		double dividend = 0;
		while(result != dividend)
		{
			dividend = num / result;
			result = (dividend + result) / 2;
		}
		return result;
	}
	
	/**
	 * trying to use this <html><iframe src="https://en.wikipedia.org/wiki/Taylor_series#Approximation_and_convergence"></iframe></src> idea
	 * @param num
	 * @param accuracy
	 * @return
	 */
	public static double estimateSine(double num, int accuracy)
	{
		double result = 0;
		for(int i = 0; i < accuracy; i++)
		{
			result += (2 * (i % 2) - 1) * (pow(num, (2 * i) + 1) / factorial((2 * i) + 1));
			result += (multiply(2, i % 2) - 1) * (pow(num, (2 * i) + 1) / factorial(multiply(2, i) + 1));
		}
		return result;
	}
	
	public static double estimateCosine(double num, int accuracy)
	{
		return estimateSine(num - (Math.PI / 2), accuracy);
	}
	
	public static double pow(double num, int pow)
	{
		double result = 1;
		for(int i = 0; i < pow; i++)
		{
			result *= num;
		}
		return result;
	}
	
	public static int factorial(int num)
	{
		int result = 1;
		for(int i = num; i > 1; i--)
		{
//			result *= i;
			result = multiply(result, i);
		}
		return result;
	}
	public static double add(double num1, double num2) {
		return num1 + num2;
	}
	public static double subtract(double num1, double num2) {
//		return add(num1, multiply(-1, num2));
		return num1 - num2;
	}
	public static int multiply(int num1, int num2) {
		int result = 0;
		for(int i = 0; i < num2; i++) {
			result += num1;
		}
		return result;
	}
	//	a/b=c; a=bc;
	public static double divide(double num1, double num2) {
		double guess = 0;
		double error = 0.01;
		double tolerance = 1;
		double currentGuess = 5;
		double counter2 = 90;
		int counter = 0;
		while(guess * num2 != num1) {
			if(guess * num2 > num1) {
				guess -= tolerance;
				tolerance -= counter2 * error;
				counter2 /= 10;
				if(currentGuess - guess < error) break;
			}
			else {
				guess += tolerance;
			}
			System.out.println(counter + ": " + guess);
//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			currentGuess = guess;
			counter++;
		}
		return guess;
	}
}
