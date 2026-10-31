package numberguess;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class Main extends JFrame implements ActionListener {

	private Game game;
	private JPanel panel = new JPanel();
	private JButton button = new JButton("SUBMIT");
	private JLabel label = new JLabel("Guess a number btwn 1 and 100");
	private JTextField textField = new JTextField();

	public Main() {
		game = new Game();
		this.setVisible(true);
		this.setSize(300, 200);
		this.setTitle("Number Guess");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.add(label);
		panel.add(textField);
		panel.add(button);
		this.add(panel);
		textField.setColumns(10);
		button.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource() == button) {
			label.setText(game.guess(Integer.parseInt(textField.getText())));
		}
	}

	public static void main(String args[]) {
		new Main();
	}
}
