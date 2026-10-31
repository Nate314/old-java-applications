package web.shim.javax.sound.midi;

public interface TMidiChannel {
    void noteOn(int noteNumber, int velocity);

    void noteOff(int noteNumber, int velocity);

    void noteOff(int noteNumber);

    void programChange(int program);

    void programChange(int bank, int program);

    int getProgram();

    void setPitchBend(int bend);

    int getPitchBend();

    void setChannelPressure(int pressure);

    int getChannelPressure();

    void controlChange(int controller, int value);

    void allNotesOff();

    void allSoundOff();
}
