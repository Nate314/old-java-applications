package synthesizer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class KeyboardWindow extends JFrame implements ActionListener, KeyListener {
   private MidiPlayer midiPlayer = new MidiPlayer(this);
   private NoteNameConverter noteConverter = new NoteNameConverter();
   private JLayeredPane layeredPane = new JLayeredPane();
   private JPanel whiteKeyPanel = new JPanel();
   private JPanel blackKeyPanel = new JPanel();
   private JButton[] whiteKeys = new KeyButton[8];
   private JButton[] blackKeys = new KeyButton[5];
   private int[] whiteKeyNotes = new int[]{60, 62, 64, 65, 67, 69, 71, 72};
   private int[] blackKeyNotes = new int[]{61, 63, 66, 68, 70};
   private int screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
   private int screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
   private int whiteKeyWidth;
   private boolean firstPaint = true;
   private Color whiteKeyColor = new Color(255, 255, 255);
   private Color blackKeyColor = new Color(0, 0, 0);
   private Color pressedWhiteKeyColor = new Color(175, 175, 175);
   private Color pressedBlackKeyColor = new Color(50, 50, 50);
   private int octaveShift = 0;
   private int instrument = 0;
   public long tickCount = 0L;

   public KeyboardWindow() {
      new HashMap();
      this.screenWidth = (int)Toolkit.getDefaultToolkit().getScreenSize().getWidth();
      this.screenHeight = (int)Toolkit.getDefaultToolkit().getScreenSize().getHeight();
      this.setIconImage(Toolkit.getDefaultToolkit().getImage("icon.jpg"));
      this.setTitle("AWT Synthesizer");
      this.setSize(this.screenWidth * 3 / 4, this.screenHeight / 2);
      this.setDefaultCloseOperation(3);
      this.setVisible(true);
      this.addKeyListener(this);
      this.add(this.layeredPane);
      Timer timer;
      (timer = new Timer(1, new TickCounter(this))).setRepeats(true);
      timer.start();
      int middleCNote = synthesizer.NoteNameConverter.noteNumber("C5");
      System.out.println(middleCNote);
   }

   @Override
   public final void actionPerformed(ActionEvent event) {
      Object[] pressedKey = new Object[2];

      for (int whiteIndex = 0; whiteIndex < this.whiteKeys.length; whiteIndex++) {
         if (event.getSource() == this.whiteKeys[whiteIndex]) {
            pressedKey[0] = "whiteKeys";
            pressedKey[1] = whiteIndex;
         }
      }

      for (int blackIndex = 0; blackIndex < this.blackKeys.length; blackIndex++) {
         if (event.getSource() == this.blackKeys[blackIndex]) {
            pressedKey[0] = "blackKeys";
            pressedKey[1] = blackIndex;
         }
      }

      if (pressedKey[0] == "whiteKeys") {
         this.midiPlayer.playNote(this.whiteKeyNotes[(Integer)pressedKey[1]], 150, 10, 0, 0, 0);
      } else {
         if (pressedKey[0] == "blackKeys") {
            this.midiPlayer.playNote(this.blackKeyNotes[(Integer)pressedKey[1]], 150, 10, 0, 0, 0);
         }
      }
   }

   @Override
   public final void paint(Graphics graphics) {
      KeyboardWindow keyboard = this;
      if (!this.firstPaint) {
         for (int i = 0; i < keyboard.whiteKeys.length; i++) {
            keyboard.whiteKeys[i].setVisible(false);
            keyboard.whiteKeys[i].disable();
         }

         for (int j = 0; j < keyboard.blackKeys.length; j++) {
            keyboard.blackKeys[j].setVisible(false);
            keyboard.blackKeys[j].disable();
         }
      }

      keyboard.whiteKeyWidth = keyboard.getWidth() / 8;
      keyboard.whiteKeyPanel.setLayout(null);
      keyboard.blackKeyPanel.setLayout(null);
      keyboard.whiteKeyPanel.setBounds(0, 0, keyboard.getWidth(), keyboard.getHeight());
      keyboard.blackKeyPanel.setBounds(0, 0, keyboard.getWidth(), keyboard.getHeight() * 3 / 5);
      keyboard.blackKeyPanel.setBackground(new Color(0, 0, 0, 0));

      for (int whiteIndex = 0; whiteIndex < keyboard.whiteKeys.length; whiteIndex++) {
         keyboard.whiteKeys[whiteIndex] = new KeyButton();
         keyboard.whiteKeys[whiteIndex].addActionListener(keyboard);
         keyboard.whiteKeys[whiteIndex].setBounds(whiteIndex * keyboard.whiteKeyWidth, 0, keyboard.whiteKeyWidth, keyboard.whiteKeyPanel.getHeight());
         keyboard.whiteKeys[whiteIndex].setBackground(new Color(255, 255, 255));
         keyboard.whiteKeyPanel.add(keyboard.whiteKeys[whiteIndex]);
      }

      for (int blackIndex = 0; blackIndex < keyboard.blackKeys.length; blackIndex++) {
         keyboard.blackKeys[blackIndex] = new KeyButton();
         keyboard.blackKeys[blackIndex].addActionListener(keyboard);
         if (blackIndex != 0 && blackIndex != 1) {
            keyboard.blackKeys[blackIndex].setBounds(keyboard.whiteKeyWidth + keyboard.whiteKeyWidth / 2 + blackIndex * keyboard.whiteKeyWidth + keyboard.whiteKeyWidth / 8, 0, keyboard.whiteKeyWidth - keyboard.whiteKeyWidth / 4, keyboard.blackKeyPanel.getHeight());
         } else {
            keyboard.blackKeys[blackIndex].setBounds(keyboard.whiteKeyWidth / 2 + blackIndex * keyboard.whiteKeyWidth + keyboard.whiteKeyWidth / 8, 0, keyboard.whiteKeyWidth - keyboard.whiteKeyWidth / 4, keyboard.blackKeyPanel.getHeight());
         }

         keyboard.blackKeys[blackIndex].setBackground(new Color(0, 0, 0));
         keyboard.blackKeyPanel.add(keyboard.blackKeys[blackIndex]);
      }

      keyboard.layeredPane.add(keyboard.whiteKeyPanel, Integer.valueOf(0), 0);
      keyboard.layeredPane.add(keyboard.blackKeyPanel, Integer.valueOf(1), 0);
      keyboard.firstPaint = false;
   }

   @Override
   public final void keyTyped(KeyEvent event) {
   }

   @Override
   public final void keyPressed(KeyEvent event) {
      if (event.getKeyCode() == 37) {
         this.octaveShift--;
      } else if (event.getKeyCode() == 39) {
         this.octaveShift++;
      } else if (event.getKeyCode() == 38) {
         if (this.instrument < 120) {
            this.instrument++;
         }

         this.midiPlayer.setInstrument(0, this.instrument);
      } else if (event.getKeyCode() == 40) {
         if (this.instrument > 0) {
            this.instrument--;
         }

         this.midiPlayer.setInstrument(0, this.instrument);
      }

      if (event.getKeyCode() == 90) {
         this.midiPlayer.noteOn(0, 60 + 12 * this.octaveShift);
         this.whiteKeys[0].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 83) {
         this.midiPlayer.noteOn(0, 61 + 12 * this.octaveShift);
         this.blackKeys[0].setBackground(this.pressedBlackKeyColor);
      }

      if (event.getKeyCode() == 88) {
         this.midiPlayer.noteOn(0, 62 + 12 * this.octaveShift);
         this.whiteKeys[1].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 68) {
         this.midiPlayer.noteOn(0, 63 + 12 * this.octaveShift);
         this.blackKeys[1].setBackground(this.pressedBlackKeyColor);
      }

      if (event.getKeyCode() == 67) {
         this.midiPlayer.noteOn(0, 64 + 12 * this.octaveShift);
         this.whiteKeys[2].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 86) {
         this.midiPlayer.noteOn(0, 65 + 12 * this.octaveShift);
         this.whiteKeys[3].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 71) {
         this.midiPlayer.noteOn(0, 66 + 12 * this.octaveShift);
         this.blackKeys[2].setBackground(this.pressedBlackKeyColor);
      }

      if (event.getKeyCode() == 66) {
         this.midiPlayer.noteOn(0, 67 + 12 * this.octaveShift);
         this.whiteKeys[4].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 72) {
         this.midiPlayer.noteOn(0, 68 + 12 * this.octaveShift);
         this.blackKeys[3].setBackground(this.pressedBlackKeyColor);
      }

      if (event.getKeyCode() == 78) {
         this.midiPlayer.noteOn(0, 69 + 12 * this.octaveShift);
         this.whiteKeys[5].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 74) {
         this.midiPlayer.noteOn(0, 70 + 12 * this.octaveShift);
         this.blackKeys[4].setBackground(this.pressedBlackKeyColor);
      }

      if (event.getKeyCode() == 77) {
         this.midiPlayer.noteOn(0, 71 + 12 * this.octaveShift);
         this.whiteKeys[6].setBackground(this.pressedWhiteKeyColor);
      }

      if (event.getKeyCode() == 44) {
         this.midiPlayer.noteOn(0, 72 + 12 * this.octaveShift);
         this.whiteKeys[7].setBackground(this.pressedWhiteKeyColor);
      }
   }

   @Override
   public final void keyReleased(KeyEvent event) {
      if (event.getKeyCode() == 90) {
         this.midiPlayer.noteOff(0, 60 + 12 * this.octaveShift);
         this.whiteKeys[0].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 83) {
         this.midiPlayer.noteOff(0, 61 + 12 * this.octaveShift);
         this.blackKeys[0].setBackground(this.blackKeyColor);
      }

      if (event.getKeyCode() == 88) {
         this.midiPlayer.noteOff(0, 62 + 12 * this.octaveShift);
         this.whiteKeys[1].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 68) {
         this.midiPlayer.noteOff(0, 63 + 12 * this.octaveShift);
         this.blackKeys[1].setBackground(this.blackKeyColor);
      }

      if (event.getKeyCode() == 67) {
         this.midiPlayer.noteOff(0, 64 + 12 * this.octaveShift);
         this.whiteKeys[2].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 86) {
         this.midiPlayer.noteOff(0, 65 + 12 * this.octaveShift);
         this.whiteKeys[3].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 71) {
         this.midiPlayer.noteOff(0, 66 + 12 * this.octaveShift);
         this.blackKeys[2].setBackground(this.blackKeyColor);
      }

      if (event.getKeyCode() == 66) {
         this.midiPlayer.noteOff(0, 67 + 12 * this.octaveShift);
         this.whiteKeys[4].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 72) {
         this.midiPlayer.noteOff(0, 68 + 12 * this.octaveShift);
         this.blackKeys[3].setBackground(this.blackKeyColor);
      }

      if (event.getKeyCode() == 78) {
         this.midiPlayer.noteOff(0, 69 + 12 * this.octaveShift);
         this.whiteKeys[5].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 74) {
         this.midiPlayer.noteOff(0, 70 + 12 * this.octaveShift);
         this.blackKeys[4].setBackground(this.blackKeyColor);
      }

      if (event.getKeyCode() == 77) {
         this.midiPlayer.noteOff(0, 71 + 12 * this.octaveShift);
         this.whiteKeys[6].setBackground(this.whiteKeyColor);
      }

      if (event.getKeyCode() == 44) {
         this.midiPlayer.noteOff(0, 72 + 12 * this.octaveShift);
         this.whiteKeys[7].setBackground(this.whiteKeyColor);
      }
   }
}
