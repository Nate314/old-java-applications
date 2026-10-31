package synthesizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import musictheoryexpert.Main;

public class MySynthesizer
{
    /**
     * the synthesizer
     */
    private final Synthesizer    synthesizer;
    /**
     * channels of the synthesizer
     */
    private final MidiChannel[]  channels;
    /**
     * HashMap of friendly note names and their synthesizer equivalents
     */
    private Map<String, Integer> friendlyNotes     = new HashMap<String, Integer>();
    /**
     * Friendly note names to be used in the friendlyNotes HashMap
     */
    private String[]             friendlyNoteNames = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#",
                    "B" };
    /**
     * Window that created the this synthesizer. Only used in the wait function.
     */
    private Main                 window;

    /**
     * Creates the synthesizer, and populates the friendlyNotes HashMap
     */
    public MySynthesizer(Main window)
    {
        this.window = window;
        try
        {
            synthesizer = MidiSystem.getSynthesizer();
            channels = synthesizer.getChannels();
            synthesizer.open();
        }
        catch (final MidiUnavailableException ex)
        {
            throw new RuntimeException(ex);
        }
        for (int i = -36; i < 132; i++)
        {
            friendlyNotes.put(friendlyNoteNames[Math.abs(i) % 12] + ((i / 12)), 12 + i);
        }
        /*
         * for(int i = 0; i < synthesizer.getLoadedInstruments().size(); i++) {
         * String[] temp1 =
         * synthesizer.getLoadedInstruments().get(i).toString().split(":");
         * String[] temp2 = temp1[1].split("bank"); System.out.println(
         * "public static final int " + temp2[0].replaceAll("-|\\.|'| ",
         * "").toUpperCase() + " = " + i + ";"); }
         */
        //// System.out.print("public static String[] instruments = {");
        // for(int i = 0; i < synthesizer.getLoadedInstruments().size(); i++)
        // {
        // String[] temp1 =
        //// synthesizer.getLoadedInstruments().get(i).toString().split(":");
        // String[] temp2 = temp1[1].split("bank");
        //// if(i != synthesizer.getLoadedInstruments().size() - 1)
        //// System.out.print(temp2[0].trim() + ", ");
        //// else System.out.print(temp2[0].trim());
        // System.out.println("instruments.put(\"" + temp2[0].trim() + "\", " +
        //// i+ ");");
        // }
        //// System.out.println("}");
    }

    /**
     * sets the instrument of the specified channel
     * 
     * @param channel
     *            channel to set the instrument of
     * @param instrument
     *            instrument to set the channel to
     */
    public void setInstrument(int channel, int instrument)
    {
        channels[channel].programChange(instrument);
    }

    /**
     * this function plays multiple notes using numbers
     * 
     * @param notes
     *            the notes to be played
     * @param duration
     *            the length of time to play the chord (in milliseconds)
     * @param pause
     *            the amount of time between this chord and the next
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    public void playNotes(ArrayList<Integer> notes, ArrayList<Integer> durration, ArrayList<Integer> pause,
                    ArrayList<Integer> pressure, ArrayList<Integer> pitchBend, int channel)
    {
        for (int i = 0; i < notes.size(); i++)
        {
            playNote(notes.get(i), durration.get(i), pause.get(i), pressure.get(i), pitchBend.get(i), channel);
        }
    }

    /**
     * this function plays a note using numbers
     * 
     * @param note
     *            the note to be played
     * @param duration
     *            the length of time to play the chord (in milliseconds)
     * @param pause
     *            the amount of time between this chord and the next
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    public void playNotes(int note, ArrayList<Integer> onOff, ArrayList<Integer> duration, ArrayList<Integer> pause,
                    ArrayList<Integer> pressure, ArrayList<Integer> pitchBend, int channel)
    {
        for (int i = 0; i < onOff.size(); i++)
        {
            if (onOff.get(i) == 1)
            {
                playNote(note, duration.get(i), pause.get(i), pressure.get(i), pitchBend.get(i), channel);
            }
            else
                waitMilliseconds(duration.get(i));
        }
    }

    /**
     * this function plays a note using numbers
     * 
     * @param note
     *            the note to be played
     * @param duration
     *            the length of time to play the chord (in milliseconds)
     * @param pause
     *            the amount of time between this chord and the next
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    public void playNotes(int note, int duration, int pause, int pressure, int pitchBend, int channel)
    {
        playNote(note, duration, pause, pressure, pitchBend, channel);
    }

    /**
     * this function plays multiple chords using numbers
     * 
     * @param chords
     *            the chords to be played
     * @param duration
     *            the length of time to play the chord (in milliseconds)
     * @param pause
     *            the amount of time between this chord and the next
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    public void playChords(ArrayList<ArrayList<Integer>> chords, ArrayList<Integer> durration, ArrayList<Integer> pause,
                    ArrayList<Integer> pressure, ArrayList<Integer> pitchBend, int channel)
    {
        for (int i = 0; i < chords.size(); i++)
        {
            playChord(chords.get(i), durration.get(i), pause.get(i), pressure.get(i), pitchBend.get(i), channel);
        }
    }

    /**
     * this function is used to play a chord using numbers
     * 
     * @param chord
     *            array of notes in the chord to be played (60 is middle c)
     * @param duration
     *            the length of time to play the chord (in milliseconds)
     * @param pause
     *            the amount of time between this chord and the next
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    public void playChords(ArrayList<Integer> chord, int duration, int pause, int pressure, int pitchBend, int channel)
    {
        playChord(chord, duration, pause, pressure, pitchBend, channel);
    }

    /**
     * this private function is used to play a note
     * 
     * @param note
     *            note to be played (60 is middle c)
     * @param duration
     *            duration of the chord (milliseconds)
     * @param pause
     *            pause between this chord and the next (milliseconds)
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    private void playNote(int note, int duration, int pause, int pressure, int pitchBend, int channel)
    {
        if (note != Notes.NOT_A_NOTE)
        {
            // channels[channel].noteOn(note, 1000);
            turnOnNote(channel, note, 1000);
            if (pitchBend != 0) pitchBend(duration, pitchBend, pressure);
            else
                waitMilliseconds(duration);
            // waitMilliseconds(duration);
            // channels[channel].noteOff(note);
            turnOffNote(channel, note);
            waitMilliseconds(pause);
        }
        else
        {
            waitMilliseconds(duration + pause);
        }
    }

    /**
     * this private function is used to play a chord
     * 
     * @param notes
     *            notes in the cord to be played
     * @param duration
     *            duration of the chord (milliseconds)
     * @param pause
     *            pause between this chord and the next (milliseconds)
     * @param pressure
     *            amount to bend (0 is no bend, 127 is max bend)
     * @param pitchBend
     *            amount to pitch bend
     * @param channel
     *            channel to play the note on
     */
    private void playChord(ArrayList<Integer> notes, int duration, int pause, int pressure, int pitchBend, int channel)
    {
        if (notes != null)
        {
            for (int i = 0; i < notes.size(); i++)
            {
                // channels[channel].noteOn(notes.get(i), 1000);
                turnOnNote(channel, notes.get(i), 1000);
            }
            if (pitchBend != 0) pitchBend(duration, pitchBend, pressure);
            else
                waitMilliseconds(duration);
            for (int i = 0; i < notes.size(); i++)
            {
                // channels[channel].noteOff(notes.get(i));
                turnOffNote(channel, notes.get(i));
            }
            waitMilliseconds(pause);
        }
        else
        {
            waitMilliseconds(duration + pause);
        }
    }

    /**
     * @param channel
     *            channel to use the note on
     * @param note
     *            note to turn on
     * @param velocity
     *            velocity of the note
     */
    public void turnOnNote(int channel, int note, int velocity)
    {
        channels[channel].noteOn(note, velocity);
    }

    /**
     * @param channel
     *            channel to use
     * @param note
     *            note to turn off
     */
    public void turnOffNote(int channel, int note)
    {
        channels[channel].noteOff(note);
    }

    /**
     * this function is used for bending and wobbling the pitch
     * 
     * @param duration
     *            duration of the pitch bend (in milliseconds)
     * @param wobbliness
     *            amount to wobble (0 to 127)
     */
    private void pitchBend(int duration, int pitchBend, int wobbliness)
    {
        channels[0].setChannelPressure(wobbliness);
        for (int i = pitchBend; i > 0; i -= (pitchBend / duration))
        {
            channels[0].setPitchBend(channels[0].getPitchBend() - (pitchBend / duration));
            waitMilliseconds(1);
        }
    }

    /**
     * this function was created so that the rest of this class does not have to
     * be littered with try-catch statements
     * 
     * @param millis
     *            milliseconds to sleep for
     */
    public void waitMilliseconds(final long millis)
    {
//        long endTime = millis + window.CLOCK_TICK;
//        while (window.CLOCK_TICK < endTime)
//        {
//            try
//            {
//                Thread.sleep(1);
//            }
//            catch (InterruptedException e)
//            {
//                e.printStackTrace();
//            }
//        }
		try {
			final long finishTime = System.currentTimeMillis() + millis;
			while (/* true */finishTime - System.currentTimeMillis() > 0) {
				try {
					Thread.sleep(finishTime - System.currentTimeMillis());
				} catch (final InterruptedException ex) {
				}
			}
		} catch (final IllegalArgumentException ex) {
		}
    }
}