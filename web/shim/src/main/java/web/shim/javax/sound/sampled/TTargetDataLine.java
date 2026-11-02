package web.shim.javax.sound.sampled;

/** The recording calls of javax.sound.sampled.TargetDataLine that apps use. */
public interface TTargetDataLine {
    void open(TAudioFormat format) throws TLineUnavailableException;

    void open() throws TLineUnavailableException;

    void start();

    void stop();

    void close();

    boolean isOpen();

    TAudioFormat getFormat();

    int available();

    int read(byte[] bytes, int offset, int length);
}
