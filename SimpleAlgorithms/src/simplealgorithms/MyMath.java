package simplealgorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MyMath {
	public static double estimateSquareRoot(double num)
	{
		List<Double> steps = squareRootSteps(num);
		return steps.get(steps.size() - 1);
	}

	/**
	 * Every guess estimateSquareRoot makes, starting from 1: each next guess is the average of the guess and num
	 * divided by it. Stops when the two agree, or after 100 guesses (a guess can bounce between two neighbouring
	 * doubles, and 0 or a negative number never settles).
	 */
	public static List<Double> squareRootSteps(double num)
	{
		List<Double> steps = new ArrayList<>();
		double result = 1;
		double dividend = 0;
		steps.add(result);
		while(result != dividend && steps.size() < 100)
		{
			dividend = num / result;
			result = (dividend + result) / 2;
			steps.add(result);
		}
		return steps;
	}
	
	/**
	 * trying to use this <html><iframe src="https://en.wikipedia.org/wiki/Taylor_series#Approximation_and_convergence"></iframe></src> idea
	 * @param num
	 * @param accuracy
	 * @return
	 */
	public static double estimateSine(double num, int accuracy)
	{
		return taylorSeries(num, accuracy, 1);
	}
	
	public static double estimateCosine(double num, int accuracy)
	{
		return taylorSeries(num, accuracy, 0);
	}
	
	/** Sum of (-1)^i x^(2i+firstPower) / (2i+firstPower)!: sine for firstPower 1, cosine for 0. */
	private static double taylorSeries(double num, int accuracy, int firstPower)
	{
		double result = 0;
		for(int i = 0; i < accuracy; i++)
		{
			int power = multiply(2, i) + firstPower;
			result += (i % 2 == 0 ? 1 : -1) * (pow(num, power) / factorial(power));
		}
		return result;
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
	
	/** Exact up to 20!; larger values overflow a long. */
	public static long factorial(int num)
	{
		long result = 1;
		for(int i = num; i > 1; i--)
		{
			result *= i;
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
		return divide(num1, num2, System.out::println);
	}

	/** divide, handing each step of the search ("0: 1.0") to steps instead of printing it. */
	public static double divide(double num1, double num2, Consumer<String> steps) {
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
			steps.accept(counter + ": " + guess);
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
