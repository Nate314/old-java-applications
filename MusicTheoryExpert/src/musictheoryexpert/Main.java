package musictheoryexpert;

import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

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
	private EarTrainer trainer;
	private JPanel notesPanel = new JPanel();
	private JButton button = new JButton("test");

	public Main(Boolean visibility) {
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("Music Theory Expert");
		trainer = new EarTrainer(synth,
				DrillRecords.load(new File(System.getProperty("user.home"), ".musictheory-drills.txt")));
		tpane.addTab("Ear training", trainer);
		tpane.addTab("Notes", notesPanel);
		notesPanel.add(button);
		button.addActionListener(this);
		this.add(tpane);
		this.setSize(780, 600);
		// Keys reach the trainer whichever component has the focus, while its tab is showing.
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(event -> event.getID() == KeyEvent.KEY_PRESSED
				&& tpane.getSelectedComponent() == trainer && this.isActive() && trainer.keyPressed(event));
		this.setVisible(visibility);
//		synth.playNotes(synthesizer.Notes.A4, 300, 0, 127, 0, 0);
//		synth.playNotes(synthesizer.Notes.B4, 300, 0, 127, 0, 0);
//		synth.playNotes(synthesizer.Notes.C4, 300, 0, 127, 0, 0);
		// The I-IV-V-I demo used to play on start; it now plays from the Notes tab's test button.
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
