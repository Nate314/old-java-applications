package numberguess;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class Main extends JFrame implements ActionListener {

	private static final String PROMPT = "Guess a number btwn 1 and 100";

	private Game game;
	private JPanel panel = new JPanel();
	private JButton button = new JButton("SUBMIT");
	private JLabel label = new JLabel(PROMPT);
	private JTextField textField = new JTextField();

	public Main() {
		game = new Game();
		this.setSize(300, 200);
		this.setTitle("Number Guess");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.add(label);
		panel.add(textField);
		panel.add(button);
		this.add(panel);
		textField.setColumns(10);
		button.addActionListener(this);
		// Enter in the text field submits too.
		textField.addActionListener(this);
		this.setVisible(true);
		textField.requestFocus();
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		int guess;
		try {
			guess = Integer.parseInt(textField.getText().trim());
		} catch (NumberFormatException e) {
			label.setText("type a whole number from 1 to 100");
			return;
		}
		String result = game.guess(guess);
		int guesses = game.getGuesses();
		if (result.equals("correct")) {
			label.setText("correct in " + guesses + (guesses == 1 ? " guess" : " guesses") + "! New number picked.");
			game = new Game();
		} else {
			label.setText(guess + " is " + result + " (guess " + guesses + ")");
		}
		textField.setText("");
		textField.requestFocus();
	}

	public static void main(String args[]) {
		SwingUtilities.invokeLater(Main::new);
	}
}
