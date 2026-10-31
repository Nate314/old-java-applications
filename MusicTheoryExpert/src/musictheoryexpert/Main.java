package musictheoryexpert;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import synthesizer.MySynthesizer;

@SuppressWarnings("serial")
public class Main extends JFrame implements ActionListener{

	public static final long CLOCK_TICK = 500;
	private MySynthesizer synth = new MySynthesizer(this);
	private JTabbedPane tpane = new JTabbedPane();
	private JPanel notesPanel = new JPanel();
	private JButton button = new JButton("test");

	public Main(Boolean visibility) {
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("Music Theory Expert");
		tpane.addTab("Notes", notesPanel);
		notesPanel.add(button);
		button.addActionListener(this);
		this.add(tpane);
		this.setSize(500, 200);
		this.setVisible(visibility);
//		synth.playNotes(synthesizer.Notes.A4, 300, 0, 127, 0, 0);
//		synth.playNotes(synthesizer.Notes.B4, 300, 0, 127, 0, 0);
//		synth.playNotes(synthesizer.Notes.C4, 300, 0, 127, 0, 0);
		new Notes(synth);
	}
	
	// Set before any component is created, so the window really uses the system look.
	private static void setLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			setLookAndFeel();
			new Main(true);
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == button) {
			new Notes(synth);
		}
	}
}
