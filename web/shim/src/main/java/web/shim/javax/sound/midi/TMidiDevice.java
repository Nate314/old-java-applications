package web.shim.javax.sound.midi;

public interface TMidiDevice extends AutoCloseable {
    void open() throws TMidiUnavailableException;

    @Override
    void close();

    boolean isOpen();
}
