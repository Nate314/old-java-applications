package tankgame;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/**
 * The battle's sounds on the MIDI synthesizer: the cannon, ricochets, shells bursting on walls, hits, the long boom of
 * a destroyed tank, crates landing and taken, a horn for FIGHT!, the match's end, and each tank's engine humming while
 * it drives. The synthesizer opens on a background thread so the game does not wait for it; until then (or without one,
 * or for {@link #silent}) the game is quiet. Off (V) is saved with the record.
 */
final class Sounds
{
    private static final int DRUMS     = 9;
    private static final int GUNSHOT   = 127;
    private static final int XYLOPHONE = 13;
    private static final int TAIKO     = 116;
    private static final int VIBES     = 11;
    private static final int GLOCKEN   = 9;
    private static final int BRASS     = 61;
    private static final int ROTOR     = 125;
    /** Channels: shots, ricochets, booms, shields, crates, the horn, then red's and green's engines. */
    private static final int SHOT = 0, TINK = 1, BOOM = 2, SHIELD = 3, CRATE = 4, HORN = 5, ENGINE = 6;
    private static final int[] PROGRAMS = { GUNSHOT, XYLOPHONE, TAIKO, VIBES, GLOCKEN, BRASS, ROTOR, ROTOR };
    /** Engine notes, idle and with turbo; green's sits a tone above red's so the two can be told apart. */
    private static final int ENGINE_NOTE = 31;
    private static final int TURBO_NOTE  = 36;

    private volatile MidiChannel[] channels;
    private boolean on;
    /** The note each engine is holding (0 for none). */
    private final int[] engines = new int[2];
    /** Ticks each tank has stood still while its engine hums. */
    private final int[] stillTicks = new int[2];
    private static final int ENGINE_COAST_TICKS = 12;
    /** For the self-check: the name of every sound asked for, or null when real sounds play. */
    private final List<String> heard;

    private Sounds(boolean on, List<String> heard)
    {
        this.on = on;
        this.heard = heard;
    }

    /** Sounds on the system synthesizer, on or off to begin with. */
    static Sounds open(boolean on)
    {
        Sounds s = new Sounds(on, null);
        Thread opener = new Thread(() -> {
            try
            {
                Synthesizer synthesizer = MidiSystem.getSynthesizer();
                synthesizer.open();
                MidiChannel[] all = synthesizer.getChannels();
                for (int i = 0; i < PROGRAMS.length; i++) all[i].programChange(PROGRAMS[i]);
                s.channels = all;
            }
            catch (Exception e)
            {
                System.err.println("No sound: " + e.getMessage());
            }
        }, "tank-sounds");
        opener.setDaemon(true);
        opener.start();
        return s;
    }

    /** Never makes a sound, but lists what it was asked to play (see {@link #heard()}). */
    static Sounds silent()
    {
        return new Sounds(true, new ArrayList<>());
    }

    /** What a silent Sounds was asked to play, in order (engines not included). */
    List<String> heard()
    {
        return heard;
    }

    boolean isOn()
    {
        return on;
    }

    void setOn(boolean on)
    {
        this.on = on;
        if (!on) engines(false, false, false, false);
    }

    /** A shell leaves the barrel: a kick and a crack. */
    void fire()
    {
        cue("fire");
        note(DRUMS, 36, 110, 0, 90);
        note(SHOT, 55, 80, 0, 90);
    }

    /** A shell glances off a wall or the edge of the field. */
    void ricochet()
    {
        cue("ricochet");
        note(TINK, 96, 55, 0, 40);
        note(TINK, 91, 40, 35, 60);
    }

    /** A shell bursts on a wall. */
    void burst()
    {
        cue("burst");
        note(DRUMS, 41, 90, 0, 120);
        note(SHOT, 43, 75, 0, 120);
    }

    /** A shell takes a health square. */
    void hit()
    {
        cue("hit");
        note(DRUMS, 36, 127, 0, 140);
        note(BOOM, 40, 110, 0, 180);
        note(SHOT, 48, 100, 20, 140);
    }

    /** A shield stops a shell and pops. */
    void shieldPop()
    {
        cue("shield");
        note(SHIELD, 84, 90, 0, 90);
        note(SHIELD, 79, 80, 80, 160);
    }

    /** A tank is destroyed: a long low boom that sinks as it rolls away, under the slow-motion blast. */
    void destroyed()
    {
        cue("destroyed");
        note(DRUMS, 35, 127, 0, 300);
        note(DRUMS, 36, 120, 60, 300);
        note(SHOT, 36, 127, 0, 400);
        note(BOOM, 33, 127, 0, 1100);
        MidiChannel[] all = channels;
        if (!on || all == null) return;
        // Bend the boom down two semitones over a second, then back for the next sound on the channel.
        for (int i = 0; i <= 10; i++)
        {
            int bend = 8192 - i * 819;
            later(i * 100, () -> all[BOOM].setPitchBend(bend));
        }
        later(1200, () -> all[BOOM].setPitchBend(8192));
    }

    /** A crate touches down. */
    void landed()
    {
        cue("landed");
        note(DRUMS, 45, 60, 0, 100);
    }

    /** A tank drives over a crate and takes its power: a rising run of bells. */
    void take()
    {
        cue("take");
        for (int i = 0; i < 3; i++) note(CRATE, new int[] { 79, 84, 88 }[i], 90, i * 70, 160);
    }

    /** A shell blows up a crate. */
    void crateShot()
    {
        cue("crate shot");
        note(DRUMS, 38, 100, 0, 100);
        note(SHOT, 50, 90, 0, 120);
        note(TINK, 72, 60, 30, 80);
    }

    /** FIGHT!: two notes on the horn. */
    void fight()
    {
        cue("fight");
        note(HORN, 67, 90, 0, 140);
        note(HORN, 72, 100, 150, 320);
    }

    /** The match is over: a fanfare for a winner, or a falling line when the computer beat the player. */
    void matchOver(boolean playerLost)
    {
        cue(playerLost ? "lost" : "won");
        int[] notes = playerLost ? new int[] { 67, 63, 60, 55 } : new int[] { 60, 64, 67, 72 };
        for (int i = 0; i < notes.length; i++) note(HORN, notes[i], 95, i * 160, i == notes.length - 1 ? 480 : 150);
    }

    /** Each tank's engine hums while it drives (higher with turbo) and stops when it stops. Called every tick. */
    void engines(boolean redMoving, boolean redTurbo, boolean greenMoving, boolean greenTurbo)
    {
        engine(0, on && redMoving ? redTurbo ? TURBO_NOTE : ENGINE_NOTE : 0);
        engine(1, on && greenMoving ? (greenTurbo ? TURBO_NOTE : ENGINE_NOTE) + 2 : 0);
    }

    private void engine(int tank, int note)
    {
        MidiChannel[] all = channels;
        // The computer stops and starts every few ticks: the hum carries over stops shorter than a fifth of a second.
        stillTicks[tank] = note == 0 ? stillTicks[tank] + 1 : 0;
        if (note == 0 && on && stillTicks[tank] < ENGINE_COAST_TICKS) return;
        if (engines[tank] == note || all == null) return;
        MidiChannel ch = all[ENGINE + tank];
        if (engines[tank] != 0) ch.noteOff(engines[tank]);
        if (note != 0) ch.noteOn(note, 45);
        engines[tank] = note;
    }

    private void cue(String name)
    {
        if (heard != null && on) heard.add(name);
    }

    /** Plays a note on a channel after delayMillis, for lengthMillis, while the sound is on. */
    private void note(int channel, int note, int velocity, int delayMillis, int lengthMillis)
    {
        MidiChannel[] all = channels;
        if (!on || all == null) return;
        MidiChannel ch = all[channel];
        later(delayMillis, () -> ch.noteOn(note, velocity));
        later(delayMillis + lengthMillis, () -> ch.noteOff(note));
    }

    private static void later(int millis, Runnable action)
    {
        if (millis <= 0)
        {
            action.run();
            return;
        }
        Timer timer = new Timer(millis, event -> action.run());
        timer.setRepeats(false);
        timer.start();
    }
}
