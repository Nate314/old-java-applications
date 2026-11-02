package simplealgorithms;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
	public static void main(String[] args)
	{
//		new Hanoi().solve(5, true);
		for (String line : printout()) {
			System.out.println(line);
		}
		SwingUtilities.invokeLater(Main::openLab);
	}

	/** The original program's output, in order: the division search prints each step before its result. */
	static List<String> printout() {
		List<String> lines = new ArrayList<>();
		lines.add("sqrt(42) ~= " + MyMath.estimateSquareRoot(42));
		lines.add("sin(pi/4) ~= " + MyMath.estimateSine(Math.PI / 4, 4));
		lines.add("cos(pi/4) ~= " + MyMath.estimateCosine(Math.PI / 4, 4));
		lines.add("5! = " + MyMath.factorial(5));
		lines.add("5^3 = " + MyMath.pow(5, 3));
		double quotient = MyMath.divide(4, 2, lines::add);
		lines.add("4/2 = " + quotient);
		return lines;
	}

	/** The Algorithm Lab window: the algorithms above, and the Tower of Hanoi, drawn step by step. */
	private static void openLab() {
		JFrame frame = new JFrame("Simple Algorithms: Algorithm Lab");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		LabPanel lab = new LabPanel(new HanoiPage(), new RootPage(), new TaylorPage(), new PrintoutPage());
		frame.add(lab);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		lab.requestFocusInWindow();
	}
}
