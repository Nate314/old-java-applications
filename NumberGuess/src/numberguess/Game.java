package numberguess;

import java.util.Random;

public class Game {
	
	private int number;
	
	public Game() {
		number = new Random().nextInt(100);
	}
	
	public String guess(int guess) {
		String result ="";
		if(guess < number) {
			result = "too low";
		}
		else if(guess > number) {
			result = "too high";
		}
		else {
			result = "correct";
		}
		return result;
	}
}
