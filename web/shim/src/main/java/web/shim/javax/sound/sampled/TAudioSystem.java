package web.shim.javax.sound.sampled;

public class TAudioSystem {
    private TAudioSystem() {
    }

    /** The page's microphone, asked for with getUserMedia when the line is opened. */
    public static TTargetDataLine getTargetDataLine(TAudioFormat format) throws TLineUnavailableException {
        return new TWebMicrophoneLine(format);
    }
}
