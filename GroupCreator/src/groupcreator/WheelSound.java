package groupcreator;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/** The wheel's sounds on the MIDI synthesizer: a short click as each peg passes the pointer, and a chime on landing. */
final class WheelSound {
   private static final int WOODBLOCK = 115;
   private static final int GLOCKENSPIEL = 9;
   private static final int CLICK_NOTE = 84;
   private static final int CLICK_MILLIS = 30;
   private static final int[] CHIME = {72, 76, 79, 84};
   private static final int CHIME_STEP = 110;
   private static final int CHIME_HOLD = 600;

   private MidiChannel click;
   private MidiChannel bell;
   private boolean noSynth;
   private final Timer clickOff = new Timer(CLICK_MILLIS, event -> releaseClick());
   private final Timer chimeTimer = new Timer(CHIME_STEP, event -> nextChimeNote());
   private boolean clicking;
   private int chimeNote;

   WheelSound() {
      clickOff.setRepeats(false);
   }

   /** One peg passing the pointer. */
   void click() {
      if (!open()) {
         return;
      }
      releaseClick();
      click.noteOn(CLICK_NOTE, 70);
      clicking = true;
      clickOff.restart();
   }

   /** The rising four notes played when the wheel stops. */
   void chime() {
      if (!open()) {
         return;
      }
      chimeTimer.stop();
      chimeTimer.setDelay(CHIME_STEP);
      bell.allNotesOff();
      chimeNote = 0;
      nextChimeNote();
      chimeTimer.start();
   }

   private void nextChimeNote() {
      if (chimeNote < CHIME.length) {
         bell.noteOn(CHIME[chimeNote], chimeNote == CHIME.length - 1 ? 110 : 90);
         chimeNote++;
         if (chimeNote == CHIME.length) {
            chimeTimer.setDelay(CHIME_HOLD);
         }
      } else {
         chimeTimer.stop();
         chimeTimer.setDelay(CHIME_STEP);
         bell.allNotesOff();
      }
   }

   private void releaseClick() {
      if (clicking) {
         click.noteOff(CLICK_NOTE);
         clicking = false;
      }
   }

   /** Opens the synthesizer on first use; false when there is none (the wheel then spins silently). */
   private boolean open() {
      if (click == null && !noSynth) {
         try {
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            MidiChannel[] channels = synthesizer.getChannels();
            click = channels[0];
            click.programChange(WOODBLOCK);
            bell = channels[1];
            bell.programChange(GLOCKENSPIEL);
         } catch (Exception e) {
            noSynth = true;
         }
      }
      return click != null;
   }
}
