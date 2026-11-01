package web.shim.javax.sound.midi;

import org.teavm.jso.JSBody;

/**
 * A 16-channel synthesizer on the Web Audio API. Each note is one oscillator with an attack/release envelope;
 * the General MIDI program only picks the waveform. The voice code lives in boot.js (window.__shimSynth).
 */
final class TWebSynthesizer implements TSynthesizer {
    private final TMidiChannel[] channels = new TMidiChannel[16];
    private boolean open;

    TWebSynthesizer() {
        for (int i = 0; i < channels.length; i++) {
            channels[i] = new Channel(i);
        }
    }

    @Override
    public TMidiChannel[] getChannels() {
        return channels.clone();
    }

    @Override
    public void open() {
        open = true;
    }

    @Override
    public void close() {
        open = false;
        for (TMidiChannel c : channels) {
            c.allSoundOff();
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    private static final class Channel implements TMidiChannel {
        private final int number;
        private int program;
        private int bend = 8192;
        private int pressure;

        Channel(int number) {
            this.number = number;
        }

        @Override
        public void noteOn(int note, int velocity) {
            if (velocity <= 0) {
                noteOff(note);
            } else {
                jsNoteOn(number, note, Math.min(velocity, 127), program, bendCents());
            }
        }

        @Override
        public void noteOff(int note, int velocity) {
            noteOff(note);
        }

        @Override
        public void noteOff(int note) {
            jsNoteOff(number, note);
        }

        @Override
        public void programChange(int program) {
            this.program = Math.max(0, Math.min(127, program));
        }

        @Override
        public void programChange(int bank, int program) {
            programChange(program);
        }

        @Override
        public int getProgram() {
            return program;
        }

        @Override
        public void setPitchBend(int bend) {
            this.bend = Math.max(0, Math.min(16383, bend));
            jsBend(number, bendCents());
        }

        @Override
        public int getPitchBend() {
            return bend;
        }

        /** Default bend range is +/- 2 semitones. */
        private double bendCents() {
            return (bend - 8192) / 8192.0 * 200;
        }

        @Override
        public void setChannelPressure(int pressure) {
            this.pressure = pressure;
        }

        @Override
        public int getChannelPressure() {
            return pressure;
        }

        @Override
        public void controlChange(int controller, int value) {
            if (controller == 123 || controller == 120) {
                allSoundOff();
            }
        }

        @Override
        public void allNotesOff() {
            jsAllOff(number);
        }

        @Override
        public void allSoundOff() {
            jsAllOff(number);
        }
    }

    @JSBody(params = { "ch", "note", "vel", "program", "cents" }, script =
            "if (window.__shimSynth) window.__shimSynth.noteOn(ch, note, vel, program, cents);")
    private static native void jsNoteOn(int ch, int note, int vel, int program, double cents);

    @JSBody(params = { "ch", "note" }, script = "if (window.__shimSynth) window.__shimSynth.noteOff(ch, note);")
    private static native void jsNoteOff(int ch, int note);

    @JSBody(params = { "ch", "cents" }, script = "if (window.__shimSynth) window.__shimSynth.bend(ch, cents);")
    private static native void jsBend(int ch, double cents);

    @JSBody(params = "ch", script = "if (window.__shimSynth) window.__shimSynth.allOff(ch);")
    private static native void jsAllOff(int ch);
}
